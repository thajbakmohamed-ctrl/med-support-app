
package com.medsupport.identity_service.service;

import com.medsupport.identity_service.exception.ResourceConflictException;
import com.medsupport.identity_service.model.User;
import com.medsupport.identity_service.repository.UserRepository;
import com.medsupport.identity_service.request.RegisterRequest;
import com.medsupport.identity_service.request.ChangePasswordRequest;
import com.medsupport.identity_service.security.JWTUtils;
import com.medsupport.identity_service.request.StaffRegistrationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JWTUtils jwtUtils;

    @Mock
    private EmailService emailService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AuthService authService;

    private User user;

    private static final String EMAIL = "donor@example.com";

    @BeforeEach
    void setUp() {

        ReflectionTestUtils.setField(
                authService,
                "frontendUrl",
                "http://localhost:5500"
        );

        user = new User();
        user.setId(1L);
        user.setName("Test Donor");
        user.setEmail(EMAIL);
        user.setPhoneNumber("12345678");
        user.setRole("DONOR");
        user.setStatus("ACTIVE");
        user.setEmailVerified(true);
        user.setHashedPassword("encodedPassword");
    }

    // TEST 1: Register donor successfully.
    @Test
    void shouldRegisterDonorAndWriteAuditLog() {

        RegisterRequest request = mock(RegisterRequest.class);

        when(request.getEmail()).thenReturn(EMAIL);
        when(request.getPhoneNumber()).thenReturn("12345678");
        when(request.getRole()).thenReturn("DONOR");
        when(request.getName()).thenReturn("Test Donor");
        when(request.getPassword()).thenReturn("Password123");

        when(passwordEncoder.encode("Password123"))
                .thenReturn("encodedPassword");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {
                    User saved = invocation.getArgument(0);
                    saved.setId(1L);
                    return saved;
                });

        User result = authService.registerUser(request);

        assertNotNull(result);
        assertEquals("DONOR", result.getRole());
        assertEquals(EMAIL, result.getEmail());
        assertFalse(result.isEmailVerified());

        verify(userRepository).save(any(User.class));

        verify(emailService).sendVerificationEmail(
                eq(EMAIL),
                contains("http://localhost:5500/?token=")
        );

        verify(auditLogService).saveAuditLog(
                eq("USER_REGISTERED"),
                eq(EMAIL),
                contains("registered with role DONOR")
        );
    }

    // TEST 2: Prevent self-registration as hospital staff.
    @Test
    void shouldRejectHospitalStaffPublicRegistration() {

        RegisterRequest request = mock(RegisterRequest.class);

        when(request.getEmail()).thenReturn(EMAIL);
        when(request.getPhoneNumber()).thenReturn("12345678");
        when(request.getRole()).thenReturn("HOSPITAL_STAFF");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.registerUser(request)
        );

        assertEquals(
                "Public registration is available for donors only",
                exception.getMessage()
        );

        verify(userRepository, never()).save(any());
        verifyNoInteractions(auditLogService);
    }

    // TEST 3: Prevent duplicate email registration.
    @Test
    void shouldRejectDuplicateEmail() {

        RegisterRequest request = mock(RegisterRequest.class);

        when(request.getEmail()).thenReturn(EMAIL);

        when(userRepository.existsByEmail(EMAIL))
                .thenReturn(true);

        assertThrows(
                ResourceConflictException.class,
                () -> authService.registerUser(request)
        );

        verify(userRepository, never()).save(any());
        verifyNoInteractions(auditLogService);
    }

    // TEST 4: Verify email successfully.
    @Test
    void shouldVerifyEmailAndWriteAuditLog() {

        String token = "valid-verification-token";

        user.setEmailVerified(false);
        user.setVerificationToken(token);
        user.setVerificationTokenExpiry(
                LocalDateTime.now().plusHours(1)
        );

        when(userRepository.findByVerificationToken(token))
                .thenReturn(Optional.of(user));

        boolean result = authService.verifyEmail(token);

        assertTrue(result);
        assertTrue(user.isEmailVerified());
        assertNull(user.getVerificationToken());

        verify(userRepository).save(user);

        verify(auditLogService).saveAuditLog(
                eq("EMAIL_VERIFIED"),
                eq(EMAIL),
                contains("Email verified")
        );
    }

    // TEST 5: Reject expired verification token.
    @Test
    void shouldRejectExpiredVerificationToken() {

        String token = "expired-verification-token";

        user.setVerificationToken(token);
        user.setVerificationTokenExpiry(
                LocalDateTime.now().minusHours(1)
        );

        when(userRepository.findByVerificationToken(token))
                .thenReturn(Optional.of(user));

        boolean result = authService.verifyEmail(token);

        assertFalse(result);

        verify(userRepository, never()).save(any());
        verifyNoInteractions(auditLogService);
    }

    // TEST 6: Change password successfully.
    @Test
    void shouldChangePasswordAndWriteAuditLog() {

        ChangePasswordRequest request =
                mock(ChangePasswordRequest.class);

        when(request.getCurrentPassword())
                .thenReturn("OldPassword123");

        when(request.getNewPassword())
                .thenReturn("NewPassword456");

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "OldPassword123",
                "encodedPassword"
        )).thenReturn(true);

        when(passwordEncoder.matches(
                "NewPassword456",
                "encodedPassword"
        )).thenReturn(false);

        when(passwordEncoder.encode("NewPassword456"))
                .thenReturn("newEncodedPassword");

        boolean result = authService.changePasswordByEmail(
                EMAIL,
                request
        );

        assertTrue(result);
        assertEquals(
                "newEncodedPassword",
                user.getHashedPassword()
        );

        verify(userRepository).save(user);

        verify(auditLogService).saveAuditLog(
                eq("PASSWORD_CHANGED"),
                eq(EMAIL),
                contains("Password changed")
        );
    }

    // TEST 7: Reject incorrect current password.
    @Test
    void shouldRejectWrongCurrentPasswordWithoutAudit() {

        ChangePasswordRequest request =
                mock(ChangePasswordRequest.class);

        when(request.getCurrentPassword())
                .thenReturn("WrongPassword");

        when(userRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "WrongPassword",
                "encodedPassword"
        )).thenReturn(false);

        boolean result = authService.changePasswordByEmail(
                EMAIL,
                request
        );

        assertFalse(result);

        verify(userRepository, never()).save(any());
        verifyNoInteractions(auditLogService);
    }

    // TEST 8: Register hospital staff successfully.
    @Test
    void shouldRegisterHospitalStaffSuccessfully() {

        StaffRegistrationRequest request =
                new StaffRegistrationRequest();

        request.setName("Hospital Staff");
        request.setEmail("staff@example.com");
        request.setPhoneNumber("33445566");
        request.setPassword("StrongPassword123!");

        when(passwordEncoder.encode("StrongPassword123!"))
                .thenReturn("encodedStaffPassword");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {
                    User saved = invocation.getArgument(0);
                    saved.setId(10L);
                    return saved;
                });

        authService.registerHospitalStaff(
                request,
                "admin@example.com"
        );

        org.mockito.ArgumentCaptor<User> userCaptor =
                org.mockito.ArgumentCaptor.forClass(User.class);

        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();

        assertEquals("HOSPITAL_STAFF", savedUser.getRole());
        assertEquals("staff@example.com", savedUser.getEmail());
        assertEquals("encodedStaffPassword",
                savedUser.getHashedPassword());
        assertEquals("ACTIVE", savedUser.getStatus());
        assertFalse(savedUser.isEmailVerified());
        assertNotNull(savedUser.getVerificationToken());
        assertTrue(savedUser.getVerificationTokenExpiry()
                .isAfter(LocalDateTime.now()));

        verify(passwordEncoder).encode("StrongPassword123!");

        verify(emailService).sendVerificationEmail(
                eq("staff@example.com"),
                contains("http://localhost:5500/?token=")
        );

        verify(auditLogService).saveAuditLog(
                eq("HOSPITAL_STAFF_REGISTERED"),
                eq("admin@example.com"),
                contains("Hospital staff user ID 10")
        );
    }

    // TEST 9: Reject duplicate staff email.
    @Test
    void shouldRejectDuplicateStaffEmail() {

        StaffRegistrationRequest request =
                new StaffRegistrationRequest();

        request.setEmail("staff@example.com");

        when(userRepository.existsByEmail("staff@example.com"))
                .thenReturn(true);

        assertThrows(
                ResourceConflictException.class,
                () -> authService.registerHospitalStaff(
                        request,
                        "admin@example.com"
                )
        );

        verify(userRepository, never()).save(any());
        verifyNoInteractions(emailService);
        verifyNoInteractions(auditLogService);
    }

    // TEST 10: Reject missing admin identity.
    @Test
    void shouldRejectStaffRegistrationWithoutAdminIdentity() {

        StaffRegistrationRequest request =
                new StaffRegistrationRequest();

        assertThrows(
                IllegalArgumentException.class,
                () -> authService.registerHospitalStaff(
                        request,
                        null
                )
        );

        verify(userRepository, never()).save(any());
        verifyNoInteractions(emailService);
        verifyNoInteractions(auditLogService);
    }

}
