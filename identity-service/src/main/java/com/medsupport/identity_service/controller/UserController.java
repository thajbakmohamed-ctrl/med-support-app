
package com.medsupport.identity_service.controller;

import com.medsupport.identity_service.request.UpdateProfileRequest;
import com.medsupport.identity_service.response.UserResponse;
import com.medsupport.identity_service.service.UserService;
import com.medsupport.identity_service.request.StaffRegistrationRequest;
import com.medsupport.identity_service.service.AuthService;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    private final AuthService authService;

    public UserController(UserService userService, AuthService authService) {

        this.userService = userService;
        this.authService = authService;
    }


    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyProfile(Authentication authentication) {

        UserResponse response = userService.getMyProfile(authentication.getName());

        return ResponseEntity.ok(response);
    }


    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMyProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication) {

        UserResponse response = userService.updateMyProfile(authentication.getName(), request);

        return ResponseEntity.ok(response);
    }


    @DeleteMapping("/me")
    public ResponseEntity<Void> deactivateMyAccount(Authentication authentication) {

        userService.deactivateMyAccount(authentication.getName());

        return ResponseEntity.noContent().build();
    }


    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/staff")
    public ResponseEntity<UserResponse> registerHospitalStaff(
            @Valid @RequestBody StaffRegistrationRequest request,
            Authentication authentication) {

        UserResponse response = authService.registerHospitalStaff(
                request, authentication.getName());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{userId}/hospital/{hospitalId}")
    public ResponseEntity<UserResponse> assignStaffToHospital(
            @PathVariable Long userId,
            @PathVariable Long hospitalId, Authentication authentication) {

        String performedByEmail = authentication.getName();

        UserResponse response = userService.assignStaffToHospital(userId,
                hospitalId, performedByEmail);

        return ResponseEntity.ok(response);
    }



    @PostMapping(value = "/profile/cpr",
            consumes = "multipart/form-data")
    public ResponseEntity<Map<String, String>> uploadCprDocument(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) throws IOException {

        userService.uploadCprDocument(authentication.getName(), file);

        return ResponseEntity.ok(Map.of(
                "message", "CPR document uploaded successfully"));
    }

    @PostMapping(value = "/profile/image",
            consumes = "multipart/form-data")
    public ResponseEntity<Map<String, String>> uploadProfileImage(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) throws IOException {

        userService.uploadProfileImage(authentication.getName(), file);

        return ResponseEntity.ok(Map.of(
                "message", "Profile image uploaded successfully"));
    }
}
