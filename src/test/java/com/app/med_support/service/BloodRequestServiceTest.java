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

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

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

        bloodRequestService = new BloodRequestService(donationBookingRepository, bloodRequestRepository, hospitalRepository);
    }


    // Test 1:
    // Blood request cannot be created without a hospital
    @Test
    void shouldReturnNullWhenHospitalIsMissing() {
        BloodRequest bloodRequest = new BloodRequest();

        BloodRequest result = bloodRequestService.createBloodRequest(bloodRequest);
        assertNull(result);
    }


    // Test 2:
    // Hospital must exist in the database
    @Test
    void shouldReturnNullWhenHospitalDoesNotExist() {
        BloodRequest bloodRequest = new BloodRequest();
        Hospital hospital = new Hospital();
        hospital.setId(999L);
        bloodRequest.setHospital(hospital);
        when(hospitalRepository.findById(999L)).thenReturn(Optional.empty());

        BloodRequest result = bloodRequestService.createBloodRequest(bloodRequest);
        assertNull(result);
    }


    // Test 3:
    // Hospital must be active
    @Test
    void shouldReturnNullWhenHospitalIsInactive() {

        Hospital hospital = new Hospital();
        hospital.setId(1L);
        hospital.setHospitalActive(false);

        BloodRequest bloodRequest = new BloodRequest();
        bloodRequest.setHospital(hospital);

        when(hospitalRepository.findById(1L)).thenReturn(Optional.of(hospital));

        BloodRequest result = bloodRequestService.createBloodRequest(bloodRequest);

        assertNull(result);
    }


    // Test 4:
    // Required blood units must be greater than zero
    @Test
    void shouldReturnNullWhenBloodUnitsAreZero() {

        Hospital hospital = new Hospital();
        hospital.setId(1L);
        hospital.setHospitalActive(true);

        BloodRequest bloodRequest = new BloodRequest();
        bloodRequest.setHospital(hospital);
        bloodRequest.setRequiredBloodUnits(0);

        when(hospitalRepository.findById(1L)).thenReturn(Optional.of(hospital));

        BloodRequest result = bloodRequestService.createBloodRequest(bloodRequest);

        assertNull(result);
    }


    // Test 5:
    // Blood needed date cannot be in the past
    @Test
    void shouldReturnNullWhenBloodNeededDateIsInThePast() {

        Hospital hospital = new Hospital();
        hospital.setId(1L);
        hospital.setHospitalActive(true);

        BloodRequest bloodRequest = new BloodRequest();
        bloodRequest.setHospital(hospital);
        bloodRequest.setRequiredBloodUnits(2);
        bloodRequest.setBloodNeededDate(LocalDate.now().minusDays(1));

        when(hospitalRepository.findById(1L)).thenReturn(Optional.of(hospital));

        BloodRequest result = bloodRequestService.createBloodRequest(bloodRequest);

        assertNull(result);
    }


    // Test 6:
    // Only valid blood types are accepted
    @Test
    void shouldReturnNullWhenBloodTypeIsInvalid() {

        Hospital hospital = new Hospital();
        hospital.setId(1L);
        hospital.setHospitalActive(true);

        BloodRequest bloodRequest = new BloodRequest();
        bloodRequest.setHospital(hospital);
        bloodRequest.setRequiredBloodUnits(2);
        bloodRequest.setBloodNeededDate(LocalDate.now().plusDays(2));
        bloodRequest.setRequiredBloodType("X+");

        when(hospitalRepository.findById(1L)).thenReturn(Optional.of(hospital));

        BloodRequest result = bloodRequestService.createBloodRequest(bloodRequest);

        assertNull(result);
    }


    // Test 7:
    // Only LOW, MEDIUM, HIGH, or CRITICAL urgency is accepted
    @Test
    void shouldReturnNullWhenUrgencyIsInvalid() {

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

        BloodRequest result = bloodRequestService.createBloodRequest(bloodRequest);

        assertNull(result);
    }


    // Test 8:
    // Valid blood request should be created successfully
    // and its status should automatically become OPEN
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
    }


    // Test 9:
    // Hospital staff cannot update a request belonging to another hospital
    @Test
    void shouldReturnNullWhenDifferentHospitalTriesToUpdateStatus() {

        Hospital hospital = new Hospital();
        hospital.setId(1L);

        BloodRequest bloodRequest = new BloodRequest();
        bloodRequest.setId(1L);
        bloodRequest.setHospital(hospital);
        bloodRequest.setBloodRequestStatus("OPEN");

        when(bloodRequestRepository.findById(1L)).thenReturn(Optional.of(bloodRequest));

        BloodRequest result = bloodRequestService.updateBloodRequestStatus(
        1L, "CLOSED", 2L);

        assertNull(result);
    }


    // Test 10:
    // Invalid blood request status must be rejected
    @Test
    void shouldReturnNullWhenNewStatusIsInvalid() {

        Hospital hospital = new Hospital();
        hospital.setId(1L);

        BloodRequest bloodRequest = new BloodRequest();
        bloodRequest.setId(1L);
        bloodRequest.setHospital(hospital);
        bloodRequest.setBloodRequestStatus("OPEN");

        when(bloodRequestRepository.findById(1L)).thenReturn(Optional.of(bloodRequest));

        BloodRequest result = bloodRequestService.updateBloodRequestStatus(
                1L, "UNKNOWN", 1L);

        assertNull(result);
    }


    // Test 11:
    // Hospital can successfully close its own blood request
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
    }
}
