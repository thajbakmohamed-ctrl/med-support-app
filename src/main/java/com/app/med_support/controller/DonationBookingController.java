package com.app.med_support.controller;

import com.app.med_support.model.DonationBooking;
import com.app.med_support.model.DonorProfile;
import com.app.med_support.model.User;
import com.app.med_support.repository.DonorProfileRepository;
import com.app.med_support.service.DonationBookingService;
import com.app.med_support.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/donation-bookings")
@Tag(name = "Donation Bookings",
        description = "APIs for creating, viewing, updating, and cancelling donation bookings. " +
                "Access is controlled based on DONOR and HOSPITAL_STAFF roles.")
public class DonationBookingController {

    private final DonationBookingService donationBookingService;
    private final UserService userService;
    private final DonorProfileRepository donorProfileRepository;

    public DonationBookingController(
            DonationBookingService donationBookingService,
            UserService userService,
            DonorProfileRepository donorProfileRepository) {

        this.donationBookingService = donationBookingService;
        this.userService = userService;
        this.donorProfileRepository = donorProfileRepository;
    }


    // Create donation booking
    @Operation(summary = "Create donation booking",
            description = "Allows an authenticated DONOR to create a donation booking for an OPEN blood request. " +
                    "The booking date and time must be valid and duplicate active bookings are not allowed.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Donation booking created successfully"),
            @ApiResponse(responseCode = "400", description = "Donation booking creation failed because the booking information is invalid"),
            @ApiResponse(responseCode = "401", description = "Authentication is required or the JWT token is invalid"),
            @ApiResponse(responseCode = "403", description = "Access denied. DONOR role is required"),
            @ApiResponse(responseCode = "404", description = "User or donor profile not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("hasRole('DONOR')")
    @PostMapping
    public ResponseEntity<DonationBooking> createDonationBooking(
            @RequestBody DonationBooking donationBooking,
            Authentication authentication) {

        String email = authentication.getName();
        User user = userService.getUserByEmail(email);

        if(user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        DonorProfile donorProfile = donorProfileRepository.findByUserId(user.getId());

        if (donorProfile == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        donationBooking.setDonorProfile(donorProfile);

        DonationBooking createdBooking = donationBookingService.createDonationBooking(donationBooking);

        if (createdBooking == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(createdBooking);
    }


    // Get bookings for logged-in donor
    @Operation(summary = "Get donor bookings",
            description = "Allows an authenticated DONOR to view all donation bookings belonging to their own donor profile.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Donation bookings retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication is required or the JWT token is invalid"),
            @ApiResponse(responseCode = "403", description = "Access denied. DONOR role is required"),
            @ApiResponse(responseCode = "404", description = "User or donor profile not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("hasRole('DONOR')")
    @GetMapping("/donor")
    public ResponseEntity<List<DonationBooking>> getBookingsByDonor(
            Authentication authentication) {

        String email = authentication.getName();
        User user = userService.getUserByEmail(email);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        DonorProfile donorProfile = donorProfileRepository.findByUserId(user.getId());

        if (donorProfile == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        List<DonationBooking> bookings = donationBookingService.getBookingsByDonor(donorProfile.getId());

        return ResponseEntity.status(HttpStatus.OK).body(bookings);
    }


    // Get bookings for a blood request
    @Operation(summary = "Get bookings by blood request",
            description = "Allows authenticated HOSPITAL_STAFF to view donation bookings for a blood request belonging to their own hospital.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Donation bookings retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication is required or the JWT token is invalid"),
            @ApiResponse(responseCode = "403", description = "Access denied. HOSPITAL_STAFF role is required"),
            @ApiResponse(responseCode = "404", description = "User, hospital, or blood request not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("hasRole('HOSPITAL_STAFF')")
    @GetMapping("/blood-request/{bloodRequestId}")
    public ResponseEntity<List<DonationBooking>> getBookingsByBloodRequest(
            @PathVariable Long bloodRequestId,
            Authentication authentication) {

        String email = authentication.getName();
        User user = userService.getUserByEmail(email);

        if (user == null || user.getHospital() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Long hospitalId = user.getHospital().getId();

        List<DonationBooking> bookings = donationBookingService.getBookingsByBloodRequest(
                        bloodRequestId, hospitalId);

        return ResponseEntity.status(HttpStatus.OK).body(bookings);
    }


    // Update donation booking status
    @Operation(summary = "Update donation booking status",
            description = "Allows authenticated HOSPITAL_STAFF to update the status of a donation booking. " +
                    "Valid status transitions are enforced by the booking service.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Donation booking status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Status update failed because the status or status transition is invalid"),
            @ApiResponse(responseCode = "401", description = "Authentication is required or the JWT token is invalid"),
            @ApiResponse(responseCode = "403", description = "Access denied. HOSPITAL_STAFF role is required"),
            @ApiResponse(responseCode = "404", description = "Donation booking not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("hasRole('HOSPITAL_STAFF')")
    @PutMapping("/{bookingId}/status")
    public ResponseEntity<DonationBooking> updateDonationBookingStatus(
            @PathVariable Long bookingId,
            @RequestParam String newStatus) {

        DonationBooking updatedBooking = donationBookingService.updateDonationBookingStatus(
                bookingId, newStatus);

        if (updatedBooking == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        return ResponseEntity.status(HttpStatus.OK).body(updatedBooking);
    }


    // Cancel donation booking
    @Operation(summary = "Cancel donation booking",
            description = "Allows an authenticated DONOR to cancel one of their own donation bookings. " +
                    "Completed or already cancelled bookings cannot be cancelled.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Donation booking cancelled successfully"),
            @ApiResponse(responseCode = "400", description = "Booking cancellation failed because the booking cannot be cancelled"),
            @ApiResponse(responseCode = "401", description = "Authentication is required or the JWT token is invalid"),
            @ApiResponse(responseCode = "403", description = "Access denied. DONOR role is required"),
            @ApiResponse(responseCode = "404", description = "User or donor profile not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("hasRole('DONOR')")
    @PutMapping("/{bookingId}/cancel")
    public ResponseEntity<DonationBooking> cancelDonationBooking(
            @PathVariable Long bookingId,
            Authentication authentication) {

        String email = authentication.getName();
        User user = userService.getUserByEmail(email);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        DonorProfile donorProfile = donorProfileRepository.findByUserId(user.getId());

        if (donorProfile == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        DonationBooking cancelledBooking = donationBookingService.cancelDonationBooking(
                bookingId, donorProfile.getId());

        if (cancelledBooking == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        return ResponseEntity.status(HttpStatus.OK).body(cancelledBooking);
    }
}
