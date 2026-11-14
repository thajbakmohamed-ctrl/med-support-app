
package com.medsupport.blood_request_service.controller;

import com.medsupport.blood_request_service.client.IdentityClient;
import com.medsupport.blood_request_service.model.BloodRequest;
import com.medsupport.blood_request_service.request.BloodRequestRequest;
import com.medsupport.blood_request_service.response.BloodRequestResponse;
import com.medsupport.blood_request_service.service.BloodRequestService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false
            ) String authorization,
            @Valid @RequestBody BloodRequestRequest request) {

        Long hospitalId = identityClient.getVerifiedHospitalId(authorization);
        String performedByEmail = identityClient.getAuthenticatedEmail(authorization);

        return bloodRequestService.createBloodRequest(request, hospitalId, performedByEmail);
    }


    @PutMapping("/{id}")
    public BloodRequestResponse updateBloodRequest(
            @PathVariable Long id,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION,
                    required = false) String authorization,
            @Valid @RequestBody BloodRequestRequest request) {

        Long hospitalId = identityClient.getVerifiedHospitalId(authorization);

        String performedByEmail = identityClient.getAuthenticatedEmail(authorization);

        return bloodRequestService.updateBloodRequest(id, request, hospitalId, performedByEmail);
    }


    @PatchMapping("/{id}/status")
    public BloodRequestResponse updateBloodRequestStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION,
                    required = false) String authorization) {

        Long hospitalId = identityClient.getVerifiedHospitalId(authorization);

        String performedByEmail = identityClient.getAuthenticatedEmail(authorization);

        return bloodRequestService.updateBloodRequestStatus(id, status, hospitalId, performedByEmail);
    }


    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBloodRequest(
            @PathVariable Long id,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION,
                    required = false) String authorization) {

        Long hospitalId = identityClient.getVerifiedHospitalId(authorization);
        String performedByEmail = identityClient.getAuthenticatedEmail(authorization);
        bloodRequestService.deleteBloodRequest(id, hospitalId, performedByEmail);
    }



    // ADMIN: View all blood requests with pagination and sorting
    @GetMapping
    public Page<BloodRequestResponse> getAllBloodRequests(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION,
                    required = false) String authorization,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        identityClient.verifyAdmin(authorization);
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Page must be >= 0 and size must be between 1 and 100");
        }
        java.util.Set<String> allowedSortFields = java.util.Set.of(
                "id", "requiredBloodType",
                "requiredBloodUnits", "bloodRequestUrgency", "bloodNeededDate",
                "bloodRequestStatus", "createdAtBloodRequest", "updatedAtBloodRequest");

        if (!allowedSortFields.contains(sortBy)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid sortBy field");
        }

        if (!direction.equalsIgnoreCase("asc")
                && !direction.equalsIgnoreCase("desc")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Sort direction must be asc or desc");
        }

        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        return bloodRequestService.getAllBloodRequests(pageable)
                .map(BloodRequestResponse::from);
    }


    @GetMapping("/open")
    public List<BloodRequestResponse> getOpenBloodRequests(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION,
                    required = false) String authorization) {

        identityClient.verifyDonor(authorization);
        return bloodRequestService.getOpenBloodRequests()
                .stream()
                .map(BloodRequestResponse::from)
                .toList();
    }


    @GetMapping("/open/filter")
    public List<BloodRequestResponse> getOpenBloodRequestsByBloodType(
            @RequestParam String bloodType,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION,
                    required = false) String authorization) {

        identityClient.verifyDonor(authorization);
        return bloodRequestService
                .getOpenBloodRequestsByBloodType(bloodType)
                .stream()
                .map(BloodRequestResponse::from)
                .toList();
    }


    @GetMapping("/hospital")
    public List<BloodRequestResponse> getBloodRequestsByHospital(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION,
                    required = false) String authorization) {

        Long hospitalId = identityClient.getVerifiedHospitalId(authorization);

        return bloodRequestService.getBloodRequestsByHospital(hospitalId)
                .stream()
                .map(BloodRequestResponse::from)
                .toList();
    }



    @GetMapping("/{id}")
    public BloodRequestResponse getBloodRequestById(
            @PathVariable Long id,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION,
                    required = false) String authorization) {

        Long verifiedHospitalId = identityClient.verifyAdminOrHospitalStaff(authorization);

        BloodRequest bloodRequest = bloodRequestService.getBloodRequestById(id);

        if (verifiedHospitalId != null) {

            if (bloodRequest.getHospital() == null
                    || !verifiedHospitalId.equals(bloodRequest.getHospital().getId())) {

                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You cannot view another hospital's blood request");
            }
        }

        return BloodRequestResponse.from(bloodRequest);
    }

    @GetMapping("/open/{id}")
    public BloodRequestResponse getOpenBloodRequestById(
            @PathVariable Long id,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION,
                    required = false) String authorization) {

        identityClient.verifyDonor(authorization);

        BloodRequest bloodRequest = bloodRequestService.getBloodRequestById(id);

        if (!"OPEN".equals(bloodRequest.getBloodRequestStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Blood request is not open for booking");
        }

        return BloodRequestResponse.from(bloodRequest);
    }

}
