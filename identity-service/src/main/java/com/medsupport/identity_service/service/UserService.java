
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
