package com.app.med_support.controller;

import com.app.med_support.model.User;
import com.app.med_support.request.*;
import com.app.med_support.response.AuthResponse;
import com.app.med_support.security.JWTUtils;
import com.app.med_support.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RequestMapping("/api/auth")
@RestController

public class UserController {
    //user controller can use user service
    private final UserService userService;
    private final JWTUtils jwtUtils;

    public UserController(UserService userService,JWTUtils jwtUtils) {

        this.userService = userService;
        this.jwtUtils=jwtUtils;
    }
    @PostMapping("/register")
    public AuthResponse registerUser(@RequestBody RegisterRequest registerRequest) {
        User user = userService.registerUser(registerRequest);
        if(user == null) {
            return new AuthResponse("Registration failed");
        }
        return new AuthResponse("Registration successful");
    }
    @PostMapping("/login")
    public AuthResponse loginUser(@RequestBody LoginRequest loginRequest) {
        User user = userService.loginUser(loginRequest);
        if(user == null) {
            return new AuthResponse("Login failed");
        }
        // Generate a JWT token after successful login
        String token = jwtUtils.generateJwtToken(user.getEmail());
        return new AuthResponse("Login successful", token);
    }
    @GetMapping("/verify-email")
    public AuthResponse verifyEmail(@RequestParam String token) {
        boolean verified = userService.verifyEmail(token);
        if(!verified) {
            return new AuthResponse("Verification link is invalid or expired");
        }
        return new AuthResponse("Email verified successfully");
    }
    @PostMapping("/forgot-password")
    public AuthResponse forgotPassword(@RequestParam String email) {
        System.out.println("FORGOT PASSWORD CONTROLLER REACHED");
        boolean sent = userService.forgotPassword(email);
        if(!sent) {
            return new AuthResponse("Password reset request failed");
        }
        return new AuthResponse("Password reset link sent successfully");
    }
    @PostMapping("/reset-password")
    public AuthResponse resetPassword(
            @RequestBody ResetPasswordRequest resetPasswordRequest) {
        boolean passwordReset = userService.resetPassword(resetPasswordRequest);
        if (!passwordReset) {
            return new AuthResponse("Password reset token is invalid or expired");
        }
        return new AuthResponse("Password reset successful");

    }
    @PutMapping("/change-password")
    public AuthResponse changePassword(
            @RequestBody ChangePasswordRequest changePasswordRequest,
            Authentication authentication) {
        String email = authentication.getName();
        User user = userService.getUserByEmail(email);
        boolean passwordChanged = userService.changePassword(
                user.getId(), changePasswordRequest);
        if (!passwordChanged) {
            return new AuthResponse("Password change failed. Please check your current password and try again.");
        }
        return new AuthResponse("Password changed successfully");

    }
    @PutMapping("/profile")
    public AuthResponse updateProfile(
            @RequestBody UpdateProfileRequest updateProfileRequest,
            Authentication authentication) {
        String email = authentication.getName();
        User user = userService.getUserByEmail(email);

        return new AuthResponse("Profile updated successfully");

    }
    @PostMapping("/profile/cpr")
    public AuthResponse uploadCprDocument(
            @RequestParam("file") MultipartFile cprDocument,
            Authentication authentication) {
        String email = authentication.getName();
        User user = userService.getUserByEmail(email);
        boolean cprUploaded = userService.uploadCprDocument(user.getId(), cprDocument);
        if (!cprUploaded) {
        return new AuthResponse("CPR upload failed. Please check the file and try again.");
        }
        return new AuthResponse("CPR document uploaded successfully");
    }


}
