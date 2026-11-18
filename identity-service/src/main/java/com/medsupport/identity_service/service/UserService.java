
package com.medsupport.identity_service.service;

import com.medsupport.identity_service.exception.ResourceConflictException;
import com.medsupport.identity_service.model.User;
import com.medsupport.identity_service.repository.UserRepository;
import com.medsupport.identity_service.request.UpdateProfileRequest;
import com.medsupport.identity_service.response.UserResponse;
import com.medsupport.identity_service.util.FileValidationUtil;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;
    private final AuditLogService auditLogService;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<UserResponse> searchUsers(String search, int page) {
        if (page < 0 || search.length() > 100) throw new IllegalArgumentException("Invalid search or page");
        return userRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(search, search,
                org.springframework.data.domain.PageRequest.of(page, 20, org.springframework.data.domain.Sort.by("id")))
                .map(user -> new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getPhoneNumber(),
                        user.getRole(), user.getStatus(), user.isEmailVerified(), null, user.getHospitalId()));
    }

    @Transactional
    public UserResponse changeAccountStatus(Long id, boolean active, String actor) {
        User user = userRepository.findById(id).orElseThrow(() -> new NoSuchElementException("User not found"));
        if (!active && "ADMIN".equals(user.getRole()))
            throw new IllegalArgumentException("Administrator accounts cannot be deactivated here");
        user.setStatus(active ? "ACTIVE" : "INACTIVE");
        if (!active) {
            user.setPasswordResetToken(null); user.setPasswordResetTokenExpiry(null);
        }
        auditLogService.saveAuditLog(active ? "ACCOUNT_REACTIVATED" : "ACCOUNT_DEACTIVATED", actor, "User ID " + id);
        return UserResponse.from(userRepository.save(user));
    }


    public UserService(UserRepository userRepository,
            JdbcTemplate jdbcTemplate, AuditLogService auditLogService) {

        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.auditLogService = auditLogService;
    }


    @Transactional(readOnly = true)
    public UserResponse getMyProfile(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        return UserResponse.from(user);
    }


    @Transactional
    public UserResponse updateMyProfile(String email, UpdateProfileRequest request) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        String newPhoneNumber = request.getPhoneNumber();

        if (!user.getPhoneNumber().equals(newPhoneNumber)
                && userRepository.existsByPhoneNumber(newPhoneNumber)) {

            throw new ResourceConflictException("Phone number already exists");
        }

        user.setName(request.getName());
        user.setPhoneNumber(newPhoneNumber);


        User updatedUser = userRepository.save(user);

        auditLogService.saveAuditLog(
                "USER_PROFILE_UPDATED", email,
                "Profile updated for user ID " + updatedUser.getId());

        return UserResponse.from(updatedUser);

    }


    @Transactional
    public void deactivateMyAccount(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        if ("ADMIN".equals(user.getRole()))
            throw new IllegalArgumentException("Administrator accounts cannot be deactivated here");

        if ("INACTIVE".equals(user.getStatus())) {
            return;
        }

        user.setStatus("INACTIVE");

        user.setVerificationToken(null);
        user.setVerificationTokenExpiry(null);
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);

        userRepository.save(user);

        auditLogService.saveAuditLog(
                "ACCOUNT_DEACTIVATED", email,
                "Account deactivated for user ID " + user.getId());

    }


    @Transactional
    public UserResponse assignStaffToHospital(Long userId, Long hospitalId,
            String performedByEmail) {

        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("Invalid user ID");
        }

        if (hospitalId == null || hospitalId <= 0) {
            throw new IllegalArgumentException("Invalid hospital ID");
        }


        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));


        if (!"HOSPITAL_STAFF".equals(user.getRole())) {
            throw new IllegalArgumentException("Only hospital staff can be assigned to a hospital");
        }


        if (!"ACTIVE".equals(user.getStatus())) {
            throw new IllegalArgumentException("Hospital staff account must be active");
        }

        Integer hospitalCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM hospital WHERE id = ?",
                Integer.class, hospitalId);

        if (hospitalCount == null || hospitalCount == 0) {
            throw new NoSuchElementException("Hospital not found");
        }

        user.setHospitalId(hospitalId);


        User updatedUser = userRepository.save(user);


        auditLogService.saveAuditLog(
                "STAFF_ASSIGNED_TO_HOSPITAL", performedByEmail,
                "User ID " + updatedUser.getId()
                        + " assigned to hospital ID " + hospitalId);

        return UserResponse.from(updatedUser);

    }


    @Transactional
    public void uploadCprDocument(String email, MultipartFile file) throws IOException {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        if (!"DONOR".equals(user.getRole())) {
            throw new IllegalArgumentException("Only donors can upload CPR documents");
        }

        String extension = FileValidationUtil.validateFile(file, true);

        Path folder = Path.of(uploadDir, "cpr").toAbsolutePath().normalize();

        Files.createDirectories(folder);

        String fileName = UUID.randomUUID() + extension;
        Path filePath = folder.resolve(fileName);

        try {
            file.transferTo(filePath);

            user.setCprDocument(filePath.toString());
            userRepository.save(user);

            auditLogService.saveAuditLog(
                    "CPR_DOCUMENT_UPLOADED", email,
                    "CPR document updated for user ID " + user.getId());


        } catch (IOException | RuntimeException ex) {
            Files.deleteIfExists(filePath);
            throw ex;
        }
    }


    @Transactional
    public void uploadProfileImage(String email, MultipartFile file) throws IOException {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        String extension = FileValidationUtil.validateFile(file, false);

        Path folder = Path.of(uploadDir, "profile").toAbsolutePath().normalize();

        Files.createDirectories(folder);

        String fileName = UUID.randomUUID() + extension;
        Path filePath = folder.resolve(fileName);

        try {
            file.transferTo(filePath);

            user.setProfileImage(filePath.toString());
            userRepository.save(user);

            auditLogService.saveAuditLog(
                    "PROFILE_IMAGE_UPDATED", email,
                    "Profile image updated for user ID " + user.getId());


        } catch (IOException | RuntimeException ex) {
            Files.deleteIfExists(filePath);
            throw ex;
        }
    }
}
