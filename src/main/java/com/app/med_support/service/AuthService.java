package com.app.med_support.service;

import com.app.med_support.model.User;
import com.app.med_support.request.ChangePasswordRequest;
import com.app.med_support.request.LoginRequest;
import com.app.med_support.request.RegisterRequest;
import com.app.med_support.request.ResetPasswordRequest;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

import com.app.med_support.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class AuthService {
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
    private String generateVerificationToken() {
        return UUID.randomUUID().toString();
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

}
