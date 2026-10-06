package com.app.med_support.controller;

import com.app.med_support.model.User;
import com.app.med_support.request.*;
import com.app.med_support.response.AuthResponse;
import com.app.med_support.security.JWTUtils;
import com.app.med_support.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication",
description = "APIs for user registration, login, email verification, password recovery, reset, and password changes.")
public class AuthController {

    private final AuthService authService;
    private final JWTUtils jwtUtils;

    public AuthController(AuthService authService, JWTUtils jwtUtils) {
        this.authService = authService;
        this.jwtUtils = jwtUtils;
    }

    // Register new user
    @Operation(summary = "Register a new user",
    description = "Creates a new DONOR or HOSPITAL_STAFF account and sends an email verification link.")
    @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "User registered successfully"),
    @ApiResponse(responseCode = "400", description = "Invalid registration data"),
    @ApiResponse(responseCode = "409", description = "Email or phone number already exists"),
    @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> registerUser(
    @Valid @RequestBody RegisterRequest registerRequest) {
        User user = authService.registerUser(registerRequest);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new AuthResponse("Registration failed"));
        }

        return ResponseEntity.status(HttpStatus.CREATED)
        .body(new AuthResponse("Registration successful"));
    }

    // Login user
    @Operation(summary = "Login user",
    description = "Authenticates a registered and verified user using email and password. " +
            "Returns a JWT token when authentication is successful.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200",
    description = "Login successful and JWT token returned"),
    @ApiResponse(responseCode = "401",
    description = "Invalid email or password, unverified email, or inactive account"),
    @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> loginUser(
            @RequestBody LoginRequest loginRequest) {

        User user = authService.loginUser(loginRequest);

        if(user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse("Login failed"));
        }

        String token = jwtUtils.generateJwtToken(user.getEmail());

        return ResponseEntity.status(HttpStatus.OK)
                .body(new AuthResponse("Login successful", token));
    }

    // Forgot password
    @Operation(summary = "Request password reset",
    description = "Sends a password reset link to the user's registered email address.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200",
    description = "Password reset link sent successfully"),
    @ApiResponse(responseCode = "400", description = "Password reset request failed"),
    @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<AuthResponse> forgotPassword(
            @RequestParam String email) {

        boolean sent = authService.forgotPassword(email);

        if(!sent) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AuthResponse("Password reset request failed"));
        }

        return ResponseEntity.status(HttpStatus.OK)
                .body(new AuthResponse("Password reset link sent successfully"));
    }

    // Reset password
    @Operation(summary = "Reset password",
    description = "Resets the user's password using a valid password reset token.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Password reset successfully"),
    @ApiResponse(responseCode = "400", description = "Password reset token is invalid or expired"),
    @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/reset-password")
    public ResponseEntity<AuthResponse> resetPassword(
            @RequestBody ResetPasswordRequest resetPasswordRequest) {

        boolean passwordReset = authService.resetPassword(resetPasswordRequest);

        if(!passwordReset) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AuthResponse("Password reset token is invalid or expired"));
        }

        return ResponseEntity.status(HttpStatus.OK)
                .body(new AuthResponse("Password reset successful"));
    }

    // Change password for logged-in user
    @Operation(
            summary = "Change password",
            description = "Allows an authenticated user to change their password after providing the correct current password."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Password changed successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Password change failed because the current password is incorrect or the request is invalid"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required or the JWT token is invalid"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    @PutMapping("/change-password")
    public ResponseEntity<AuthResponse> changePassword(@RequestBody ChangePasswordRequest changePasswordRequest,
            Authentication authentication) {
        String email = authentication.getName();
        boolean passwordChanged = authService.changePasswordByEmail(
        email, changePasswordRequest);

        if(!passwordChanged) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new AuthResponse("Password change failed. Please check your current password and try again."));
        }

        return ResponseEntity.status(HttpStatus.OK).body(new AuthResponse(
                "Password changed successfully"));
    }
}
