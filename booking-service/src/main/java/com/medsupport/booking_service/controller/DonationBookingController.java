
package com.medsupport.booking_service.controller;

import com.medsupport.booking_service.client.IdentityClient;
import com.medsupport.booking_service.client.BloodRequestClient;
import com.medsupport.booking_service.request.DonationBookingRequest;
import com.medsupport.booking_service.response.DonationBookingResponse;
import com.medsupport.booking_service.service.DonationBookingService;

import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/donation-bookings")
public class DonationBookingController {

    private final DonationBookingService bookingService;
    private final IdentityClient identityClient;
    private final BloodRequestClient bloodRequestClient;

    public DonationBookingController(
            DonationBookingService bookingService,
            IdentityClient identityClient,
            BloodRequestClient bloodRequestClient) {

        this.bookingService = bookingService;
        this.identityClient = identityClient;
        this.bloodRequestClient = bloodRequestClient;
    }

    // Check JWT before calling other microservices
    private String requireBearerToken(String bearerToken) {

        if (bearerToken == null
                || !bearerToken.startsWith("Bearer ")
                || bearerToken.substring(7).isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication token is required"
            );
        }

        return bearerToken;
    }

    // Create donation booking
    @PostMapping
    public ResponseEntity<DonationBookingResponse> createBooking(
            @RequestHeader(
                    value = HttpHeaders.AUTHORIZATION,
                    required = false
            ) String bearerToken,
            @Valid @RequestBody DonationBookingRequest request) {

        requireBearerToken(bearerToken);

        var donor = identityClient.getMyDonorProfile(bearerToken);

        var bloodRequest = bloodRequestClient.getBloodRequest(
                request.getBloodRequestId(),
                bearerToken
        );

        DonationBookingResponse response = bookingService.createBooking(
                request,
                donor.id(),
                bloodRequest.hospitalId(),
                bloodRequest.neededDate(),
                bloodRequest.bloodRequestStatus()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // Cancel donor's own booking
    @DeleteMapping("/{bookingId}")
    public ResponseEntity<DonationBookingResponse> cancelBooking(
            @RequestHeader(
                    value = HttpHeaders.AUTHORIZATION,
                    required = false
            ) String bearerToken,
            @PathVariable Long bookingId) {

        requireBearerToken(bearerToken);

        var donor = identityClient.getMyDonorProfile(bearerToken);

        DonationBookingResponse response = bookingService.cancelBooking(
                bookingId,
                donor.id()
        );

        return ResponseEntity.ok(response);
    }

    // Update booking status by hospital staff
    @PutMapping("/{bookingId}/status")
    public ResponseEntity<DonationBookingResponse> updateBookingStatus(
            @RequestHeader(
                    value = HttpHeaders.AUTHORIZATION,
                    required = false
            ) String bearerToken,
            @PathVariable Long bookingId,
            @RequestParam String status) {

        requireBearerToken(bearerToken);

        var staff = identityClient.getMyHospitalStaffInfo(bearerToken);

        DonationBookingResponse response = bookingService.updateBookingStatus(
                bookingId,
                status,
                staff.hospitalId()
        );

        return ResponseEntity.ok(response);
    }

    // View authenticated donor's bookings
    @GetMapping("/my")
    public ResponseEntity<List<DonationBookingResponse>> getMyBookings(
            @RequestHeader(
                    value = HttpHeaders.AUTHORIZATION,
                    required = false
            ) String bearerToken) {

        requireBearerToken(bearerToken);

        var donor = identityClient.getMyDonorProfile(bearerToken);

        return ResponseEntity.ok(
                bookingService.getBookingsByDonor(donor.id())
        );
    }

    // View bookings for a blood request (hospital staff only)
    @GetMapping("/blood-request/{bloodRequestId}")
    public ResponseEntity<List<DonationBookingResponse>> getBookingsByBloodRequest(
            @RequestHeader(
                    value = HttpHeaders.AUTHORIZATION,
                    required = false
            ) String bearerToken,
            @PathVariable Long bloodRequestId) {

        requireBearerToken(bearerToken);

        var staff = identityClient.getMyHospitalStaffInfo(bearerToken);

        var bloodRequest = bloodRequestClient.getBloodRequest(
                bloodRequestId,
                bearerToken
        );

        if (!staff.hospitalId().equals(bloodRequest.hospitalId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You cannot view another hospital's bookings"
            );
        }

        return ResponseEntity.ok(
                bookingService.getBookingsByBloodRequest(bloodRequestId)
        );
    }
}
