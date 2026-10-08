package com.app.med_support.service;

import com.app.med_support.model.User;
import com.app.med_support.request.ChangePasswordRequest;
import com.app.med_support.request.LoginRequest;
import com.app.med_support.request.RegisterRequest;
import com.app.med_support.request.ResetPasswordRequest;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

import com.app.med_support.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }
    @Transactional
    //  give me the registertion data and the service will do the account
    public User registerUser(RegisterRequest registerRequest) {
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            return null;
        }
        // Phone number must be unique
        if (userRepository.existsByPhoneNumber(registerRequest.getPhoneNumber())) {
            return null;
        }
        // Phone number must contain exactly 8 digits
        if (registerRequest.getPhoneNumber() == null
                || !registerRequest.getPhoneNumber().matches("\\d{8}")) {
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
        // Log successful registration
        logger.info("New user registered successfully with email: {}", savedUser.getEmail());
        return savedUser;
    }
    private String generateVerificationToken() {
        return UUID.randomUUID().toString();
    }
    public User loginUser(LoginRequest loginRequest) {

        User user = userRepository.findByEmail(loginRequest.getEmail());

        if (user == null) {logger.warn("Login failed: user not found for email: {}",
                    loginRequest.getEmail());
            return null;
        }

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getHashedPassword())) {
            logger.warn("Login failed: incorrect password for email: {}", loginRequest.getEmail());
            return null;
        }

        if (!user.isEmailVerified()) {
            logger.warn("Login failed: email is not verified for user: {}", loginRequest.getEmail());
            return null;
        }

        if (!user.getStatus().equals("ACTIVE")) {
            logger.warn("Login failed: inactive account for email: {}", loginRequest.getEmail());
            return null;
        }

        logger.info("User logged in successfully with email: {}", user.getEmail());

        return user;
    }
    public boolean verifyEmail(String verificationToken) {
        User user = userRepository.findByVerificationToken(verificationToken);
        if (user == null) {
            logger.warn("Email verification failed: invalid verification token");
            return false;
        }

        if (user.getVerificationTokenExpiry().isBefore(LocalDateTime.now())) {
            logger.warn("Email verification failed: verification token expired for email: {}",
                    user.getEmail());
            return false;
        }

        user.setEmailVerified(true);
        userRepository.save(user);
        logger.info("Email verified successfully for user: {}", user.getEmail());
        return true;
    }
    public boolean forgotPassword(String email) {

        User user = userRepository.findByEmail(email);

        if (user == null) {logger.warn("Password reset request failed: user not found for email: {}",
                    email);
            return false;
        }

        user.setPasswordResetToken(UUID.randomUUID().toString());
        user.setPasswordResetTokenExpiry(LocalDateTime.now().plusHours(1));
        userRepository.save(user);

        String resetLink = "http://localhost:8080/api/auth/reset-password?token="
                + user.getPasswordResetToken();

        emailService.sendPasswordResetEmail(user.getEmail(), resetLink);

        logger.info("Password reset email sent successfully to: {}",
                user.getEmail());
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
    public boolean changePasswordByEmail(String email, ChangePasswordRequest changePasswordRequest) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            return false;
        }
        if (!passwordEncoder.matches(changePasswordRequest.getCurrentPassword(), user.getHashedPassword())) {
            return false;
        }
        user.setHashedPassword(passwordEncoder.encode(changePasswordRequest.getNewPassword()));
        userRepository.save(user);
        return true;
    }


}
