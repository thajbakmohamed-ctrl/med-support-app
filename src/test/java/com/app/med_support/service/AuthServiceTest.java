package com.app.med_support.service;

import com.app.med_support.model.User;
import com.app.med_support.repository.UserRepository;
import com.app.med_support.request.LoginRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        authService = new AuthService(userRepository, passwordEncoder, emailService);
    }

    // Test 1:
    // Login should fail when the user does not exist
    @Test
    void shouldReturnNullWhenLoginUserDoesNotExist() {

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("user@test.com");
        loginRequest.setPassword("Password123");

        when(userRepository.findByEmail("user@test.com")).thenReturn(null);
        User result = authService.loginUser(loginRequest);

        assertNull(result);
    }

    // Test 2:
    // Login should fail when the password is incorrect
    @Test
    void shouldReturnNullWhenLoginPasswordIsIncorrect() {

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("user@test.com");
        loginRequest.setPassword("WrongPassword");

        User user = new User();
        user.setEmail("user@test.com");
        user.setHashedPassword("hashedPassword");

        when(userRepository.findByEmail("user@test.com")).thenReturn(user);

        when(passwordEncoder.matches("WrongPassword", "hashedPassword"))
                .thenReturn(false);

        User result = authService.loginUser(loginRequest);

        assertNull(result);
    }

    // Test 3:
    // User cannot login before verifying their email
    @Test
    void shouldReturnNullWhenEmailIsNotVerified() {

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("user@test.com");
        loginRequest.setPassword("Password123");

        User user = new User();
        user.setEmail("user@test.com");
        user.setHashedPassword("hashedPassword");
        user.setEmailVerified(false);

        when(userRepository.findByEmail("user@test.com")).thenReturn(user);

        when(passwordEncoder.matches("Password123", "hashedPassword"))
                .thenReturn(true);

        User result = authService.loginUser(loginRequest);

        assertNull(result);
    }

    // Test 4:
    // Inactive users cannot login
    @Test
    void shouldReturnNullWhenUserAccountIsInactive() {

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("user@test.com");
        loginRequest.setPassword("Password123");

        User user = new User();
        user.setEmail("user@test.com");
        user.setHashedPassword("hashedPassword");
        user.setEmailVerified(true);
        user.setStatus("INACTIVE");

        when(userRepository.findByEmail("user@test.com")).thenReturn(user);

        when(passwordEncoder.matches("Password123", "hashedPassword"))
                .thenReturn(true);

        User result = authService.loginUser(loginRequest);

        assertNull(result);
    }

    // Test 5:
    // Active and verified user can login successfully
    @Test
    void shouldLoginSuccessfullyWhenUserDataIsValid() {

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("user@test.com");
        loginRequest.setPassword("Password123");

        User user = new User();
        user.setEmail("user@test.com");
        user.setHashedPassword("hashedPassword");
        user.setEmailVerified(true);
        user.setStatus("ACTIVE");

        when(userRepository.findByEmail("user@test.com")).thenReturn(user);

        when(passwordEncoder.matches("Password123", "hashedPassword"))
                .thenReturn(true);

        User result = authService.loginUser(loginRequest);

        assertNotNull(result);
        assertEquals("user@test.com", result.getEmail());
    }
}