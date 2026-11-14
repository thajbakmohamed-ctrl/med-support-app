
package com.medsupport.identity_service.service;

import com.medsupport.identity_service.exception.ResourceConflictException;
import com.medsupport.identity_service.model.User;
import com.medsupport.identity_service.repository.UserRepository;
import com.medsupport.identity_service.request.UpdateProfileRequest;
import com.medsupport.identity_service.response.UserResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long HOSPITAL_ID = 2L;
    private static final String EMAIL = "donor@example.com";
    private static final String ADMIN_EMAIL = "admin@example.com";

    @Mock
    private UserRepository userRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {

        user = new User();

        user.setId(USER_ID);
        user.setName("Test User");
        user.setEmail(EMAIL);
        user.setPhoneNumber("12345678");
        user.setRole("DONOR");
        user.setStatus("ACTIVE");
        user.setEmailVerified(true);
    }

    // TEST 1: Update profile successfully.
    @Test
    void shouldUpdateProfileAndWriteAuditLog() {

        UpdateProfileRequest request =
                mock(UpdateProfileRequest.class);

        when(request.getName())
                .thenReturn("Updated User");

        when(request.getPhoneNumber())
                .thenReturn("87654321");

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        when(userRepository.existsByPhoneNumber("87654321"))
                .thenReturn(false);

        when(userRepository.save(user))
                .thenReturn(user);

        UserResponse response =
                userService.updateMyProfile(EMAIL, request);

        assertNotNull(response);
        assertEquals("Updated User", user.getName());
        assertEquals("87654321", user.getPhoneNumber());

        verify(userRepository).save(user);

        verify(auditLogService).saveAuditLog(
                eq("USER_PROFILE_UPDATED"),
                eq(EMAIL),
                contains("Profile updated")
        );
    }

    // TEST 2: Prevent duplicate phone number.
    @Test
    void shouldRejectDuplicatePhoneNumber() {

        UpdateProfileRequest request =
                mock(UpdateProfileRequest.class);

        when(request.getPhoneNumber())
                .thenReturn("87654321");

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        when(userRepository.existsByPhoneNumber("87654321"))
                .thenReturn(true);

        assertThrows(
                ResourceConflictException.class,
                () -> userService.updateMyProfile(EMAIL, request)
        );

        verify(userRepository, never()).save(any());
        verifyNoInteractions(auditLogService);
    }

    // TEST 3: Deactivate account successfully.
    @Test
    void shouldDeactivateAccountAndWriteAuditLog() {

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        user.setVerificationToken("verify-token");
        user.setPasswordResetToken("reset-token");

        userService.deactivateMyAccount(EMAIL);

        assertEquals("INACTIVE", user.getStatus());
        assertNull(user.getVerificationToken());
        assertNull(user.getPasswordResetToken());

        verify(userRepository).save(user);

        verify(auditLogService).saveAuditLog(
                eq("ACCOUNT_DEACTIVATED"),
                eq(EMAIL),
                contains("Account deactivated")
        );
    }

    // TEST 4: Do not deactivate an inactive account again.
    @Test
    void shouldNotDeactivateAlreadyInactiveAccount() {

        user.setStatus("INACTIVE");

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        userService.deactivateMyAccount(EMAIL);

        verify(userRepository, never()).save(any());
        verifyNoInteractions(auditLogService);
    }

    // TEST 5: Assign hospital staff successfully.
    @Test
    void shouldAssignStaffToHospitalAndWriteAuditLog() {

        user.setRole("HOSPITAL_STAFF");

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));

        when(jdbcTemplate.queryForObject(
                anyString(),
                eq(Integer.class),
                eq(HOSPITAL_ID)
        )).thenReturn(1);

        when(userRepository.save(user))
                .thenReturn(user);

        UserResponse response =
                userService.assignStaffToHospital(
                        USER_ID,
                        HOSPITAL_ID,
                        ADMIN_EMAIL
                );

        assertNotNull(response);
        assertEquals(HOSPITAL_ID, user.getHospitalId());

        verify(userRepository).save(user);

        verify(auditLogService).saveAuditLog(
                eq("STAFF_ASSIGNED_TO_HOSPITAL"),
                eq(ADMIN_EMAIL),
                contains("assigned to hospital ID")
        );
    }

    // TEST 6: Prevent assigning a donor as hospital staff.
    @Test
    void shouldRejectAssigningDonorToHospital() {

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));

        assertThrows(
                IllegalArgumentException.class,
                () -> userService.assignStaffToHospital(
                        USER_ID,
                        HOSPITAL_ID,
                        ADMIN_EMAIL
                )
        );

        verify(userRepository, never()).save(any());
        verifyNoInteractions(auditLogService);
    }

    // TEST 7: Prevent assignment to missing hospital.
    @Test
    void shouldRejectAssignmentToMissingHospital() {

        user.setRole("HOSPITAL_STAFF");

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));

        when(jdbcTemplate.queryForObject(
                anyString(),
                eq(Integer.class),
                eq(HOSPITAL_ID)
        )).thenReturn(0);

        assertThrows(
                java.util.NoSuchElementException.class,
                () -> userService.assignStaffToHospital(
                        USER_ID,
                        HOSPITAL_ID,
                        ADMIN_EMAIL
                )
        );

        verify(userRepository, never()).save(any());
        verifyNoInteractions(auditLogService);
    }

    // TEST 8: Prevent assignment of inactive staff.
    @Test
    void shouldRejectAssigningInactiveStaff() {

        user.setRole("HOSPITAL_STAFF");
        user.setStatus("INACTIVE");

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));

        assertThrows(
                IllegalArgumentException.class,
                () -> userService.assignStaffToHospital(
                        USER_ID,
                        HOSPITAL_ID,
                        ADMIN_EMAIL
                )
        );

        verify(userRepository, never()).save(any());
        verifyNoInteractions(auditLogService);
    }

    // Helper is not needed; use repository mock directly.
}
