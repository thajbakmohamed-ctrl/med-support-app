
package com.medsupport.identity_service.controller;

import com.medsupport.identity_service.request.StaffRegistrationRequest;
import com.medsupport.identity_service.service.AuthService;
import com.medsupport.identity_service.service.UserService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserControllerSecurityTest {

    private AnnotationConfigApplicationContext context;
    private UserController userController;
    private UserService userService;
    private AuthService authService;

    @Configuration
    @EnableMethodSecurity
    static class TestSecurityConfig {

        @Bean
        UserService userService() {
            return mock(UserService.class);
        }

        @Bean
        AuthService authService() {
            return mock(AuthService.class);
        }

        @Bean
        UserController userController(
                UserService userService,
                AuthService authService) {

            return new UserController(
                    userService,
                    authService
            );
        }
    }

    @BeforeEach
    void setUp() {

        context = new AnnotationConfigApplicationContext(
                TestSecurityConfig.class
        );

        userController = context.getBean(UserController.class);
        userService = context.getBean(UserService.class);
        authService = context.getBean(AuthService.class);

        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        context.close();
    }

    private void authenticate(String email, String role) {

        var authentication =
                new UsernamePasswordAuthenticationToken(
                        email,
                        null,
                        List.of(new SimpleGrantedAuthority(role))
                );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);
    }

    // TEST 1: ADMIN can assign hospital staff.
    @Test
    void shouldAllowAdminToAssignStaff() {

        authenticate("admin@example.com", "ROLE_ADMIN");

        assertDoesNotThrow(() ->
                userController.assignStaffToHospital(
                        1L,
                        2L,
                        SecurityContextHolder.getContext()
                                .getAuthentication()
                )
        );

        verify(userService).assignStaffToHospital(
                1L,
                2L,
                "admin@example.com"
        );
    }

    // TEST 2: DONOR cannot assign hospital staff.
    @Test
    void shouldRejectDonorAssigningStaff() {

        authenticate("donor@example.com", "ROLE_DONOR");

        assertThrows(
                AccessDeniedException.class,
                () -> userController.assignStaffToHospital(
                        1L,
                        2L,
                        SecurityContextHolder.getContext()
                                .getAuthentication()
                )
        );

        verifyNoInteractions(userService);
    }

    // TEST 3: Missing authentication cannot assign staff.
    @Test
    void shouldRejectUnauthenticatedStaffAssignment() {

        SecurityContextHolder.clearContext();

        assertThrows(
                AuthenticationCredentialsNotFoundException.class,
                () -> userController.assignStaffToHospital(
                        1L,
                        2L,
                        null
                )
        );

        verifyNoInteractions(userService);
    }

    // TEST 4: ADMIN can register hospital staff.
    @Test
    void shouldAllowAdminToRegisterHospitalStaff() {

        authenticate("admin@example.com", "ROLE_ADMIN");

        StaffRegistrationRequest request =
                new StaffRegistrationRequest();

        request.setName("Hospital Staff");
        request.setEmail("staff@example.com");
        request.setPassword("StrongPassword123!");
        request.setPhoneNumber("33445566");

        assertDoesNotThrow(() ->
                userController.registerHospitalStaff(
                        request,
                        SecurityContextHolder.getContext()
                                .getAuthentication()
                )
        );

        verify(authService).registerHospitalStaff(
                same(request),
                eq("admin@example.com")
        );
    }

    // TEST 5: DONOR cannot register hospital staff.
    @Test
    void shouldRejectDonorRegisteringHospitalStaff() {

        authenticate("donor@example.com", "ROLE_DONOR");

        StaffRegistrationRequest request =
                new StaffRegistrationRequest();

        assertThrows(
                AccessDeniedException.class,
                () -> userController.registerHospitalStaff(
                        request,
                        SecurityContextHolder.getContext()
                                .getAuthentication()
                )
        );

        verifyNoInteractions(authService);
    }

    // TEST 6: Missing authentication cannot register staff.
    @Test
    void shouldRejectUnauthenticatedStaffRegistration() {

        SecurityContextHolder.clearContext();

        StaffRegistrationRequest request =
                new StaffRegistrationRequest();

        assertThrows(
                AuthenticationCredentialsNotFoundException.class,
                () -> userController.registerHospitalStaff(
                        request,
                        null
                )
        );

        verifyNoInteractions(authService);
    }
}
