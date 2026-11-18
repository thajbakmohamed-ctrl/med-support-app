
package com.app.med_support.service;

import com.app.med_support.model.BloodRequest;
import com.app.med_support.model.Hospital;
import com.app.med_support.repository.BloodRequestRepository;
import com.app.med_support.repository.DonationBookingRepository;
import com.app.med_support.repository.HospitalRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class BloodRequestServiceTest {

    @Mock
    private DonationBookingRepository donationBookingRepository;

    @Mock
    private BloodRequestRepository bloodRequestRepository;

    @Mock
    private HospitalRepository hospitalRepository;

    private BloodRequestService bloodRequestService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        bloodRequestService = new BloodRequestService(donationBookingRepository, bloodRequestRepository,
                hospitalRepository);
    }

    // Test 1: Hospital ID is required
    @Test
    void shouldRejectWhenHospitalIsMissing() {
        BloodRequest bloodRequest = new BloodRequest();

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bloodRequestService.createBloodRequest(bloodRequest));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Hospital ID is required", exception.getReason());
        verify(bloodRequestRepository, never()).save(any());
    }

    // Test 2: Hospital must exist in the database
    @Test
    void shouldRejectWhenHospitalDoesNotExist() {
        BloodRequest bloodRequest = new BloodRequest();

        Hospital hospital = new Hospital();
        hospital.setId(999L);
        bloodRequest.setHospital(hospital);

        when(hospitalRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> bloodRequestService.createBloodRequest(bloodRequest));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Hospital not found", exception.getReason());
        verify(bloodRequestRepository, never()).save(any());
    }

    // Test 3: Hospital must be active
    @Test
    void shouldRejectWhenHospitalIsInactive() {
        Hospital hospital = new Hospital();
        hospital.setId(1L);
        hospital.setHospitalActive(false);

        BloodRequest bloodRequest = new BloodRequest();
        bloodRequest.setHospital(hospital);

        when(hospitalRepository.findById(1L)).thenReturn(Optional.of(hospital));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bloodRequestService.createBloodRequest(bloodRequest));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Hospital is not active", exception.getReason());
        verify(bloodRequestRepository, never()).save(any());
    }

    // Test 4: Required blood units must be greater than zero
    @Test
    void shouldRejectWhenBloodUnitsAreZero() {
        Hospital hospital = new Hospital();
        hospital.setId(1L);
        hospital.setHospitalActive(true);

        BloodRequest bloodRequest = new BloodRequest();
        bloodRequest.setHospital(hospital);
        bloodRequest.setRequiredBloodUnits(0);

        when(hospitalRepository.findById(1L)).thenReturn(Optional.of(hospital));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bloodRequestService.createBloodRequest(bloodRequest));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals(
                "Required blood units must be greater than zero",
                exception.getReason());
        verify(bloodRequestRepository, never()).save(any());
    }

    // Test 5: Blood needed date cannot be in the past
    @Test
    void shouldRejectWhenBloodNeededDateIsInThePast() {
        Hospital hospital = new Hospital();
        hospital.setId(1L);
        hospital.setHospitalActive(true);

        BloodRequest bloodRequest = new BloodRequest();
        bloodRequest.setHospital(hospital);
        bloodRequest.setRequiredBloodUnits(2);
        bloodRequest.setBloodNeededDate(LocalDate.now().minusDays(1));

        when(hospitalRepository.findById(1L)).thenReturn(Optional.of(hospital));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bloodRequestService.createBloodRequest(bloodRequest));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals(
                "Blood needed date is required and cannot be in the past",
                exception.getReason());
        verify(bloodRequestRepository, never()).save(any());
    }

    // Test 6: Only valid blood types are accepted
    @Test
    void shouldRejectWhenBloodTypeIsInvalid() {
        Hospital hospital = new Hospital();
        hospital.setId(1L);
        hospital.setHospitalActive(true);

        BloodRequest bloodRequest = new BloodRequest();
        bloodRequest.setHospital(hospital);
        bloodRequest.setRequiredBloodUnits(2);
        bloodRequest.setBloodNeededDate(LocalDate.now().plusDays(2));
        bloodRequest.setRequiredBloodType("X+");

        when(hospitalRepository.findById(1L))
                .thenReturn(Optional.of(hospital));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bloodRequestService.createBloodRequest(bloodRequest));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Invalid blood type", exception.getReason());
        verify(bloodRequestRepository, never()).save(any());
    }

    // Test 7: Urgency must be LOW, MEDIUM, HIGH, or CRITICAL
    @Test
    void shouldRejectWhenUrgencyIsInvalid() {
        Hospital hospital = new Hospital();
        hospital.setId(1L);
        hospital.setHospitalActive(true);

        BloodRequest bloodRequest = new BloodRequest();
        bloodRequest.setHospital(hospital);
        bloodRequest.setRequiredBloodUnits(2);
        bloodRequest.setBloodNeededDate(LocalDate.now().plusDays(2));
        bloodRequest.setRequiredBloodType("O+");
        bloodRequest.setBloodRequestUrgency("VERY_HIGH");

        when(hospitalRepository.findById(1L)).thenReturn(Optional.of(hospital));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bloodRequestService.createBloodRequest(bloodRequest));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals(
                "Invalid blood request urgency",
                exception.getReason());
        verify(bloodRequestRepository, never()).save(any());
    }

    // Test 8: Valid blood request is created with OPEN status
    @Test
    void shouldCreateBloodRequestSuccessfullyWhenDataIsValid() {
        Hospital hospital = new Hospital();
        hospital.setId(1L);
        hospital.setHospitalActive(true);

        BloodRequest bloodRequest = new BloodRequest();
        bloodRequest.setHospital(hospital);
        bloodRequest.setRequiredBloodUnits(3);
        bloodRequest.setBloodNeededDate(LocalDate.now().plusDays(3));
        bloodRequest.setRequiredBloodType("A+");
        bloodRequest.setBloodRequestUrgency("HIGH");

        when(hospitalRepository.findById(1L)).thenReturn(Optional.of(hospital));

        when(bloodRequestRepository.save(bloodRequest)).thenReturn(bloodRequest);

        BloodRequest result = bloodRequestService.createBloodRequest(bloodRequest);

        assertNotNull(result);
        assertEquals("OPEN", result.getBloodRequestStatus());
        verify(bloodRequestRepository).save(bloodRequest);
    }

    // Test 9: Hospital cannot update another hospital's request
    @Test
    void shouldRejectWhenDifferentHospitalTriesToUpdateStatus() {
        Hospital hospital = new Hospital();
        hospital.setId(1L);

        BloodRequest bloodRequest = new BloodRequest();
        bloodRequest.setId(1L);
        bloodRequest.setHospital(hospital);
        bloodRequest.setBloodRequestStatus("OPEN");

        when(bloodRequestRepository.findById(1L)).thenReturn(Optional.of(bloodRequest));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bloodRequestService.updateBloodRequestStatus(
                        1L, "CLOSED", 2L)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals(
                "You cannot update a blood request from another hospital",
                exception.getReason());
        verify(bloodRequestRepository, never()).save(any());
    }

    // Test 10: Invalid blood request status must be rejected
    @Test
    void shouldRejectWhenNewStatusIsInvalid() {
        Hospital hospital = new Hospital();
        hospital.setId(1L);

        BloodRequest bloodRequest = new BloodRequest();
        bloodRequest.setId(1L);
        bloodRequest.setHospital(hospital);
        bloodRequest.setBloodRequestStatus("OPEN");

        when(bloodRequestRepository.findById(1L)).thenReturn(Optional.of(bloodRequest));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bloodRequestService.updateBloodRequestStatus(
                        1L, "UNKNOWN", 1L));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals(
                "Invalid blood request status", exception.getReason());
        verify(bloodRequestRepository, never()).save(any());
    }

    // Test 11: Hospital can close its own blood request
    @Test
    void shouldUpdateBloodRequestStatusSuccessfully() {
        Hospital hospital = new Hospital();
        hospital.setId(1L);

        BloodRequest bloodRequest = new BloodRequest();
        bloodRequest.setId(1L);
        bloodRequest.setHospital(hospital);
        bloodRequest.setBloodRequestStatus("OPEN");

        when(bloodRequestRepository.findById(1L)).thenReturn(Optional.of(bloodRequest));

        when(bloodRequestRepository.save(bloodRequest)).thenReturn(bloodRequest);

        BloodRequest result = bloodRequestService.updateBloodRequestStatus(
                1L, "CLOSED", 1L);

        assertNotNull(result);
        assertEquals("CLOSED", result.getBloodRequestStatus());
        verify(bloodRequestRepository).save(bloodRequest);
    }
}
