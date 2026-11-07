
package com.medsupport.identity_service.controller;

import com.medsupport.identity_service.response.UserResponse;
import com.medsupport.identity_service.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
