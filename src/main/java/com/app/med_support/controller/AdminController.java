package com.app.med_support.controller;

import com.app.med_support.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }
    @PutMapping("/users/{userId}/reactivate")
    public String reactivateUser(@PathVariable Long userId) {
        boolean reactivated = userService.reactivateUser(userId);
        if (!reactivated) {
            return "User not found";
        }
        return "User reactivated successfully";
    }
}
