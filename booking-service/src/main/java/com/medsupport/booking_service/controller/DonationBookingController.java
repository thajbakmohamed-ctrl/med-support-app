
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

@io.swagger.v3.oas.annotations.security.SecurityRequirement(name="bearerAuth")
@RestController
@RequestMapping("/api/donation-bookings")
public class DonationBookingController {
    @PutMapping("/{bookingId}/reschedule")
    public DonationBookingResponse reschedule(
            @RequestHeader(value=HttpHeaders.AUTHORIZATION, required=false) String bearerToken,
            @PathVariable Long bookingId, @Valid @RequestBody DonationBookingRequest request) {
        requireBearerToken(bearerToken);
        var donor = identityClient.getMyDonorProfile(bearerToken);
        var bloodRequest = bloodRequestClient.getBloodRequest(request.getBloodRequestId(), bearerToken);
        return bookingService.rescheduleBooking(bookingId, request, donor.id(), bloodRequest.hospitalId(),
                bloodRequest.neededDate(), identityClient.getAuthenticatedEmail(bearerToken));
    }
    @GetMapping("/my-hospital")
    public List<DonationBookingResponse> hospitalBookings(
            @RequestHeader(value=HttpHeaders.AUTHORIZATION, required=false) String bearerToken) {
        return bookingService.getHospitalBookings(identityClient.getMyHospitalStaffInfo(bearerToken).hospitalId());
    }
    @GetMapping("/hospital-summary")
    public java.util.Map<String, Long> hospitalSummary(
            @RequestHeader(value=HttpHeaders.AUTHORIZATION, required=false) String bearerToken) {
        Long hospitalId = identityClient.getMyHospitalStaffInfo(bearerToken).hospitalId();
        return bookingService.getHospitalSummary(hospitalId);
    }
    private final DonationBookingService bookingService;
    private final IdentityClient identityClient;
    private final BloodRequestClient bloodRequestClient;

    public DonationBookingController(DonationBookingService bookingService,
            IdentityClient identityClient, BloodRequestClient bloodRequestClient) {

        this.bookingService = bookingService;
        this.identityClient = identityClient;
        this.bloodRequestClient = bloodRequestClient;
    }
    private String requireBearerToken(String bearerToken) {

        if (bearerToken == null
                || !bearerToken.startsWith("Bearer ")
                || bearerToken.substring(7).isBlank()) {

            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Authentication token is required");
        }

        return bearerToken;
    }

    @PostMapping
    public ResponseEntity<DonationBookingResponse> createBooking(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION,
                    required = false) String bearerToken,
            @Valid @RequestBody DonationBookingRequest request) {

        requireBearerToken(bearerToken);

        var donor = identityClient.getMyDonorProfile(bearerToken);

        var bloodRequest = bloodRequestClient.getBloodRequest(request.getBloodRequestId(), bearerToken);
        String performedByEmail = identityClient.getAuthenticatedEmail(bearerToken);
        DonationBookingResponse response = bookingService.createBooking(
                request, donor.id(), bloodRequest.hospitalId(),
                bloodRequest.neededDate(), bloodRequest.bloodRequestStatus(), performedByEmail);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{bookingId}")
    public ResponseEntity<DonationBookingResponse> cancelBooking(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION,
                    required = false) String bearerToken,
            @PathVariable Long bookingId) {

        requireBearerToken(bearerToken);

        var donor = identityClient.getMyDonorProfile(bearerToken);

        String performedByEmail = identityClient.getAuthenticatedEmail(bearerToken);

        DonationBookingResponse response = bookingService.cancelBooking(
                bookingId, donor.id(), performedByEmail);

        return ResponseEntity.ok(response);
    }
    @PutMapping("/{bookingId}/status")
    public ResponseEntity<DonationBookingResponse> updateBookingStatus(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION,
                    required = false) String bearerToken,
            @PathVariable Long bookingId,
            @RequestParam String status) {

        requireBearerToken(bearerToken);
        var staff = identityClient.getMyHospitalStaffInfo(bearerToken);

        String performedByEmail = identityClient.getAuthenticatedEmail(bearerToken);
        Long donorProfileId = bookingService.getDonorProfileIdForStaffNotification(
                bookingId, staff.hospitalId());
        String donorEmail = identityClient.getDonorEmail(donorProfileId);
        DonationBookingResponse response = bookingService.updateBookingStatus(
                bookingId, status, staff.hospitalId(), performedByEmail,
                donorProfileId, donorEmail);

        return ResponseEntity.ok(response);
    }
    @GetMapping("/my")
    public ResponseEntity<List<DonationBookingResponse>> getMyBookings(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION,
                    required = false) String bearerToken) {

        requireBearerToken(bearerToken);

        var donor = identityClient.getMyDonorProfile(bearerToken);

        return ResponseEntity.ok(bookingService.getBookingsByDonor(donor.id()));
    }
    @GetMapping("/blood-request/{bloodRequestId}")
    public ResponseEntity<List<DonationBookingResponse>> getBookingsByBloodRequest(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION,
                    required = false) String bearerToken,
            @PathVariable Long bloodRequestId) {

        requireBearerToken(bearerToken);

        var staff = identityClient.getMyHospitalStaffInfo(bearerToken);

        var bloodRequest = bloodRequestClient.getBloodRequestForStaff(bloodRequestId, bearerToken);

        if (!staff.hospitalId().equals(bloodRequest.hospitalId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You cannot view another hospital's bookings");
        }

        return ResponseEntity.ok(bookingService.getBookingsByBloodRequest(bloodRequestId));
    }
}
