
package com.medsupport.blood_request_service.service;

import com.medsupport.blood_request_service.model.BloodRequest;
import com.medsupport.blood_request_service.model.Hospital;
import com.medsupport.blood_request_service.repository.BloodRequestRepository;
import com.medsupport.blood_request_service.repository.HospitalRepository;
import com.medsupport.blood_request_service.request.BloodRequestRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BloodRequestServiceTest {

    private static final Long BLOOD_REQUEST_ID = 10L;
    private static final Long HOSPITAL_ID = 2L;
    private static final String EMAIL = "staff@example.com";

    @Mock
    private BloodRequestRepository bloodRequestRepository;

    @Mock
    private HospitalRepository hospitalRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private BloodRequestService bloodRequestService;

    private BloodRequest bloodRequest;
    private Hospital hospital;

    @BeforeEach
    void setUp() {

        hospital = mock(Hospital.class);

        lenient().when(hospital.getId())
                .thenReturn(HOSPITAL_ID);

        bloodRequest = new BloodRequest();

        bloodRequest.setId(BLOOD_REQUEST_ID);
        bloodRequest.setHospital(hospital);
        bloodRequest.setRequiredBloodUnits(3);
        bloodRequest.setBloodRequestStatus("OPEN");
        bloodRequest.setRequiredBloodType("O+");
        bloodRequest.setBloodRequestUrgency("HIGH");
        bloodRequest.setBloodNeededDate(
                LocalDate.now().plusDays(5)
        );
    }

    // TEST 1: Prevent changing a completed request.
    @Test
    void shouldRejectUpdatingCompletedBloodRequest() {

        bloodRequest.setBloodRequestStatus("COMPLETED");

        when(bloodRequestRepository.findByIdForUpdate(
                BLOOD_REQUEST_ID
        )).thenReturn(Optional.of(bloodRequest));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bloodRequestService.updateBloodRequestStatus(
                        BLOOD_REQUEST_ID,
                        "OPEN",
                        HOSPITAL_ID,
                        EMAIL
                )
        );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode()
        );

        assertEquals(
                "COMPLETED",
                bloodRequest.getBloodRequestStatus()
        );

        verify(bloodRequestRepository, never()).save(any());
        verifyNoInteractions(auditLogService);
    }

    // TEST 2: Prevent changing a cancelled request.
    @Test
    void shouldRejectUpdatingCancelledBloodRequest() {

        bloodRequest.setBloodRequestStatus("CANCELLED");

        when(bloodRequestRepository.findByIdForUpdate(
                BLOOD_REQUEST_ID
        )).thenReturn(Optional.of(bloodRequest));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bloodRequestService.updateBloodRequestStatus(
                        BLOOD_REQUEST_ID,
                        "OPEN",
                        HOSPITAL_ID,
                        EMAIL
                )
        );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode()
        );

        assertEquals(
                "CANCELLED",
                bloodRequest.getBloodRequestStatus()
        );

        verify(bloodRequestRepository, never()).save(any());
        verifyNoInteractions(auditLogService);
    }

    // TEST 3: Prevent another hospital from updating a request.
    @Test
    void shouldRejectUpdateFromAnotherHospital() {

        Long anotherHospitalId = 99L;

        when(bloodRequestRepository.findByIdForUpdate(
                BLOOD_REQUEST_ID
        )).thenReturn(Optional.of(bloodRequest));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bloodRequestService.updateBloodRequestStatus(
                        BLOOD_REQUEST_ID,
                        "CLOSED",
                        anotherHospitalId,
                        EMAIL
                )
        );

        assertEquals(
                HttpStatus.FORBIDDEN,
                exception.getStatusCode()
        );

        assertEquals(
                "OPEN",
                bloodRequest.getBloodRequestStatus()
        );

        verify(bloodRequestRepository, never()).save(any());
        verifyNoInteractions(auditLogService);
    }

    // TEST 4: Prevent deletion when bookings exist.
    @Test
    void shouldRejectDeletionWhenBookingsExist() {

        when(bloodRequestRepository.findByIdForUpdate(
                BLOOD_REQUEST_ID
        )).thenReturn(Optional.of(bloodRequest));

        // Simulate two existing bookings.
        when(jdbcTemplate.queryForObject(
                anyString(),
                eq(Long.class),
                eq(BLOOD_REQUEST_ID)
        )).thenReturn(2L);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bloodRequestService.deleteBloodRequest(
                        BLOOD_REQUEST_ID,
                        HOSPITAL_ID,
                        EMAIL
                )
        );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode()
        );

        verify(bloodRequestRepository, never()).delete(any());
        verifyNoInteractions(auditLogService);
    }

    // TEST 5: Delete an open request and record Audit Log.
    @Test
    void shouldDeleteOpenBloodRequestWithoutBookings() {

        when(bloodRequestRepository.findByIdForUpdate(
                BLOOD_REQUEST_ID
        )).thenReturn(Optional.of(bloodRequest));

        // Simulate no existing donation bookings.
        when(jdbcTemplate.queryForObject(
                anyString(),
                eq(Long.class),
                eq(BLOOD_REQUEST_ID)
        )).thenReturn(0L);

        assertDoesNotThrow(
                () -> bloodRequestService.deleteBloodRequest(
                        BLOOD_REQUEST_ID,
                        HOSPITAL_ID,
                        EMAIL
                )
        );

        verify(bloodRequestRepository).delete(bloodRequest);

        verify(auditLogService).saveAuditLog(
                eq("BLOOD_REQUEST_DELETED"),
                eq(EMAIL),
                contains("deleted by hospital")
        );
    }

    // TEST 6: Create a blood request and record Audit Log.
    @Test
    void shouldCreateBloodRequestAndWriteAuditLog() {

        BloodRequestRequest request =
                mock(BloodRequestRequest.class);

        when(request.getHospitalId())
                .thenReturn(HOSPITAL_ID);

        when(request.getRequiredBloodType())
                .thenReturn("O+");

        when(request.getRequiredBloodUnits())
                .thenReturn(3);

        when(request.getBloodRequestUrgency())
                .thenReturn("HIGH");

        when(request.getBloodNeededDate())
                .thenReturn(LocalDate.now().plusDays(5));

        when(hospitalRepository.findById(HOSPITAL_ID))
                .thenReturn(Optional.of(hospital));

        when(hospital.isHospitalActive())
                .thenReturn(true);

        when(bloodRequestRepository.save(
                any(BloodRequest.class)
        )).thenAnswer(invocation -> {

            BloodRequest saved =
                    invocation.getArgument(0);

            saved.setId(BLOOD_REQUEST_ID);

            return saved;
        });

        assertDoesNotThrow(
                () -> bloodRequestService.createBloodRequest(
                        request,
                        HOSPITAL_ID,
                        EMAIL
                )
        );

        verify(bloodRequestRepository).save(
                argThat(saved ->
                        "OPEN".equals(
                                saved.getBloodRequestStatus()
                        )
                                && HOSPITAL_ID.equals(
                                saved.getHospital().getId()
                        )
                )
        );

        verify(auditLogService).saveAuditLog(
                eq("BLOOD_REQUEST_CREATED"),
                eq(EMAIL),
                contains("created for hospital")
        );
    }

    // TEST 7: Update a blood request and record Audit Log.
    @Test
    void shouldUpdateBloodRequestAndWriteAuditLog() {

        BloodRequestRequest request =
                mock(BloodRequestRequest.class);

        when(request.getHospitalId())
                .thenReturn(HOSPITAL_ID);

        when(request.getRequiredBloodType())
                .thenReturn("A+");

        when(request.getRequiredBloodUnits())
                .thenReturn(5);

        when(request.getBloodRequestUrgency())
                .thenReturn("CRITICAL");

        when(request.getBloodNeededDate())
                .thenReturn(LocalDate.now().plusDays(7));

        when(bloodRequestRepository.findByIdForUpdate(
                BLOOD_REQUEST_ID
        )).thenReturn(Optional.of(bloodRequest));

        when(bloodRequestRepository.save(bloodRequest))
                .thenReturn(bloodRequest);

        assertDoesNotThrow(
                () -> bloodRequestService.updateBloodRequest(
                        BLOOD_REQUEST_ID,
                        request,
                        HOSPITAL_ID,
                        EMAIL
                )
        );

        assertEquals(
                "A+",
                bloodRequest.getRequiredBloodType()
        );

        assertEquals(
                5,
                bloodRequest.getRequiredBloodUnits()
        );

        assertEquals(
                "CRITICAL",
                bloodRequest.getBloodRequestUrgency()
        );

        verify(auditLogService).saveAuditLog(
                eq("BLOOD_REQUEST_UPDATED"),
                eq(EMAIL),
                contains("updated by hospital")
        );
    }

    // TEST 8: Close a blood request and record Audit Log.
    @Test
    void shouldCloseBloodRequestAndWriteAuditLog() {

        when(bloodRequestRepository.findByIdForUpdate(
                BLOOD_REQUEST_ID
        )).thenReturn(Optional.of(bloodRequest));

        when(bloodRequestRepository.save(bloodRequest))
                .thenReturn(bloodRequest);

        bloodRequestService.updateBloodRequestStatus(
                BLOOD_REQUEST_ID,
                "CLOSED",
                HOSPITAL_ID,
                EMAIL
        );

        assertEquals(
                "CLOSED",
                bloodRequest.getBloodRequestStatus()
        );

        verify(auditLogService).saveAuditLog(
                eq("BLOOD_REQUEST_CLOSED"),
                eq(EMAIL),
                contains("changed to CLOSED")
        );
    }

    // TEST 9: Cancel a blood request and record Audit Log.
    @Test
    void shouldCancelBloodRequestAndWriteAuditLog() {

        when(bloodRequestRepository.findByIdForUpdate(
                BLOOD_REQUEST_ID
        )).thenReturn(Optional.of(bloodRequest));

        when(bloodRequestRepository.save(bloodRequest))
                .thenReturn(bloodRequest);

        bloodRequestService.updateBloodRequestStatus(
                BLOOD_REQUEST_ID,
                "CANCELLED",
                HOSPITAL_ID,
                EMAIL
        );

        assertEquals(
                "CANCELLED",
                bloodRequest.getBloodRequestStatus()
        );

        verify(auditLogService).saveAuditLog(
                eq("BLOOD_REQUEST_CANCELLED"),
                eq(EMAIL),
                contains("changed to CANCELLED")
        );
    }

    // TEST 10: Reject reopening a closed blood request.
    @Test
    void shouldRejectReopeningClosedBloodRequestWithoutAudit() {

        bloodRequest.setBloodRequestStatus("CLOSED");

        when(bloodRequestRepository.findByIdForUpdate(
                BLOOD_REQUEST_ID
        )).thenReturn(Optional.of(bloodRequest));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bloodRequestService.updateBloodRequestStatus(
                        BLOOD_REQUEST_ID,
                        "OPEN",
                        HOSPITAL_ID,
                        EMAIL
                )
        );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode()
        );

        assertEquals(
                "CLOSED",
                bloodRequest.getBloodRequestStatus()
        );

        verify(bloodRequestRepository, never()).save(any());
        verifyNoInteractions(auditLogService);
    }
}
