
package com.medsupport.identity_service.service;

import com.medsupport.identity_service.exception.ResourceConflictException;
import com.medsupport.identity_service.model.User;
import com.medsupport.identity_service.repository.UserRepository;
import com.medsupport.identity_service.request.RegisterRequest;
import com.medsupport.identity_service.request.ResetPasswordRequest;
import com.medsupport.identity_service.request.LoginRequest;
import com.medsupport.identity_service.request.ChangePasswordRequest;
import com.medsupport.identity_service.security.JWTUtils;
import com.medsupport.identity_service.response.AuthResponse;
import com.medsupport.identity_service.request.StaffRegistrationRequest;
import com.medsupport.identity_service.response.UserResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTUtils jwtUtils;
    private final EmailService emailService;
    private final AuditLogService auditLogService;

    @Value("${app.base-url:http://localhost:8083}")
    private String baseUrl;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
            JWTUtils jwtUtils, EmailService emailService, AuditLogService auditLogService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.emailService = emailService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public User registerUser(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceConflictException("Email already exists");
        }

        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new ResourceConflictException("Phone number already exists");
        }

        if (request.getPhoneNumber() == null || !request.getPhoneNumber().matches("\\d{8}")) {
            throw new IllegalArgumentException("Phone number must contain 8 digits");
        }


        if (!"DONOR".equals(request.getRole())) {
            throw new IllegalArgumentException("Public registration is available for donors only");
        }


        User user = new User();

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setRole(request.getRole());
        user.setHashedPassword(passwordEncoder.encode(request.getPassword()));

        user.setStatus("ACTIVE");
        user.setEmailVerified(false);
        user.setVerificationToken(UUID.randomUUID().toString());
        user.setVerificationTokenExpiry(LocalDateTime.now().plusDays(1));

        User savedUser = userRepository.save(user);
        auditLogService.saveAuditLog(
                "USER_REGISTERED",
                savedUser.getEmail(),
                "User ID " + savedUser.getId()
                        + " registered with role " + savedUser.getRole());

        String verificationLink = baseUrl + "/api/auth/verify-email?token="
                + savedUser.getVerificationToken();

        emailService.sendVerificationEmail(savedUser.getEmail(),
                verificationLink);

        return savedUser;
    }

    @Transactional
    public UserResponse registerHospitalStaff(StaffRegistrationRequest request, String performedByEmail) {

        if (performedByEmail == null || performedByEmail.isBlank()) {
            throw new IllegalArgumentException("Authenticated admin email is required");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceConflictException("Email already exists");
        }

        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new ResourceConflictException("Phone number already exists");
        }

        if (request.getPhoneNumber() == null || !request.getPhoneNumber().matches("\\d{8}")) {
            throw new IllegalArgumentException("Phone number must contain exactly 8 digits");
        }

        User user = new User();

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setRole("HOSPITAL_STAFF");
        user.setHashedPassword(passwordEncoder.encode(request.getPassword()));

        user.setStatus("ACTIVE");
        user.setEmailVerified(false);

        user.setVerificationToken(UUID.randomUUID().toString());

        user.setVerificationTokenExpiry(LocalDateTime.now().plusDays(1));

        User savedUser = userRepository.save(user);

        auditLogService.saveAuditLog(
                "HOSPITAL_STAFF_REGISTERED",
                performedByEmail,
                "Hospital staff user ID " + savedUser.getId()
                        + " created by admin");

        String verificationLink = baseUrl + "/api/auth/verify-email?token="
                + savedUser.getVerificationToken();

        emailService.sendVerificationEmail(savedUser.getEmail(), verificationLink);

        return UserResponse.from(savedUser);
    }

    @Transactional
    public boolean verifyEmail(String verificationToken) {

        User user = userRepository.findByVerificationToken(verificationToken)
                .orElse(null);

        if (user == null) {
            return false;
        }

        if (user.getVerificationTokenExpiry() == null ||
                !user.getVerificationTokenExpiry().isAfter(LocalDateTime.now())) {
            return false;
        }

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiry(null);

        userRepository.save(user);
        auditLogService.saveAuditLog(
                "EMAIL_VERIFIED", user.getEmail(),
                "Email verified for user ID " + user.getId());

        return true;
    }

    public AuthResponse loginUser(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail()).orElse(null);

        if (user == null) {
            throw new IllegalArgumentException(
                    "Invalid email or password");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getHashedPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        if (!user.isEmailVerified()) {
            throw new IllegalStateException("Email is not verified");
        }

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new IllegalStateException("Account is not active");
        }

        String token = jwtUtils.generateToken(user);

        return new AuthResponse(token, user.getEmail(), user.getRole());
    }

    @Transactional
    public void forgotPassword(String email) {

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            return;
        }

        user.setPasswordResetToken(UUID.randomUUID().toString());

        user.setPasswordResetTokenExpiry(LocalDateTime.now().plusHours(1));

        userRepository.save(user);
        auditLogService.saveAuditLog(
                "PASSWORD_RESET_REQUESTED", user.getEmail(),
                "Password reset requested for user ID " + user.getId());

        String resetLink = baseUrl + "/reset-password.html?token="
                + user.getPasswordResetToken();

        emailService.sendPasswordResetEmail(user.getEmail(), resetLink);
    }

    @Transactional
    public boolean resetPassword(ResetPasswordRequest request) {

        User user = userRepository.findByPasswordResetToken(
                        request.getPasswordResetToken()).orElse(null);

        if (user == null) {
            return false;
        }

        if (user.getPasswordResetTokenExpiry() == null ||
                !user.getPasswordResetTokenExpiry().isAfter(LocalDateTime.now())) {
            return false;
        }

        user.setHashedPassword(passwordEncoder.encode(
                        request.getNewPassword()));

        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);

        userRepository.save(user);
        auditLogService.saveAuditLog(
                "PASSWORD_RESET_COMPLETED", user.getEmail(),
                "Password reset completed for user ID " + user.getId());
        return true;
    }

    @Transactional
    public boolean changePasswordByEmail(String email, ChangePasswordRequest request) {

        User user = userRepository.findByEmail(email)
                .orElse(null);

        if (user == null) {
            return false;
        }
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getHashedPassword())) {
            return false;
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getHashedPassword())) {
            throw new IllegalArgumentException("New password must be different from current password");
        }

        user.setHashedPassword(passwordEncoder.encode(request.getNewPassword()));

        userRepository.save(user);
        auditLogService.saveAuditLog(
                "PASSWORD_CHANGED", user.getEmail(),
                "Password changed for user ID " + user.getId());
        return true;
    }
}
