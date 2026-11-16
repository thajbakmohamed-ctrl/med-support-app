
package com.medsupport.booking_service.controller;

import com.medsupport.booking_service.client.BloodRequestClient;
import com.medsupport.booking_service.client.IdentityClient;
import com.medsupport.booking_service.model.DonationSlot;
import com.medsupport.booking_service.request.DonationSlotRequest;
import com.medsupport.booking_service.response.DonationSlotResponse;
import com.medsupport.booking_service.service.DonationSlotService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@io.swagger.v3.oas.annotations.security.SecurityRequirement(name="bearerAuth")
@RestController
@RequestMapping("/api/donation-slots")
public class DonationSlotController {

    @GetMapping("/my-hospital/lifecycle")
    public List<DonationSlotService.SlotLifecycle> hospitalLifecycle(
            @RequestHeader(value=HttpHeaders.AUTHORIZATION, required=false) String bearerToken) {
        Long hospitalId = identityClient.getMyHospitalStaffInfo(bearerToken).hospitalId();
        return slotService.getLifecycle(hospitalId, null);
    }

    @GetMapping("/my-history")
    public List<DonationSlotService.SlotLifecycle> donorLifecycle(
            @RequestHeader(value=HttpHeaders.AUTHORIZATION, required=false) String bearerToken) {
        var donor = identityClient.getMyDonorProfile(bearerToken);
        if (donor == null || donor.id() == null || donor.id() <= 0)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "A valid donor profile is required");
        return slotService.getLifecycle(null, donor.id());
    }

    private final DonationSlotService slotService;
    private final IdentityClient identityClient;
    private final BloodRequestClient bloodRequestClient;

    public DonationSlotController(DonationSlotService slotService,
            IdentityClient identityClient, BloodRequestClient bloodRequestClient) {

        this.slotService = slotService;
        this.identityClient = identityClient;
        this.bloodRequestClient = bloodRequestClient;
    }

    @PostMapping
    public ResponseEntity<DonationSlotResponse> createSlot(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String bearerToken,
            @Valid @RequestBody DonationSlotRequest request) {

        Long hospitalId = identityClient.getMyHospitalStaffInfo(bearerToken).hospitalId();

        DonationSlot slot = slotService.createSlot(hospitalId, request.slotDate(),
                request.startTime(), request.endTime(), request.capacity());

        return ResponseEntity.status(HttpStatus.CREATED).body(DonationSlotResponse.from(slot));
    }

    @PutMapping("/{slotId}/capacity")
    public ResponseEntity<DonationSlotResponse> updateCapacity(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String bearerToken,
            @PathVariable Long slotId,
            @RequestParam @Min(1) Integer capacity) {

        Long hospitalId = identityClient.getMyHospitalStaffInfo(bearerToken)
                .hospitalId();

        DonationSlot slot = slotService.updateCapacity(slotId, capacity, hospitalId);

        return ResponseEntity.ok(DonationSlotResponse.from(slot));
    }
    @GetMapping("/my-hospital")
    public ResponseEntity<List<DonationSlotResponse>> getMyHospitalSlots(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String bearerToken) {

        Long hospitalId = identityClient.getMyHospitalStaffInfo(bearerToken)
                .hospitalId();

        List<DonationSlotResponse> response = slotService
                .getSlotsByHospital(hospitalId)
                .stream()
                .map(DonationSlotResponse::from)
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/available")
    public ResponseEntity<List<DonationSlotResponse>> getAvailableSlots(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String bearerToken,
            @RequestParam @Min(1) Long bloodRequestId) {
        IdentityClient.DonorProfileInfo donor = identityClient.getMyDonorProfile(bearerToken);

        if (donor == null || donor.id() == null || donor.id() <= 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "A valid donor profile is required");
        }
        BloodRequestClient.BloodRequestInfo bloodRequest = bloodRequestClient.getBloodRequest(
                bloodRequestId, bearerToken);

        if (!"OPEN".equals(bloodRequest.bloodRequestStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only OPEN blood requests have available slots");
        }

        List<DonationSlotResponse> response = slotService
                .getAvailableSlotsForBloodRequest(bloodRequest.hospitalId(),
                        bloodRequest.neededDate()
                )
                .stream()
                .map(DonationSlotResponse::from)
                .toList();

        return ResponseEntity.ok(response);
    }
}
