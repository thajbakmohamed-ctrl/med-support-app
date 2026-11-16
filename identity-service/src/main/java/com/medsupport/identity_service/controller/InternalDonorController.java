
package com.medsupport.identity_service.controller;

import com.medsupport.identity_service.service.DonorContactLookupService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@io.swagger.v3.oas.annotations.security.SecurityRequirement(name="internalKey")
@RestController
@RequestMapping("/api/internal/donors")
@PreAuthorize("hasRole('SERVICE')")
public class InternalDonorController {

    private final DonorContactLookupService donorContactLookupService;

    public InternalDonorController(DonorContactLookupService donorContactLookupService) {
        this.donorContactLookupService = donorContactLookupService;
    }

    @GetMapping("/{donorProfileId}/email")
    public ResponseEntity<DonorEmailResponse> getDonorEmail(
            @PathVariable Long donorProfileId
    ) {

        String email = donorContactLookupService.getDonorEmail(donorProfileId);

        return ResponseEntity.ok(new DonorEmailResponse(email));
    }

    public record DonorEmailResponse(String email) {
    }
}
