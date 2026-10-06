package com.app.med_support.controller;

import com.app.med_support.model.BloodRequest;
import com.app.med_support.model.User;
import com.app.med_support.service.BloodRequestService;
import com.app.med_support.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/blood-requests")
@Tag(name = "Blood Requests",
        description = "APIs for creating, viewing, filtering, and updating blood requests. " +
                "Access is controlled based on DONOR, HOSPITAL_STAFF, and ADMIN roles.")
public class BloodRequestController {

    private final BloodRequestService bloodRequestService;
    private final UserService userService;

    public BloodRequestController(BloodRequestService bloodRequestService, UserService userService) {
        this.bloodRequestService = bloodRequestService;
        this.userService = userService;
    }


    // Create blood request
    @Operation(summary = "Create blood request",
            description = "Allows authenticated HOSPITAL_STAFF to create a blood request for their assigned hospital.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Blood request created successfully"),
            @ApiResponse(responseCode = "400", description = "Blood request creation failed because the submitted data is invalid"),
            @ApiResponse(responseCode = "401", description = "Authentication is required or the JWT token is invalid"),
            @ApiResponse(responseCode = "403", description = "Access denied. HOSPITAL_STAFF role is required"),
            @ApiResponse(responseCode = "404", description = "User or assigned hospital not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("hasRole('HOSPITAL_STAFF')")
    @PostMapping
    public ResponseEntity<BloodRequest> createBloodRequest(@RequestBody BloodRequest bloodRequest,
            Authentication authentication) {
        String email = authentication.getName();
        User user = userService.getUserByEmail(email);

        if (user == null || user.getHospital() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        bloodRequest.setHospital(user.getHospital());

        BloodRequest createdBloodRequest = bloodRequestService.createBloodRequest(bloodRequest);

        if (createdBloodRequest == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(createdBloodRequest);
    }


    // Get all blood requests
    @Operation(summary = "Get all blood requests",
            description = "Allows an authenticated ADMIN to view all blood requests with pagination and sorting support.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Blood requests retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication is required or the JWT token is invalid"),
            @ApiResponse(responseCode = "403", description = "Access denied. ADMIN role is required"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<Page<BloodRequest>> getAllBloodRequests(
            Pageable pageable) {

        Page<BloodRequest> bloodRequests = bloodRequestService.getAllBloodRequests(pageable);

        return ResponseEntity.status(HttpStatus.OK).body(bloodRequests);
    }


    // Get all open blood requests
    @Operation(summary = "Get open blood requests",
            description = "Allows an authenticated DONOR to view all blood requests that currently have OPEN status.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Open blood requests retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication is required or the JWT token is invalid"),
            @ApiResponse(responseCode = "403", description = "Access denied. DONOR role is required"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("hasRole('DONOR')")
    @GetMapping("/open")
    public ResponseEntity<List<BloodRequest>> getOpenBloodRequests() {
        List<BloodRequest> bloodRequests = bloodRequestService.getOpenBloodRequests();
        return ResponseEntity.status(HttpStatus.OK).body(bloodRequests);
    }


    // Get open blood requests by blood type
    @Operation(summary = "Get open blood requests by blood type",
            description = "Allows an authenticated DONOR to filter OPEN blood requests by the required blood type.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Open blood requests for the selected blood type retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication is required or the JWT token is invalid"),
            @ApiResponse(responseCode = "403", description = "Access denied. DONOR role is required"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("hasRole('DONOR')")
    @GetMapping("/open/blood-type/{bloodType}")
    public ResponseEntity<List<BloodRequest>> getOpenBloodRequestsByBloodType(
            @PathVariable String bloodType) {

        List<BloodRequest> bloodRequests = bloodRequestService.getOpenBloodRequestsByBloodType(bloodType);

        return ResponseEntity.status(HttpStatus.OK).body(bloodRequests);
    }


    // Get blood request by ID
    @Operation(summary = "Get blood request by ID",
            description = "Allows an authenticated ADMIN or HOSPITAL_STAFF user to retrieve a specific blood request by its ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Blood request retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication is required or the JWT token is invalid"),
            @ApiResponse(responseCode = "403", description = "Access denied. ADMIN or HOSPITAL_STAFF role is required"),
            @ApiResponse(responseCode = "404", description = "Blood request not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'HOSPITAL_STAFF')")
    @GetMapping("/{bloodRequestId}")
    public ResponseEntity<BloodRequest> getBloodRequestById(
            @PathVariable Long bloodRequestId) {

        BloodRequest bloodRequest = bloodRequestService.getBloodRequestById(bloodRequestId);

        if (bloodRequest == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.status(HttpStatus.OK).body(bloodRequest);
    }


    // Update blood request status
    @Operation(summary = "Update blood request status",
            description = "Allows authenticated HOSPITAL_STAFF to update the status of a blood request belonging to their own hospital. " +
                    "Allowed statuses are OPEN, CLOSED, and CANCELLED.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Blood request status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Status update failed because the status or request is invalid"),
            @ApiResponse(responseCode = "401", description = "Authentication is required or the JWT token is invalid"),
            @ApiResponse(responseCode = "403", description = "Access denied. HOSPITAL_STAFF role is required"),
            @ApiResponse(responseCode = "404", description = "User, hospital, or blood request not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("hasRole('HOSPITAL_STAFF')")
    @PutMapping("/{bloodRequestId}/status")
    public ResponseEntity<BloodRequest> updateBloodRequestStatus(
            @PathVariable Long bloodRequestId, @RequestParam String newStatus,
            Authentication authentication) {

        String email = authentication.getName();
        User user = userService.getUserByEmail(email);

        if (user == null || user.getHospital() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Long hospitalId = user.getHospital().getId();

        BloodRequest updatedBloodRequest = bloodRequestService.updateBloodRequestStatus(
                bloodRequestId, newStatus, hospitalId);

        if (updatedBloodRequest == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        return ResponseEntity.status(HttpStatus.OK).body(updatedBloodRequest);
    }


    // Get blood requests for logged-in hospital
    @Operation(summary = "Get hospital blood requests",
            description = "Allows authenticated HOSPITAL_STAFF to view all blood requests belonging to their assigned hospital.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Hospital blood requests retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication is required or the JWT token is invalid"),
            @ApiResponse(responseCode = "403", description = "Access denied. HOSPITAL_STAFF role is required"),
            @ApiResponse(responseCode = "404", description = "User or assigned hospital not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("hasRole('HOSPITAL_STAFF')")
    @GetMapping("/hospital")
    public ResponseEntity<List<BloodRequest>> getBloodRequestsByHospital(
            Authentication authentication) {

        String email = authentication.getName();
        User user = userService.getUserByEmail(email);

        if (user == null || user.getHospital() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Long hospitalId = user.getHospital().getId();

        List<BloodRequest> bloodRequests = bloodRequestService.getBloodRequestsByHospital(hospitalId);

        return ResponseEntity.status(HttpStatus.OK).body(bloodRequests);
    }
}