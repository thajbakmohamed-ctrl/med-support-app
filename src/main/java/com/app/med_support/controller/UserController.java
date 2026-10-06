package com.app.med_support.controller;

import com.app.med_support.model.User;
import com.app.med_support.request.*;
import com.app.med_support.response.AuthResponse;
import com.app.med_support.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@RequestMapping("/api/users")
@RestController
@Tag(name = "User Profile",
        description = "APIs for authenticated users to manage their profile," +
                " upload CPR documents, upload profile images, and deactivate their account.")

public class UserController {
    //user controller can use user service
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Update user profile",
    description = "Allows an authenticated user to update their own profile information, including name and phone number.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Profile updated successfully"),
    @ApiResponse(responseCode = "400", description = "Profile update failed because the submitted profile information is invalid"),
    @ApiResponse(responseCode = "401", description = "Authentication is required or the JWT token is invalid"),
    @ApiResponse(responseCode = "404", description = "User not found"),
    @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/profile")
    public ResponseEntity<AuthResponse> updateProfile(@RequestBody UpdateProfileRequest updateProfileRequest,
            Authentication authentication) {
        String email = authentication.getName();
        User user = userService.getUserByEmail(email);

        if(user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new AuthResponse("User not found"));
        }

        User updatedUser = userService.updateUser(user.getId(), updateProfileRequest);

        if (updatedUser == null) {return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST).body(new AuthResponse("Profile update failed"));
        }

        return ResponseEntity.status(HttpStatus.OK)
                .body(new AuthResponse("Profile updated successfully"));
    }


    // Upload CPR document
    @Operation(summary = "Upload CPR document",
    description = "Allows an authenticated user to upload their CPR document. " +
                    "Accepted file types are PDF, JPEG, and PNG. Maximum file size is 5 MB.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200",
    description = "CPR document uploaded successfully"),
    @ApiResponse(responseCode = "400", description = "CPR upload failed because the file is empty, unsupported, or larger than 5 MB"),
    @ApiResponse(responseCode = "401", description = "Authentication is required or the JWT token is invalid"),
    @ApiResponse(responseCode = "404", description = "User not found"),
    @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/profile/cpr")
    public ResponseEntity<AuthResponse> uploadCprDocument(@RequestParam("file") MultipartFile cprDocument,
            Authentication authentication) {
        String email = authentication.getName();
        User user = userService.getUserByEmail(email);

        if(user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AuthResponse("User not found"));
        }

        boolean cprUploaded = userService.uploadCprDocument(user.getId(), cprDocument);

        if(!cprUploaded) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AuthResponse("CPR upload failed. Please check the file and try again."));
        }

        return ResponseEntity.status(HttpStatus.OK)
                .body(new AuthResponse("CPR document uploaded successfully"));
    }
    // Upload profile image
    @Operation(summary = "Upload profile image",
            description = "Allows an authenticated user to upload a profile image. " +
                    "Accepted file types are JPEG and PNG. Maximum file size is 5 MB.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Profile image uploaded successfully"),
    @ApiResponse(responseCode = "400", description = "Profile image upload failed because the file is empty," +
            " unsupported, or larger than 5 MB"),
    @ApiResponse(responseCode = "401", description = "Authentication is required or the JWT token is invalid"),
    @ApiResponse(responseCode = "404", description = "User not found"),
    @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/profile/image")
    public ResponseEntity<AuthResponse> uploadProfileImage(@RequestParam("file") MultipartFile profileImage,
            Authentication authentication) {
        String email = authentication.getName();
        User user = userService.getUserByEmail(email);

        if(user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AuthResponse("User not found"));
        }

        boolean imageUploaded = userService.uploadProfileImage(user.getId(), profileImage);

        if (!imageUploaded) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AuthResponse("Profile image upload failed. Please check the file and try again."));
        }

        return ResponseEntity.status(HttpStatus.OK)
                .body(new AuthResponse("Profile image uploaded successfully"));
    }

    // Deactivate user account
    @Operation(summary = "Deactivate user account",
            description = "Allows an authenticated user to deactivate their own account. " +
                    "The account is soft deleted by changing its status to INACTIVE instead of permanently deleting it.")
    @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Account deactivated successfully"),
    @ApiResponse(responseCode = "400", description = "Account deactivation failed"),
    @ApiResponse(responseCode = "401", description = "Authentication is required or the JWT token is invalid"),
    @ApiResponse(responseCode = "404", description = "User not found"),
    @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/profile")
    public ResponseEntity<AuthResponse> deleteProfile(Authentication authentication) {
        String email = authentication.getName();
        User user = userService.getUserByEmail(email);

        if(user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AuthResponse("User not found"));
        }

        boolean deleted = userService.deleteUser(user.getId());

        if(!deleted) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AuthResponse("Account deactivation failed. Please try again."));
        }

        return ResponseEntity.status(HttpStatus.OK)
                .body(new AuthResponse("Account deactivated successfully"));
    }



}
