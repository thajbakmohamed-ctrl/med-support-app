package com.app.med_support.service;

import com.app.med_support.model.Hospital;
import com.app.med_support.model.User;
import com.app.med_support.repository.UserRepository;
import com.app.med_support.request.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {
    // USER SERVICES WILL NEED REPOSITORY TO DEALS WITH USERS DATA
    private final UserRepository userRepository;



    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean verifyEmail(String verificationToken) {
        User user = userRepository.findByVerificationToken(verificationToken);
        if (user == null) {
            return false;
        }
        if (user.getVerificationTokenExpiry().isBefore(LocalDateTime.now())) {
            return false;
        }
        user.setEmailVerified(true);
        userRepository.save(user);
        return true;

    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }


    public User getUserById(Long userId) {
        return userRepository.findById(userId).orElse(null);
    }


    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }


    public User createUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            return null;
        }
        if (!user.getRole().equals("DONOR") && !user.getRole().equals("HOSPITAL_STAFF")
                && !user.getRole().equals("ADMIN")) {
            return null;
        }
        if (!user.getStatus().equals("ACTIVE") && !user.getStatus().equals("INACTIVE")) {
            return null;
        }
        return userRepository.save(user);
    }


    // THE FLOW IS ----
    // WE RECEIVED THE USER ID -- WE SEARCH THE ID -- IF NOT  EXISTS (NULL)
    // IF EXISTS WE UPDATE THE DATA THAT IS ABLE TO CHANGE -- NEXT SAVE
    public User updateUser(Long userId, UpdateProfileRequest updateProfileRequest) {
        User existingUser = userRepository.findById(userId).orElse(null);
        if (existingUser == null) {
            return null;
        }
        if (updateProfileRequest.getName() != null) {
            existingUser.setName(updateProfileRequest.getName());
        }

        if (updateProfileRequest.getPhoneNumber() != null) {

            if (!existingUser.getPhoneNumber().equals(updateProfileRequest.getPhoneNumber())
                    && userRepository.existsByPhoneNumber(updateProfileRequest.getPhoneNumber())) {
                return null;
            }

            existingUser.setPhoneNumber(updateProfileRequest.getPhoneNumber());
        }
        return userRepository.save(existingUser);
    }


    public boolean deleteUser(Long userId) {
        User user = userRepository.findById(userId).orElse(null);

        if (user == null) {
            return false;
        }
        user.setStatus("INACTIVE");
        userRepository.save(user);
        return true;
    }


    public User updateUserStatus(Long userId, String status) {
        User existingUser = userRepository.findById(userId).orElse(null);
        if (existingUser == null) {
            return null;
        }
        if (!status.equals("ACTIVE") && !status.equals("INACTIVE")) {
            return null;
        }
        existingUser.setStatus(status);
        return userRepository.save(existingUser);
    }


    public User verifyUserEmail(Long userId) {
        User existingUser = userRepository.findById(userId).orElse(null);
        if (existingUser == null) {
            return null;
        }
        existingUser.setEmailVerified(true);
        return userRepository.save(existingUser);
    }


    public User updateUserRole(Long userId, String role) {
        User existingUser = userRepository.findById(userId).orElse(null);
        if (existingUser == null) {
            return null;
        }
        if (!role.equals("DONOR") && !role.equals("HOSPITAL_STAFF") && !role.equals("ADMIN")) {
            return null;
        }
        existingUser.setRole(role);
        return userRepository.save(existingUser);
    }

    public User updateUserPassword(Long userId, String hashedPassword) {
        User existingUser = userRepository.findById(userId).orElse(null);
        if (existingUser == null) {
            return null;
        }
        existingUser.setHashedPassword(hashedPassword);
        return userRepository.save(existingUser);
    }


    public User assignHospitalToUser(Long userId, Hospital hospital) {
        User existingUser = userRepository.findById(userId).orElse(null);
        if (existingUser == null) {
            return null;
        }
        if (!existingUser.getRole().equals("HOSPITAL_STAFF")) {
            return null;
        }
        existingUser.setHospital(hospital);
        return userRepository.save(existingUser);
    }


    public boolean uploadCprDocument(Long userId, MultipartFile cprDocument) {
        User user = userRepository.findById(userId).orElse(null);
        if(user == null) {
            return false;
        }
        if(cprDocument == null || cprDocument.isEmpty()) {
            return false;
        }
        String contentType = cprDocument.getContentType();
        if(!"application/pdf".equals(contentType) && !"image/jpeg".equals(contentType)
                && !"image/png".equals(contentType)) {
            return false;
        }
        if (cprDocument.getSize() > 5 * 1024 * 1024) {
            return false;
        }
        Path uploadFolder = Paths.get("uploads/cpr");
        try {
            Files.createDirectories(uploadFolder);
        } catch (Exception e) {
            return false;
        }
        String extension;

        if ("application/pdf".equals(contentType)) {
            extension = ".pdf";
        } else if ("image/jpeg".equals(contentType)) {
            extension = ".jpg";
        } else {
            extension = ".png";
        }

        String fileName = UUID.randomUUID() + extension;
        Path filePath = uploadFolder.resolve(fileName);
        try {
            cprDocument.transferTo(filePath);
        } catch (Exception e) {
            return false;
        }
        user.setCprDocument(filePath.toString());
        userRepository.save(user);
        return true;
    }

    public boolean uploadProfileImage(Long userId, MultipartFile profileImage) {
        User user = userRepository.findById(userId).orElse(null);

        if (user == null) {
            return false;
        }

        if (profileImage == null || profileImage.isEmpty()) {
            return false;
        }

        // Profile image must be JPEG or PNG
        String contentType = profileImage.getContentType();

        if (!"image/jpeg".equals(contentType) && !"image/png".equals(contentType)) {
            return false;
        }

        // Maximum file size is 5 MB
        if (profileImage.getSize() > 5 * 1024 * 1024) {
            return false;
        }

        Path uploadFolder = Paths.get("uploads/profile");

        try {Files.createDirectories(uploadFolder);
        } catch (Exception e) {
            return false;
        }

        String extension;

        if ("application/pdf".equals(contentType)) {
            extension = ".pdf";
        } else if ("image/jpeg".equals(contentType)) {
            extension = ".jpg";
        } else {
            extension = ".png";
        }

        String fileName = UUID.randomUUID() + extension;

        Path filePath = uploadFolder.resolve(fileName);

        try {profileImage.transferTo(filePath);
        } catch (Exception e) {
            return false;
        }

        user.setProfileImage(filePath.toString());
        userRepository.save(user);
        return true;
    }
    public boolean reactivateUser(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }
        user.setStatus("ACTIVE");
        userRepository.save(user);
        return true;
    }

}

