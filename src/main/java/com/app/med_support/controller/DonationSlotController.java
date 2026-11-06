package com.app.med_support.controller;

import com.app.med_support.model.DonationSlot;
import com.app.med_support.model.User;
import com.app.med_support.response.DonationSlotResponse;
import com.app.med_support.service.DonationSlotService;
import com.app.med_support.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/donation-slots")
public class DonationSlotController {

    private final DonationSlotService donationSlotService;
    private final UserService userService;


    public DonationSlotController(DonationSlotService donationSlotService, UserService userService) {

        this.donationSlotService = donationSlotService;
        this.userService = userService;
    }

    @Operation(summary = "Get donation slot by ID",
            description = "Allows authenticated DONOR and HOSPITAL_STAFF users to retrieve a donation slot by its ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Donation slot retrieved successfully"),
            @ApiResponse(responseCode = "401",
                    description = "Authentication is required or the JWT token is invalid",
                    content = @Content),
            @ApiResponse(responseCode = "403",
                    description = "Access denied. DONOR or HOSPITAL_STAFF role is required",
                    content = @Content),
            @ApiResponse(responseCode = "404",
                    description = "Donation slot not found",
                    content = @Content),
            @ApiResponse(responseCode = "500",
                    description = "Internal server error",
                    content = @Content)
            })
    @PreAuthorize("hasAnyRole('DONOR', 'HOSPITAL_STAFF')")
    @GetMapping("/{slotId}")
    public ResponseEntity<DonationSlotResponse> getDonationSlotById(
            @Parameter(description = "Unique ID of the donation slot to retrieve.",
                    example = "3",
                    required = true)
            @PathVariable Long slotId) {

        DonationSlot slot = donationSlotService.getDonationSlotById(slotId);

        if (slot == null) {
            return ResponseEntity.notFound().build();
        }

        DonationSlotResponse response = donationSlotService.toResponse(slot);
        return ResponseEntity.ok(response);
    }
    @Operation(summary = "Get all donation slots",
            description = "Allows authenticated DONOR and HOSPITAL_STAFF users to retrieve a list of donation slots.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Donation slots retrieved successfully. The list may be empty"),
            @ApiResponse(responseCode = "401",
                    description = "Authentication is required or the JWT token is invalid",
                    content = @Content),
            @ApiResponse(responseCode = "403",
                    description = "Access denied. DONOR or HOSPITAL_STAFF role is required",
                    content = @Content),
            @ApiResponse(responseCode = "500",
                    description = "Internal server error",
                    content = @Content)
    })
    @PreAuthorize("hasAnyRole('DONOR', 'HOSPITAL_STAFF')")
    @GetMapping
    public ResponseEntity<List<DonationSlotResponse>> getAllDonationSlots() {

        List<DonationSlot> slots = donationSlotService.getAllDonationSlots();

        List<DonationSlotResponse> responses = slots.stream()
                .map(donationSlotService::toResponse).toList();

        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "Get available donation slots by hospital",
            description = "Allows authenticated DONOR and HOSPITAL_STAFF users to view available donation slots" +
                    " for a specific hospital. " +
                    "Only OPEN slots with remaining capacity and a valid upcoming start time are returned.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Available donation slots retrieved successfully. The list may be empty"),
            @ApiResponse(responseCode = "401",
                    description = "Authentication is required or the JWT token is invalid",
                    content = @Content),
            @ApiResponse(responseCode = "403",
                    description = "Access denied. DONOR or HOSPITAL_STAFF role is required",
                    content = @Content),
            @ApiResponse(responseCode = "500",
                    description = "Internal server error",
                    content = @Content)
    })
    @PreAuthorize("hasAnyRole('DONOR', 'HOSPITAL_STAFF')")
    @GetMapping("/available/{hospitalId}")
    public ResponseEntity<List<DonationSlotResponse>> getAvailableSlots(
            @Parameter(description = "ID of the hospital whose available donation slots will be retrieved.",
                    example = "1",
                    required = true)
            @PathVariable Long hospitalId) {

        List<DonationSlot> slots = donationSlotService.getAvailableSlotsByHospitalId(hospitalId);

        List<DonationSlotResponse> responses = slots.stream()
                .map(donationSlotService::toResponse).toList();

        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "Create donation slot",
            description = "Allows authenticated HOSPITAL_STAFF to create a donation slot for their own hospital. " +
                    "The hospital is automatically assigned from the logged-in staff account. " +
                    "The slot must have a valid date, start time, end time, and capacity.")
    @ApiResponses(value = {@ApiResponse(
                    responseCode = "201",
                    description = "Donation slot created successfully"),
            @ApiResponse(responseCode = "400",
                    description = "Donation slot creation rejected because the slot information is invalid",
                    content = @Content),
            @ApiResponse(responseCode = "401",
                    description = "Authentication is required or the JWT token is invalid",
                    content = @Content),
            @ApiResponse(responseCode = "403",
                    description = "Access denied. HOSPITAL_STAFF role is required",
                    content = @Content),
            @ApiResponse(responseCode = "404",
                    description = "Authenticated user or hospital association not found",
                    content = @Content),
            @ApiResponse(responseCode = "500",
                    description = "Internal server error",
                    content = @Content)
    })

    @PreAuthorize("hasRole('HOSPITAL_STAFF')")
    @PostMapping
    public ResponseEntity<DonationSlotResponse> createDonationSlot(
            @RequestBody DonationSlot slot,
            Authentication authentication) {

        String email = authentication.getName();

        User user = userService.getUserByEmail(email);

        if (user == null || user.getHospital() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        slot.setHospital(user.getHospital());

        DonationSlot savedSlot = donationSlotService.createDonationSlot(slot);

        if (savedSlot == null) {
            return ResponseEntity.badRequest().build();
        }

        DonationSlotResponse response = donationSlotService.toResponse(savedSlot);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Update donation slot capacity",
            description = "Allows authenticated HOSPITAL_STAFF to update the capacity of a donation slot belonging" +
                    " to their hospital. " +
                    "Only the capacity field is updated. The new capacity cannot be lower than the number of existing reservations."
    )@ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Donation slot capacity updated successfully"),
            @ApiResponse(responseCode = "400",
                    description = "Invalid capacity, slot not found, or slot does not belong to the staff member's hospital",
                    content = @Content),
            @ApiResponse(responseCode = "401",
                    description = "Authentication is required or the JWT token is invalid",
                    content = @Content),
            @ApiResponse(responseCode = "403",
                    description = "Access denied. HOSPITAL_STAFF role is required",
                    content = @Content),
            @ApiResponse(responseCode = "404",
                    description = "Authenticated user or hospital association not found",
                    content = @Content),
            @ApiResponse(responseCode = "500",
                    description = "Internal server error",
                    content = @Content)
    })
    @PreAuthorize("hasRole('HOSPITAL_STAFF')")
    @PutMapping("/{slotId}")
    public ResponseEntity<DonationSlotResponse> updateDonationSlot(
            @Parameter(description = "ID of the donation slot to update. " +
                            "The slot must belong to the authenticated staff member's hospital.",
                    example = "3",
                    required = true)
            @PathVariable Long slotId,
            @RequestBody DonationSlot updatedSlot, Authentication authentication) {

        String email = authentication.getName();
        User user = userService.getUserByEmail(email);

        if (user == null || user.getHospital() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Long hospitalId = user.getHospital().getId();

        DonationSlot slot = donationSlotService.updateDonationSlot(slotId, hospitalId, updatedSlot);

        if (slot == null) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(donationSlotService.toResponse(slot));
    }

    @Operation(summary = "Close donation slot",
            description = "Allows authenticated HOSPITAL_STAFF to close a donation slot belonging to their own hospital. " +
                    "A closed slot is no longer available for new donation bookings.")
    @ApiResponses(value = {@ApiResponse(
                    responseCode = "200",
                    description = "Donation slot closed successfully"),
            @ApiResponse(responseCode = "401",
                    description = "Authentication is required or the JWT token is invalid",
                    content = @Content),
            @ApiResponse(responseCode = "403",
                    description = "Access denied. HOSPITAL_STAFF role is required, or the staff user has no hospital association",
                    content = @Content),
            @ApiResponse(responseCode = "404",
                    description = "Donation slot not found or does not belong to the staff member's hospital",
                    content = @Content),
            @ApiResponse(responseCode = "500",
                    description = "Internal server error",
                    content = @Content)
    })
    @PreAuthorize("hasRole('HOSPITAL_STAFF')")
    @PatchMapping("/{slotId}/close")
    public ResponseEntity<DonationSlotResponse> closeDonationSlot(
            @Parameter(description = "ID of the donation slot to close. " +
                            "The slot must belong to the authenticated staff member's hospital.",
                    example = "3",
                    required = true)
            @PathVariable Long slotId, Authentication authentication) {

        String email = authentication.getName();
        User user = userService.getUserByEmail(email);

        if (user == null || user.getHospital() == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Long hospitalId = user.getHospital().getId();

        DonationSlot slot = donationSlotService.closeDonationSlot(slotId, hospitalId);

        if (slot == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.ok(donationSlotService.toResponse(slot));
    }



}
