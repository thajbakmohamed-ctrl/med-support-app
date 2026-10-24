package com.app.med_support.controller;

import com.app.med_support.service.DonorProfileService;
import com.app.med_support.service.UserService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.app.med_support.model.User;
import com.app.med_support.model.DonorProfile;
import com.app.med_support.request.DonorProfileRequest;


import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
@RestController
@RequestMapping("/api/donor-profile")
public class DonorProfileController {

    private final DonorProfileService donorProfileService;
    private final UserService userService;
    public DonorProfileController(
            DonorProfileService donorProfileService,
            UserService userService) {

        this.donorProfileService = donorProfileService;
        this.userService = userService;
    }

    @PreAuthorize("hasRole('DONOR')")
    @PostMapping
    public ResponseEntity<?> createDonorProfile(
            @RequestBody DonorProfileRequest request,
            Authentication authentication) {

        String email = authentication.getName();
        User user = userService.getUserByEmail(email);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        try {

            DonorProfile donorProfile = donorProfileService.createDonorProfile(user, request);

            return ResponseEntity.status(HttpStatus.CREATED).body(donorProfile);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}