package com.app.med_support.controller;

import com.app.med_support.model.BloodRequest;
import com.app.med_support.model.DonationBooking;
import com.app.med_support.model.DonorProfile;
import com.app.med_support.model.User;
import com.app.med_support.repository.DonorProfileRepository;
import com.app.med_support.response.DonationBookingResponse;
import com.app.med_support.service.DonationBookingService;
import com.app.med_support.service.UserService;
import com.app.med_support.response.DonorApplicationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.app.med_support.request.DonationBookingRequest;
import com.app.med_support.model.DonationSlot;

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

    public DonationBookingController(DonationBookingService donationBookingService,
            UserService userService, DonorProfileRepository donorProfileRepository) {

        this.donationBookingService = donationBookingService;
        this.userService = userService;
        this.donorProfileRepository = donorProfileRepository;
    }

    private DonationBookingResponse toResponse(DonationBooking booking) {

        Long slotId = null;


        if (booking.getDonationSlot() != null) {
            slotId = booking.getDonationSlot().getId();
        }

        return new DonationBookingResponse(booking.getId(),
                booking.getBloodRequest().getId(), slotId,
                booking.getDonationBookingDate(),
                booking.getDonationBookingTime(),
                booking.getDonationBookingStatus());
    }
    private DonorApplicationResponse toDonorApplicationResponse(
            DonationBooking booking) {

        DonorProfile donor = booking.getDonorProfile();
        User user = donor.getUser();

        DonationSlot slot = booking.getDonationSlot();

        Long slotId = null;

        if (booking.getDonationSlot() != null) {
            slotId = booking.getDonationSlot().getId();
        }

        return new DonorApplicationResponse(
                booking.getId(),
                booking.getBloodRequest().getId(),
                slot != null ? slot.getId() : null,
                booking.getDonationBookingDate(),
                booking.getDonationBookingTime(),
                booking.getDonationBookingStatus(),


                user.getName(),
                user.getEmail(),
                user.getPhoneNumber(),
                donor.getBloodType(),


                slot != null ? slot.getSlotDate() : null,
                slot != null ? slot.getStartTime() : null,
                slot != null ? slot.getEndTime() : null,
                slot != null ? slot.getStatus() : null,
                slot != null ? slot.getCapacity() : null,
                slot != null ? slot.getReservedCount() : null,


                booking.getNotesAboutTheDonationBooking(),
                booking.getCreatedAtDonationBooking(),
                booking.getUpdatedAtDonationBooking());
    }



    // Create a donation booking
    @Operation(
            summary = "Create donation booking",
            description = "Allows an authenticated DONOR to book an available donation slot. " +
                    "The slot must be OPEN and have remaining capacity. " +
                    "The booking date must match the slot date and must not exceed the blood request's needed date. " +
                    "The booking time must be within the slot's allowed time range. " +
                    "The slot and blood request must belong to the same hospital. " +
                    "Duplicate active bookings for the same donor, date, and time are not allowed. " +
                    "Pessimistic locking protects the slot capacity from concurrent overbooking."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Donation booking created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid booking date or time, closed donation slot, " +
                            "hospital mismatch, or blood request is not open"),
            @ApiResponse(responseCode = "401", description = "Authentication is required or JWT token is invalid"),
            @ApiResponse(responseCode = "403", description = "Access denied. DONOR role is required"),
            @ApiResponse(responseCode = "404", description = "User, donor profile, blood request, or donation slot not found"),
            @ApiResponse(responseCode = "409", description = "Duplicate active booking or donation slot is full"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("hasRole('DONOR')")
    @PostMapping
    public ResponseEntity<DonationBookingResponse> createDonationBooking(
            @Valid @RequestBody DonationBookingRequest request,
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
        DonationBooking donationBooking = new DonationBooking();

        donationBooking.setDonorProfile(donorProfile);

        BloodRequest bloodRequest = new BloodRequest();
        bloodRequest.setId(request.getBloodRequestId());
        donationBooking.setBloodRequest(bloodRequest);

        DonationSlot donationSlot = new DonationSlot();
        donationSlot.setId(request.getDonationSlotId());
        donationBooking.setDonationSlot(donationSlot);

        donationBooking.setDonationBookingDate(request.getDonationBookingDate());
        donationBooking.setDonationBookingTime(request.getDonationBookingTime());
        donationBooking.setNotesAboutTheDonationBooking(
                request.getNotesAboutTheDonationBooking());


        DonationBooking createdBooking = donationBookingService.createDonationBooking(donationBooking);


        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(createdBooking));

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
    public ResponseEntity<List<DonationBookingResponse>> getBookingsByDonor(
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


        List<DonationBookingResponse> responses = bookings.stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(responses);
    }


    @Operation(summary = "Get bookings by blood request",
            description = "Allows authenticated HOSPITAL_STAFF to view donation bookings " +
                    "for a blood request belonging to their own hospital. " +
                    "Returns an empty list if the blood request does not exist, " +
                    "does not belong to the staff member's hospital, or has no bookings.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Donation bookings retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication is required or the JWT token is invalid"),
            @ApiResponse(responseCode = "403", description = "Access denied. HOSPITAL_STAFF role is required"),
            @ApiResponse(responseCode = "404",
                    description = "Authenticated staff user or hospital association not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("hasRole('HOSPITAL_STAFF')")
    @GetMapping("/blood-request/{bloodRequestId}")
    public ResponseEntity<List<DonorApplicationResponse>> getBookingsByBloodRequest(
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

        List<DonorApplicationResponse> responses = bookings.stream()
                .map(this::toDonorApplicationResponse)
                .toList();

        return ResponseEntity.ok(responses);
    }


    @Operation(summary = "Update donation booking status",
            description = "Allows authenticated HOSPITAL_STAFF to update the status " +
                    "of a donation booking belonging to their hospital. " +
                    "Allowed transitions: PENDING to CONFIRMED or CANCELLED, " +
                    "and CONFIRMED to COMPLETED or CANCELLED. " +
                    "COMPLETED and CANCELLED bookings cannot be changed.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Donation booking status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Missing or invalid booking status"),
            @ApiResponse(responseCode = "401", description = "Authentication is required or JWT token is invalid"),
            @ApiResponse(responseCode = "403", description = "Access denied or booking belongs to another hospital"),
            @ApiResponse(responseCode = "404", description = "Booking, staff user, or hospital association not found"),
            @ApiResponse(responseCode = "409", description = "Invalid status transition or booking is already completed or cancelled"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("hasRole('HOSPITAL_STAFF')")
    @PutMapping("/{bookingId}/status")
    public ResponseEntity<DonationBookingResponse> updateDonationBookingStatus(

            @Parameter(description = "ID of the donation booking whose status will be updated. " +
                            "The booking must belong to the authenticated staff member's hospital.",
                    example = "18", required = true)
            @PathVariable Long bookingId,
            @Parameter(description = "New booking status. Allowed transitions: " +
                            "PENDING to CONFIRMED or CANCELLED; " +
                            "CONFIRMED to COMPLETED or CANCELLED. " +
                            "COMPLETED and CANCELLED are final states.",
                    example = "CONFIRMED", required = true)
            @RequestParam String newStatus,
            Authentication authentication) {

        String email = authentication.getName();
        User user = userService.getUserByEmail(email);

        if (user == null || user.getHospital() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Long hospitalId = user.getHospital().getId();
        DonationBooking updatedBooking =
                donationBookingService.updateDonationBookingStatus(
                        bookingId, newStatus, hospitalId, email);



        return ResponseEntity.ok(toResponse(updatedBooking));
    }


    @Operation(summary = "Cancel donation booking",
            description = "Allows an authenticated DONOR to cancel their own donation booking. " +
                    "Only PENDING or CONFIRMED bookings can be cancelled. " +
                    "COMPLETED and CANCELLED bookings cannot be cancelled again.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Donation booking cancelled successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication is required or JWT token is invalid"),
            @ApiResponse(responseCode = "403", description = "Access denied or booking belongs to another donor"),
            @ApiResponse(responseCode = "404", description = "Booking, user, or donor profile not found"),
            @ApiResponse(responseCode = "409", description = "Booking is already completed or cancelled"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PreAuthorize("hasRole('DONOR')")
    @PutMapping("/{bookingId}/cancel")
    public ResponseEntity<DonationBookingResponse> cancelDonationBooking(

            @Parameter(description = "ID of the donation booking to cancel. " +
                            "The booking must belong to the authenticated donor.",
                    example = "18", required = true)
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


        return ResponseEntity.ok(toResponse(cancelledBooking));
    }

}
