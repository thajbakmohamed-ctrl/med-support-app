
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

    @PostMapping
    public ResponseEntity<DonationBookingResponse> createBooking(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String bearerToken,
            @Valid @RequestBody DonationBookingRequest request) {

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

    @DeleteMapping("/{bookingId}")
    public ResponseEntity<DonationBookingResponse> cancelBooking(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String bearerToken,
            @PathVariable Long bookingId) {

        var donor = identityClient.getMyDonorProfile(bearerToken);


        DonationBookingResponse response = bookingService.cancelBooking(
                bookingId, donor.id());

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{bookingId}/status")
    public ResponseEntity<DonationBookingResponse> updateBookingStatus(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String bearerToken,
            @PathVariable Long bookingId,
            @RequestParam String status) {

        var staff = identityClient.getMyHospitalStaffInfo(bearerToken);


        DonationBookingResponse response = bookingService.updateBookingStatus(
                bookingId, status, staff.hospitalId());

        return ResponseEntity.ok(response);
    }


}
