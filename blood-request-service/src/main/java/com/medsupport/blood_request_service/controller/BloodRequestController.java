
package com.medsupport.blood_request_service.controller;

import com.medsupport.blood_request_service.client.IdentityClient;
import com.medsupport.blood_request_service.request.BloodRequestRequest;
import com.medsupport.blood_request_service.response.BloodRequestResponse;
import com.medsupport.blood_request_service.service.BloodRequestService;

import jakarta.validation.Valid;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/blood-requests")
public class BloodRequestController {

    private final BloodRequestService bloodRequestService;
    private final IdentityClient identityClient;

    public BloodRequestController(BloodRequestService bloodRequestService,
            IdentityClient identityClient) {

        this.bloodRequestService = bloodRequestService;
        this.identityClient = identityClient;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BloodRequestResponse createBloodRequest(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION,
                    required = false
            ) String authorization,
            @Valid @RequestBody BloodRequestRequest request) {

        Long hospitalId = identityClient.getVerifiedHospitalId(authorization);

        return bloodRequestService.createBloodRequest(
                request, hospitalId);
    }

    @PutMapping("/{id}")
    public BloodRequestResponse updateBloodRequest(
            @PathVariable Long id,
            @RequestHeader(
                    value = HttpHeaders.AUTHORIZATION,
                    required = false
            ) String authorization,
            @Valid @RequestBody BloodRequestRequest request) {

        Long hospitalId = identityClient.getVerifiedHospitalId(authorization);

        return bloodRequestService.updateBloodRequest(
                id, request, hospitalId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBloodRequest(
            @PathVariable Long id,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
            String authorization) {

        Long hospitalId = identityClient.getVerifiedHospitalId(authorization);

        bloodRequestService.deleteBloodRequest(id, hospitalId);
    }

    @GetMapping("/{id}")
    public BloodRequestResponse getBloodRequestById(
            @PathVariable Long id) {

        return BloodRequestResponse.from(bloodRequestService.getBloodRequestById(id));
    }

    @GetMapping
    public List<BloodRequestResponse> getAllBloodRequests() {

        return bloodRequestService.getAllBloodRequests()
                .stream()
                .map(BloodRequestResponse::from)
                .toList();
    }
}
