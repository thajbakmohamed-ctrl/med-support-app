
package com.medsupport.identity_service.service;

import com.medsupport.identity_service.exception.ResourceConflictException;
import com.medsupport.identity_service.model.User;
import com.medsupport.identity_service.repository.UserRepository;
import com.medsupport.identity_service.request.UpdateProfileRequest;
import com.medsupport.identity_service.response.UserResponse;
import com.medsupport.identity_service.util.FileValidationUtil;

import org.springframework.beans.factory.annotation.Value;
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

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Get current user's profile
    @Transactional(readOnly = true)
    public UserResponse getMyProfile(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new NoSuchElementException("User not found"));

        return UserResponse.from(user);
    }

    // Update current user's profile
    @Transactional
    public UserResponse updateMyProfile(
            String email,
            UpdateProfileRequest request) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new NoSuchElementException("User not found"));

        String newPhoneNumber = request.getPhoneNumber();

        if (!user.getPhoneNumber().equals(newPhoneNumber)
                && userRepository.existsByPhoneNumber(newPhoneNumber)) {
            throw new ResourceConflictException(
                    "Phone number already exists");
        }

        user.setName(request.getName());
        user.setPhoneNumber(newPhoneNumber);

        User updatedUser = userRepository.save(user);

        return UserResponse.from(updatedUser);
    }

    // Deactivate current user's account
    @Transactional
    public void deactivateMyAccount(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new NoSuchElementException("User not found"));

        if ("INACTIVE".equals(user.getStatus())) {
            return;
        }

        user.setStatus("INACTIVE");

        user.setVerificationToken(null);
        user.setVerificationTokenExpiry(null);
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);

        userRepository.save(user);
    }

    // Upload CPR document
    @Transactional
    public void uploadCprDocument(
            String email,
            MultipartFile file) throws IOException {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new NoSuchElementException("User not found"));

        if (!"DONOR".equals(user.getRole())) {
            throw new IllegalArgumentException(
                    "Only donors can upload CPR documents");
        }

        String extension =
                FileValidationUtil.validateFile(file, true);

        Path folder = Path.of(uploadDir, "cpr")
                .toAbsolutePath()
                .normalize();

        Files.createDirectories(folder);

        String fileName = UUID.randomUUID() + extension;
        Path filePath = folder.resolve(fileName);

        try {
            file.transferTo(filePath);

            user.setCprDocument(filePath.toString());
            userRepository.save(user);

        } catch (IOException | RuntimeException ex) {
            Files.deleteIfExists(filePath);
            throw ex;
        }
    }

    // Upload profile image
    @Transactional
    public void uploadProfileImage(
            String email,
            MultipartFile file) throws IOException {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new NoSuchElementException("User not found"));

        String extension =
                FileValidationUtil.validateFile(file, false);

        Path folder = Path.of(uploadDir, "profile")
                .toAbsolutePath()
                .normalize();

        Files.createDirectories(folder);

        String fileName = UUID.randomUUID() + extension;
        Path filePath = folder.resolve(fileName);

        try {
            file.transferTo(filePath);

            user.setProfileImage(filePath.toString());
            userRepository.save(user);

        } catch (IOException | RuntimeException ex) {
            Files.deleteIfExists(filePath);
            throw ex;
        }
    }
}
