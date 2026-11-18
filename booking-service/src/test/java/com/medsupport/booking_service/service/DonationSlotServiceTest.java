
package com.medsupport.booking_service.service;

import com.medsupport.booking_service.model.DonationSlot;
import com.medsupport.booking_service.repository.DonationSlotRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DonationSlotServiceTest {

    @Mock
    private DonationSlotRepository slotRepository;

    @InjectMocks
    private DonationSlotService slotService;

    // TEST 1: Create a donation slot successfully.
    @Test
    void shouldCreateSlotSuccessfully() {

        Long hospitalId = 2L;
        LocalDate date = LocalDate.now().plusDays(2);
        LocalTime startTime = LocalTime.of(9, 0);
        LocalTime endTime = LocalTime.of(12, 0);
        Integer capacity = 5;

        when(slotRepository.save(any(DonationSlot.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DonationSlot result = slotService.createSlot(
                hospitalId,
                date,
                startTime,
                endTime,
                capacity
        );

        assertNotNull(result);
        assertEquals(hospitalId, result.getHospitalId());
        assertEquals(date, result.getSlotDate());
        assertEquals(startTime, result.getStartTime());
        assertEquals(endTime, result.getEndTime());
        assertEquals(capacity, result.getCapacity());
        assertEquals(0, result.getReservedCount());
        assertEquals("OPEN", result.getStatus());

        verify(slotRepository).save(any(DonationSlot.class));
    }

    // TEST 2: Prevent capacity from being less than reservations.
    @Test
    void shouldRejectCapacityBelowExistingReservations() {

        Long slotId = 1L;
        Long hospitalId = 2L;

        DonationSlot slot = mock(DonationSlot.class);

        when(slotRepository.findByIdForUpdate(slotId))
                .thenReturn(Optional.of(slot));

        when(slot.getHospitalId()).thenReturn(hospitalId);
        when(slot.getReservedCount()).thenReturn(4);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> slotService.updateCapacity(
                        slotId,
                        3,
                        hospitalId
                )
        );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode()
        );

        verify(slot, never()).setCapacity(anyInt());
        verify(slotRepository, never()).save(any());
    }

    // TEST 3: Prevent another hospital from updating the slot.
    @Test
    void shouldRejectCapacityUpdateFromAnotherHospital() {

        Long slotId = 1L;
        Long slotHospitalId = 2L;
        Long anotherHospitalId = 99L;

        DonationSlot slot = mock(DonationSlot.class);

        when(slotRepository.findByIdForUpdate(slotId))
                .thenReturn(Optional.of(slot));

        when(slot.getHospitalId()).thenReturn(slotHospitalId);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> slotService.updateCapacity(
                        slotId,
                        10,
                        anotherHospitalId
                )
        );

        assertEquals(
                HttpStatus.FORBIDDEN,
                exception.getStatusCode()
        );

        verify(slot, never()).setCapacity(anyInt());
        verify(slotRepository, never()).save(any());
    }

    // TEST 4: Return available slots for a valid blood request.
    @Test
    void shouldReturnAvailableSlotsForBloodRequest() {

        Long hospitalId = 2L;
        LocalDate neededDate = LocalDate.now().plusDays(5);

        DonationSlot slot = new DonationSlot();

        when(slotRepository.findAvailableSlotsForBloodRequest(
                eq(hospitalId),
                eq(neededDate),
                any(LocalDate.class),
                any(LocalTime.class)
        )).thenReturn(List.of(slot));

        List<DonationSlot> result =
                slotService.getAvailableSlotsForBloodRequest(
                        hospitalId,
                        neededDate
                );

        assertEquals(1, result.size());
        assertSame(slot, result.get(0));

        verify(slotRepository).findAvailableSlotsForBloodRequest(
                eq(hospitalId),
                eq(neededDate),
                any(LocalDate.class),
                any(LocalTime.class)
        );
    }

    // TEST 5: Return no slots when blood request deadline has passed.
    @Test
    void shouldReturnEmptyWhenNeededDateIsPast() {

        List<DonationSlot> result =
                slotService.getAvailableSlotsForBloodRequest(
                        2L,
                        LocalDate.now().minusDays(1)
                );

        assertTrue(result.isEmpty());
        verifyNoInteractions(slotRepository);
    }

    // TEST 6: Reject an invalid hospital ID.
    @Test
    void shouldRejectInvalidHospitalIdForAvailability() {

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> slotService.getAvailableSlotsForBloodRequest(
                        0L,
                        LocalDate.now().plusDays(2)
                )
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );

        verifyNoInteractions(slotRepository);
    }

    // TEST 7: Reject a missing blood request deadline.
    @Test
    void shouldRejectMissingNeededDate() {

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> slotService.getAvailableSlotsForBloodRequest(
                        2L,
                        null
                )
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );

        verifyNoInteractions(slotRepository);
    }
}
