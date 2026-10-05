package com.app.med_support.service;

import com.app.med_support.model.Hospital;
import com.app.med_support.model.User;
import com.app.med_support.repository.UserRepository;
import com.app.med_support.request.*;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;


    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    private String generateVerificationToken() {
        return UUID.randomUUID().toString();
    }

    @Transactional
    //  give me the registertion data and the service will do the account
    public User registerUser(RegisterRequest registerRequest) {
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            return null;
        }
        // this if statment -- the reg is only for donor and hospital staff
        if (!registerRequest.getRole().equals("DONOR")
                && !registerRequest.getRole().equals("HOSPITAL_STAFF")) {
            return null;
        }
        //RegisterRequest.name        -->  User.name
        //RegisterRequest.email       -->  User.email
        //RegisterRequest.phoneNumber --> User.phoneNumber
        //RegisterRequest.role        -->  User.role


        //for example
        // REGISTER REQUEST                 USER ENTITY

        // name = "Thajba"      ->  name = "Thajba"

        // email = "t@x.com"    ->  email = "t@x.com"

        //phone = "3333"       ->  phone = "3333"

        //role = "DONOR"       ->  role = "DONOR"

        //password = "1234"    ->  we need to hash the pass now
        User user = new User();
        user.setName(registerRequest.getName());
        user.setEmail(registerRequest.getEmail());
        user.setPhoneNumber(registerRequest.getPhoneNumber());
        user.setRole(registerRequest.getRole());
        user.setHashedPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setStatus("ACTIVE");
        // the flow for the verification is
        // to generate a unique verification token for the new user
        //set the vir link expiry 1d
        // save the user with the token also the expiry date
        //create a ver link using the saved user token
        //send the verification link to thr user email
        user.setEmailVerified(false);
        user.setVerificationToken(generateVerificationToken());
        user.setVerificationTokenExpiry(LocalDateTime.now().plusDays(1));
        User savedUser = userRepository.save(user);
        String verificationLink = "http://localhost:8080/api/auth/verify-email?token="
                + savedUser.getVerificationToken();
        emailService.sendVerificationEmail(savedUser.getEmail(), verificationLink);
        return savedUser;
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

    public User loginUser(LoginRequest loginRequest) {
        User user = userRepository.findByEmail(loginRequest.getEmail());
        if (user == null) {
            return null;
        }
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getHashedPassword())) {
            return null;
        }
        if (!user.isEmailVerified()) {
            return null;
        }
        if (!user.getStatus().equals("ACTIVE")) {
            return null;
        }
        return user;
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
        existingUser.setName(updateProfileRequest.getName());
        existingUser.setPhoneNumber(updateProfileRequest.getPhoneNumber());
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

    public boolean forgotPassword(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            return false;
        }
        user.setPasswordResetToken(UUID.randomUUID().toString());
        user.setPasswordResetTokenExpiry(LocalDateTime.now().plusHours(1));
        userRepository.save(user);
        String resetLink =
                "http://localhost:8080/api/auth/reset-password?token="
                        + user.getPasswordResetToken();
        emailService.sendPasswordResetEmail(user.getEmail(), resetLink);
        return true;

    }
    public boolean resetPassword(ResetPasswordRequest resetPasswordRequest) {
        User user = userRepository.findByPasswordResetToken(
                resetPasswordRequest.getPasswordResetToken());
        if (user == null) {
            return false;
        }
        if (user.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {
            return false;
        }
        user.setHashedPassword(passwordEncoder.encode(resetPasswordRequest.getNewPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        userRepository.save(user);

        return true;
    }
    public boolean changePassword(Long userId, ChangePasswordRequest changePasswordRequest) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }
        // we used matches here bec pas in the database is hased we cant compare with equals
        if (!passwordEncoder.matches(changePasswordRequest.getCurrentPassword(),
                user.getHashedPassword())) {
            return false;
        }
        user.setHashedPassword(passwordEncoder.encode(changePasswordRequest.getNewPassword()));
        userRepository.save(user);
        return true;

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
        String fileName = UUID.randomUUID() + "_" + cprDocument.getOriginalFilename();
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

