
package com.medsupport.identity_service.service;

import com.medsupport.identity_service.model.User;
import com.medsupport.identity_service.repository.UserRepository;
import com.medsupport.identity_service.response.UserResponse;
import com.medsupport.identity_service.request.UpdateProfileRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    @Value("${app.upload-dir:uploads}")
    private String uploadDir;
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserResponse getMyProfile(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateMyProfile(
            String email,
            UpdateProfileRequest request) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        String newPhoneNumber = request.getPhoneNumber();

        // Check uniqueness only if phone number has changed
        if (!user.getPhoneNumber().equals(newPhoneNumber)
                && userRepository.existsByPhoneNumber(newPhoneNumber)) {
            throw new IllegalArgumentException(
                    "Phone number already exists");
        }

        user.setName(request.getName());
        user.setPhoneNumber(newPhoneNumber);

        User updatedUser = userRepository.save(user);

        return UserResponse.from(updatedUser);
    }

    @Transactional
    public void deactivateMyAccount(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        if ("INACTIVE".equals(user.getStatus())) {
            return;
        }

        user.setStatus("INACTIVE");

        // Invalidate outstanding verification and reset tokens
        user.setVerificationToken(null);
        user.setVerificationTokenExpiry(null);
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);

        userRepository.save(user);
    }

    @Transactional
    public void uploadCprDocument(
            String email,
            MultipartFile file) throws IOException {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found"));

        if (!"DONOR".equals(user.getRole())) {
            throw new IllegalArgumentException(
                    "Only donors can upload CPR documents");
        }

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "CPR document is required");
        }

        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException(
                    "Maximum file size is 5 MB");
        }

        String contentType = file.getContentType();
        String extension;

        if ("application/pdf".equals(contentType)) {
            extension = ".pdf";
        } else if ("image/jpeg".equals(contentType)) {
            extension = ".jpg";
        } else if ("image/png".equals(contentType)) {
            extension = ".png";
        } else {
            throw new IllegalArgumentException(
                    "Only PDF, JPG and PNG files are allowed");
        }

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


}
