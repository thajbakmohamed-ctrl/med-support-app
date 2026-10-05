package com.app.med_support.controller;

import com.app.med_support.model.User;
import com.app.med_support.request.RegisterRequest;
import com.app.med_support.response.AuthResponse;
import com.app.med_support.security.JWTUtils;
import com.app.med_support.service.AuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JWTUtils jwtUtils;

    public AuthController(AuthService authService, JWTUtils jwtUtils) {
        this.authService = authService;
        this.jwtUtils = jwtUtils;
    }
    @PostMapping("/register")
    public AuthResponse registerUser(@RequestBody RegisterRequest registerRequest) {

        User user = authService.registerUser(registerRequest);

        if (user == null) {
            return new AuthResponse("Registration failed");
        }

        return new AuthResponse("Registration successful");
    }

}
