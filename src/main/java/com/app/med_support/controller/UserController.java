package com.app.med_support.controller;

import com.app.med_support.model.User;
import com.app.med_support.request.LoginRequest;
import com.app.med_support.request.RegisterRequest;
import com.app.med_support.response.AuthResponse;
import com.app.med_support.service.UserService;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/auth")
@RestController
public class UserController {
    //user controller can use user service
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }
    @PostMapping("/register")
    public AuthResponse registerUser(@RequestBody RegisterRequest registerRequest) {
        User user = userService.registerUser(registerRequest);
        if (user == null) {
            return new AuthResponse("Registration failed");
        }
        return new AuthResponse("Registration successful");
    }
    @PostMapping("/login")
    public AuthResponse loginUser(@RequestBody LoginRequest loginRequest) {
        User user = userService.loginUser(loginRequest);
        if (user == null) {
            return new AuthResponse("Login failed");
        }
        return new AuthResponse("Login successful");
    }
    @GetMapping("/verify-email")
    public AuthResponse verifyEmail(@RequestParam String token) {
        boolean verified = userService.verifyEmail(token);
        if (!verified) {
            return new AuthResponse("Verification link is invalid or expired");
        }
        return new AuthResponse("Email verified successfully");
    }

}
