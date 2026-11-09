
package com.medsupport.booking_service.service;

import com.medsupport.booking_service.model.DonationSlot;
import com.medsupport.booking_service.repository.DonationBookingRepository;
import com.medsupport.booking_service.repository.DonationSlotRepository;
import com.medsupport.booking_service.request.DonationBookingRequest;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.medsupport.booking_service.model.DonationBooking;

@ExtendWith(MockitoExtension.class)
class DonationBookingServiceTest {

    @Mock
    private DonationBookingRepository bookingRepository;

    @Mock
    private DonationSlotRepository slotRepository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private Query advisoryLockQuery;

    @InjectMocks
    private DonationBookingService bookingService;

    private DonationBookingRequest request;

    @BeforeEach
    void setUp() {

        // Inject the mocked EntityManager into the service.
        ReflectionTestUtils.setField(
                bookingService,
                "entityManager",
                entityManager
        );

        request = mock(DonationBookingRequest.class);

        lenient().when(request.getDonationSlotId()).thenReturn(1L);
        lenient().when(request.getBloodRequestId()).thenReturn(10L);
        lenient().when(request.getDonorProfileId()).thenReturn(5L);
    }

    // Test 1: Reject null booking requests.
    @Test
    void shouldRejectBookingWhenRequestIsNull() {

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.createBooking(
                        null,
                        5L,
                        2L,
                        LocalDate.now().plusDays(5),
                        "OPEN"
                )
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );
    }

    // Test 2: Reject bookings for closed blood requests.
    @Test
    void shouldRejectBookingWhenBloodRequestIsClosed() {

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.createBooking(
                        request,
                        5L,
                        2L,
                        LocalDate.now().plusDays(5),
                        "CLOSED"
                )
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );

        verifyNoInteractions(bookingRepository, slotRepository);
    }

    // Test 3: Prevent booking on behalf of another donor.
    @Test
    void shouldRejectBookingForAnotherDonor() {

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.createBooking(
                        request,
                        99L,
                        2L,
                        LocalDate.now().plusDays(5),
                        "OPEN"
                )
        );

        assertEquals(
                HttpStatus.FORBIDDEN,
                exception.getStatusCode()
        );

        verifyNoInteractions(bookingRepository, slotRepository);
    }

    // Test 4: Prevent duplicate bookings at the same date and time.
    @Test
    void shouldRejectDuplicateBooking() {

        LocalDate date = LocalDate.now().plusDays(2);
        LocalTime time = LocalTime.of(10, 0);

        DonationSlot slot = mock(DonationSlot.class);

        when(request.getDonationBookingDate()).thenReturn(date);
        when(request.getDonationBookingTime()).thenReturn(time);

        // Mock PostgreSQL advisory lock.
        when(entityManager.createNativeQuery(
                "SELECT pg_advisory_xact_lock(:donorId)"
        )).thenReturn(advisoryLockQuery);

        when(advisoryLockQuery.setParameter("donorId", 5L))
                .thenReturn(advisoryLockQuery);

        when(slotRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(slot));

        when(slot.getHospitalId()).thenReturn(2L);
        when(slot.getSlotDate()).thenReturn(date);
        when(slot.getStartTime()).thenReturn(LocalTime.of(9, 0));
        when(slot.getEndTime()).thenReturn(LocalTime.of(12, 0));
        when(slot.getStatus()).thenReturn("OPEN");

        // Simulate an existing active booking.
        when(bookingRepository
                .existsByDonorProfileIdAndDonationBookingDateAndDonationBookingTimeAndDonationBookingStatusNot(
                        5L,
                        date,
                        time,
                        "CANCELLED"
                ))
                .thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.createBooking(
                        request,
                        5L,
                        2L,
                        date.plusDays(1),
                        "OPEN"
                )
        );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode()
        );

        // No booking or slot changes should be saved.
        verify(bookingRepository, never()).saveAndFlush(any());
        verify(slotRepository, never()).save(any());
    }


    @Test
    void shouldRejectBookingWhenSlotIsFull() {

        LocalDate date = LocalDate.now().plusDays(2);
        LocalTime time = LocalTime.of(10, 0);

        DonationSlot slot = mock(DonationSlot.class);

        when(request.getDonationBookingDate()).thenReturn(date);
        when(request.getDonationBookingTime()).thenReturn(time);

        // Mock PostgreSQL advisory lock
        when(entityManager.createNativeQuery(
                "SELECT pg_advisory_xact_lock(:donorId)"
        )).thenReturn(advisoryLockQuery);

        when(advisoryLockQuery.setParameter("donorId", 5L))
                .thenReturn(advisoryLockQuery);

        // Find and lock the slot
        when(slotRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(slot));

        when(slot.getHospitalId()).thenReturn(2L);
        when(slot.getSlotDate()).thenReturn(date);
        when(slot.getStartTime()).thenReturn(LocalTime.of(9, 0));
        when(slot.getEndTime()).thenReturn(LocalTime.of(12, 0));
        when(slot.getStatus()).thenReturn("OPEN");

        // Slot is already full
        when(slot.getReservedCount()).thenReturn(2);
        when(slot.getCapacity()).thenReturn(2);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.createBooking(
                        request,
                        5L,
                        2L,
                        date.plusDays(1),
                        "OPEN"
                )
        );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode()
        );

        // No changes should be saved
        verify(bookingRepository, never()).saveAndFlush(any());
        verify(slotRepository, never()).save(any());
    }


    @Test
    void shouldCreateBookingSuccessfully() {

        LocalDate date = LocalDate.now().plusDays(2);
        LocalTime time = LocalTime.of(10, 0);

        DonationSlot slot = mock(DonationSlot.class);

        when(request.getDonationBookingDate()).thenReturn(date);
        when(request.getDonationBookingTime()).thenReturn(time);

        // Mock PostgreSQL advisory lock
        when(entityManager.createNativeQuery(
                "SELECT pg_advisory_xact_lock(:donorId)"
        )).thenReturn(advisoryLockQuery);

        when(advisoryLockQuery.setParameter("donorId", 5L))
                .thenReturn(advisoryLockQuery);

        // Lock and retrieve the donation slot
        when(slotRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(slot));

        when(slot.getHospitalId()).thenReturn(2L);
        when(slot.getSlotDate()).thenReturn(date);
        when(slot.getStartTime()).thenReturn(LocalTime.of(9, 0));
        when(slot.getEndTime()).thenReturn(LocalTime.of(12, 0));
        when(slot.getStatus()).thenReturn("OPEN");

        // Slot has available capacity
        when(slot.getReservedCount()).thenReturn(1);
        when(slot.getCapacity()).thenReturn(3);

        // No duplicate booking exists
        when(bookingRepository
                .existsByDonorProfileIdAndDonationBookingDateAndDonationBookingTimeAndDonationBookingStatusNot(
                        5L, date, time, "CANCELLED"
                )).thenReturn(false);

        // Simulate saving the booking
        when(bookingRepository.saveAndFlush(any(DonationBooking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        bookingService.createBooking(
                request,
                5L,
                2L,
                date.plusDays(1),
                "OPEN"
        );

        // Verify reserved count increases from 1 to 2
        verify(slot).setReservedCount(2);
        verify(slotRepository).save(slot);

        // Verify booking was saved
        verify(bookingRepository).saveAndFlush(
                argThat(booking ->
                        booking.getDonorProfileId().equals(5L)
                                && booking.getBloodRequestId().equals(10L)
                                && booking.getDonationBookingStatus().equals("PENDING")
                                && booking.getDonationSlot() == slot
                )
        );
    }


    @Test
    void shouldCancelBookingAndReleaseSlotCapacity() {

        Long bookingId = 100L;
        Long donorId = 5L;

        DonationSlot slot = mock(DonationSlot.class);
        DonationBooking booking = mock(DonationBooking.class);

        when(bookingRepository.findById(bookingId))
                .thenReturn(Optional.of(booking));

        when(booking.getDonorProfileId()).thenReturn(donorId);
        when(booking.getDonationSlot()).thenReturn(slot);
        when(slot.getId()).thenReturn(1L);

        when(slotRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(slot));

        when(bookingRepository.findByIdForUpdate(bookingId))
                .thenReturn(Optional.of(booking));

        when(booking.getDonationBookingStatus())
                .thenReturn("PENDING");

        when(slot.getReservedCount()).thenReturn(2);

        when(bookingRepository.saveAndFlush(booking))
                .thenReturn(booking);

        bookingService.cancelBooking(bookingId, donorId);

        verify(booking).setDonationBookingStatus("CANCELLED");
        verify(slot).setReservedCount(1);
        verify(slotRepository).save(slot);
        verify(bookingRepository).saveAndFlush(booking);
    }


    @Test
    void shouldRejectCancellationOfCompletedBooking() {

        Long bookingId = 100L;
        Long donorId = 5L;

        DonationSlot slot = mock(DonationSlot.class);
        DonationBooking booking = mock(DonationBooking.class);

        when(bookingRepository.findById(bookingId))
                .thenReturn(Optional.of(booking));

        when(booking.getDonorProfileId())
                .thenReturn(donorId);

        when(booking.getDonationSlot())
                .thenReturn(slot);

        when(slot.getId()).thenReturn(1L);

        when(slotRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(slot));

        when(bookingRepository.findByIdForUpdate(bookingId))
                .thenReturn(Optional.of(booking));

        when(booking.getDonationBookingStatus())
                .thenReturn("COMPLETED");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.cancelBooking(
                        bookingId,
                        donorId
                )
        );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode()
        );

        // No changes should be saved
        verify(booking, never()).setDonationBookingStatus(anyString());
        verify(slot, never()).setReservedCount(anyInt());
        verify(slotRepository, never()).save(any());
        verify(bookingRepository, never()).saveAndFlush(any());
    }




}
