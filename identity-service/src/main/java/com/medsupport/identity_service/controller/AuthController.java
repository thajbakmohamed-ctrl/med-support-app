
package com.medsupport.identity_service.controller;

import com.medsupport.identity_service.model.User;
import com.medsupport.identity_service.request.LoginRequest;
import com.medsupport.identity_service.request.RegisterRequest;
import com.medsupport.identity_service.response.AuthResponse;
import com.medsupport.identity_service.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(
            @Valid @RequestBody RegisterRequest request) {

        User user = authService.registerUser(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "message", "User registered successfully",
                        "email", user.getEmail()
                ));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse response = authService.loginUser(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(
            @RequestParam String token) {

        boolean verified = authService.verifyEmail(token);

        if (!verified) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Invalid or expired token"));
        }

        return ResponseEntity.ok(
                Map.of("message", "Email verified successfully")
        );
    }
}
