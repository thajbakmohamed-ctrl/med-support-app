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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.app.med_support.response.BloodRequestResponse;
import java.util.List;

@RestController
@RequestMapping("/api/blood-requests")
@Tag(name = "Blood Requests",
        description = "APIs for creating, viewing, filtering, and updating blood requests. " +
                "Access is controlled based on DONOR, HOSPITAL_STAFF, and ADMIN roles."
)
public class BloodRequestController {

    private final BloodRequestService bloodRequestService;
    private final UserService userService;

    public BloodRequestController(
            BloodRequestService bloodRequestService,
            UserService userService) {

        this.bloodRequestService = bloodRequestService;
        this.userService = userService;
    }

    private BloodRequestResponse toResponse(BloodRequest bloodRequest) {

        Long hospitalId = null;

        if (bloodRequest.getHospital() != null) {
            hospitalId = bloodRequest.getHospital().getId();
        }

        return new BloodRequestResponse(bloodRequest.getId(), hospitalId,
                bloodRequest.getRequiredBloodType(),
                bloodRequest.getRequiredBloodUnits(),
                bloodRequest.getBloodNeededDate(),
                bloodRequest.getBloodRequestUrgency(),
                bloodRequest.getBloodRequestStatus(),
                bloodRequest.getBloodRequestDescription());
    }


    @Operation(summary = "Create blood request",
            description = "Allows authenticated HOSPITAL_STAFF to create a blood request for their assigned hospital.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Blood request created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid blood units, blood type, urgency, date," +
                    " or hospital information"),
            @ApiResponse(responseCode = "401", description = "Authentication required or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "HOSPITAL_STAFF role is required"),
            @ApiResponse(responseCode = "404", description = "User or hospital not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("hasRole('HOSPITAL_STAFF')")
    @PostMapping
    public ResponseEntity<BloodRequestResponse> createBloodRequest(
            @RequestBody BloodRequest bloodRequest,
            Authentication authentication) {

        String email = authentication.getName();
        User user = userService.getUserByEmail(email);


        if (user == null || user.getHospital() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "User or assigned hospital not found");
        }
        bloodRequest.setHospital(user.getHospital());

        BloodRequest createdBloodRequest = bloodRequestService.createBloodRequest(bloodRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(createdBloodRequest));
    }


    @Operation(summary = "Get all blood requests",
            description = "Allows an authenticated ADMIN to view all blood requests with pagination and sorting.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Blood requests retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid pagination or sorting parameters"),
            @ApiResponse(responseCode = "401", description = "Authentication required or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "ADMIN role is required"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<Page<BloodRequestResponse>> getAllBloodRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        if (page < 0 || size <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Page must be zero or greater and size must be greater than zero");
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

        Page<BloodRequest> bloodRequests = bloodRequestService.getAllBloodRequests(pageable);
        Page<BloodRequestResponse> responses = bloodRequests.map(this::toResponse);

        return ResponseEntity.ok(responses);
    }


    @Operation(summary = "Get open blood requests",
            description = "Allows an authenticated DONOR to view all OPEN blood requests."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Open blood requests retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "DONOR role is required"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("hasRole('DONOR')")
    @GetMapping("/open")
    public ResponseEntity<List<BloodRequestResponse>> getOpenBloodRequests() {
        List<BloodRequest> bloodRequests =
                bloodRequestService.getOpenBloodRequests();

        List<BloodRequestResponse> responses = bloodRequests.stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "Get open blood requests by blood type",
            description = "Allows an authenticated DONOR to filter OPEN blood requests by blood type.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Open blood requests retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "DONOR role is required"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("hasRole('DONOR')")
    @GetMapping("/open/blood-type/{bloodType}")
    public ResponseEntity<List<BloodRequestResponse>> getOpenBloodRequestsByBloodType(
            @PathVariable String bloodType) {

        List<BloodRequest> bloodRequests = bloodRequestService.getOpenBloodRequestsByBloodType(bloodType);

        List<BloodRequestResponse> responses = bloodRequests.stream()
                .map(this::toResponse).toList();

        return ResponseEntity.ok(responses);
    }


    @Operation(summary = "Get blood request by ID",
            description = "Allows an ADMIN or HOSPITAL_STAFF to retrieve a blood request by its ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Blood request retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "ADMIN or HOSPITAL_STAFF role is required"),
            @ApiResponse(responseCode = "404", description = "Blood request not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'HOSPITAL_STAFF')")
    @GetMapping("/{bloodRequestId}")
    public ResponseEntity<BloodRequestResponse> getBloodRequestById(
            @PathVariable Long bloodRequestId,
            Authentication authentication) {

        String email = authentication.getName();
        User user = userService.getUserByEmail(email);

        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "User not found");
        }


        BloodRequest bloodRequest = bloodRequestService.getBloodRequestById(bloodRequestId);


        if ("HOSPITAL_STAFF".equals(user.getRole())) {

            if (user.getHospital() == null || bloodRequest.getHospital() == null
                    || !user.getHospital().getId().equals(bloodRequest.getHospital().getId())) {

                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You cannot view blood requests from another hospital");
            }
        }

        return ResponseEntity.ok(toResponse(bloodRequest));
    }

    @Operation(summary = "Update blood request status",
            description = "Allows HOSPITAL_STAFF to update their hospital's blood request status. " +
                    "Allowed statuses: OPEN, CLOSED, CANCELLED.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Blood request status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Missing or invalid blood request status"),
            @ApiResponse(responseCode = "401", description = "Authentication required or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "Access denied or blood request belongs to another hospital"),
            @ApiResponse(responseCode = "404", description = "User, hospital, or blood request not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("hasRole('HOSPITAL_STAFF')")
    @PutMapping("/{bloodRequestId}/status")
    public ResponseEntity<BloodRequestResponse> updateBloodRequestStatus(
            @PathVariable Long bloodRequestId, @RequestParam String newStatus,
            Authentication authentication) {

        String email = authentication.getName();
        User user = userService.getUserByEmail(email);


        if (user == null || user.getHospital() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "User or assigned hospital not found");
        }

        Long hospitalId = user.getHospital().getId();

        BloodRequest updatedBloodRequest = bloodRequestService.updateBloodRequestStatus(
                bloodRequestId, newStatus, hospitalId);


        return ResponseEntity.ok(toResponse(updatedBloodRequest));
    }


    @Operation(summary = "Get hospital blood requests",
            description = "Allows HOSPITAL_STAFF to view blood requests belonging to their assigned hospital.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Hospital blood requests retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required or invalid JWT token"),
            @ApiResponse(responseCode = "403", description = "HOSPITAL_STAFF role is required"),
            @ApiResponse(responseCode = "404", description = "User or assigned hospital not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("hasRole('HOSPITAL_STAFF')")
    @GetMapping("/hospital")
    public ResponseEntity<List<BloodRequestResponse>> getBloodRequestsByHospital(
            Authentication authentication) {

        String email = authentication.getName();
        User user = userService.getUserByEmail(email);

        if (user == null || user.getHospital() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "User or assigned hospital not found");
        }

        Long hospitalId = user.getHospital().getId();


        List<BloodRequest> bloodRequests = bloodRequestService.getBloodRequestsByHospital(hospitalId);

        List<BloodRequestResponse> responses = bloodRequests.stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(responses);
    }
}