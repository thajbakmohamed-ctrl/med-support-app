
package com.medsupport.identity_service.controller;

import com.medsupport.identity_service.model.DonorProfile;
import com.medsupport.identity_service.request.DonorProfileRequest;
import com.medsupport.identity_service.response.DonorProfileResponse;
import com.medsupport.identity_service.service.DonorProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/donor-profile")
public class DonorProfileController {

    private final DonorProfileService donorProfileService;

    public DonorProfileController(DonorProfileService donorProfileService) {
        this.donorProfileService = donorProfileService;
    }

    @PostMapping
    public ResponseEntity<DonorProfileResponse> createDonorProfile(
            @Valid @RequestBody DonorProfileRequest request,
            Authentication authentication) {

        DonorProfile profile = donorProfileService.createDonorProfile(
                authentication.getName(),
                request
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(DonorProfileResponse.from(profile));
    }

    @GetMapping("/me")
    public ResponseEntity<DonorProfileResponse> getMyDonorProfile(
            Authentication authentication) {

        DonorProfileResponse response = donorProfileService
                .getMyDonorProfile(authentication.getName());

        return ResponseEntity.ok(response);
    }

    @PutMapping("/me")
    public ResponseEntity<DonorProfileResponse> updateMyDonorProfile(
            @Valid @RequestBody DonorProfileRequest request,
            Authentication authentication) {

        DonorProfileResponse response =
                donorProfileService.updateMyDonorProfile(
                        authentication.getName(),
                        request
                );

        return ResponseEntity.ok(response);
    }


}
