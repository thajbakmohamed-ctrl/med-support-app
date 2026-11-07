
package com.medsupport.identity_service.controller;

import com.medsupport.identity_service.response.UserResponse;
import com.medsupport.identity_service.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.medsupport.identity_service.request.UpdateProfileRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.DeleteMapping;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyProfile(
            Authentication authentication) {

        UserResponse response = userService.getMyProfile(
                authentication.getName()
        );

        return ResponseEntity.ok(response);
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMyProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication) {

        UserResponse response = userService.updateMyProfile(
                authentication.getName(),
                request
        );

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deactivateMyAccount(
            Authentication authentication) {

        userService.deactivateMyAccount(
                authentication.getName()
        );

        return ResponseEntity.noContent().build();
    }


}
