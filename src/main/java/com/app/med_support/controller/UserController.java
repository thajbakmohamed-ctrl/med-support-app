package com.app.med_support.controller;

import com.app.med_support.model.User;
import com.app.med_support.request.LoginRequest;
import com.app.med_support.request.RegisterRequest;
import com.app.med_support.response.AuthResponse;
import com.app.med_support.service.UserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}
