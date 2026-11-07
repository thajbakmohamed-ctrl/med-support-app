
package com.medsupport.identity_service.service;

import com.medsupport.identity_service.model.User;
import com.medsupport.identity_service.repository.UserRepository;
import com.medsupport.identity_service.request.RegisterRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.medsupport.identity_service.request.LoginRequest;
import com.medsupport.identity_service.security.JWTUtils;
import com.medsupport.identity_service.response.AuthResponse;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTUtils jwtUtils;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JWTUtils jwtUtils) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
    }

    @Transactional
    public User registerUser(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new IllegalArgumentException("Phone number already exists");
        }

        if (request.getPhoneNumber() == null ||
                !request.getPhoneNumber().matches("\\d{8}")) {
            throw new IllegalArgumentException("Phone number must contain 8 digits");
        }

        if (!"DONOR".equals(request.getRole()) &&
                !"HOSPITAL_STAFF".equals(request.getRole())) {
            throw new IllegalArgumentException("Invalid role");
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

        return userRepository.save(user);
    }

    @Transactional
    public boolean verifyEmail(String verificationToken) {

        User user = userRepository
                .findByVerificationToken(verificationToken)
                .orElse(null);

        if (user == null) {
            return false;
        }

        if (user.getVerificationTokenExpiry() == null ||
                user.getVerificationTokenExpiry().isBefore(LocalDateTime.now())) {
            return false;
        }

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiry(null);

        userRepository.save(user);

        return true;
    }


    public AuthResponse loginUser(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElse(null);

        if (user == null) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        if (!passwordEncoder.matches(
                request.getPassword(), user.getHashedPassword())) {
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




}
