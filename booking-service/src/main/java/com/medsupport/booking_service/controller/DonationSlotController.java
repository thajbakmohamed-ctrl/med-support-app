
package com.medsupport.booking_service.controller;

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

import java.util.List;

@RestController
@RequestMapping("/api/donation-slots")
public class DonationSlotController {

    private final DonationSlotService slotService;
    private final IdentityClient identityClient;

    public DonationSlotController(
            DonationSlotService slotService,
            IdentityClient identityClient) {
        this.slotService = slotService;
        this.identityClient = identityClient;
    }

    @PostMapping
    public ResponseEntity<DonationSlotResponse> createSlot(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String bearerToken,
            @Valid @RequestBody DonationSlotRequest request) {

        Long hospitalId = identityClient
                .getMyHospitalStaffInfo(bearerToken)
                .hospitalId();

        DonationSlot slot = slotService.createSlot(
                hospitalId,
                request.slotDate(),
                request.startTime(),
                request.endTime(),
                request.capacity()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(DonationSlotResponse.from(slot));
    }

    @PutMapping("/{slotId}/capacity")
    public ResponseEntity<DonationSlotResponse> updateCapacity(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String bearerToken,
            @PathVariable Long slotId,
            @RequestParam @Min(1) Integer capacity) {

        Long hospitalId = identityClient
                .getMyHospitalStaffInfo(bearerToken)
                .hospitalId();

        DonationSlot slot = slotService.updateCapacity(
                slotId, capacity, hospitalId
        );

        return ResponseEntity.ok(DonationSlotResponse.from(slot));
    }

    @GetMapping("/my-hospital")
    public ResponseEntity<List<DonationSlotResponse>> getMyHospitalSlots(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String bearerToken) {

        Long hospitalId = identityClient
                .getMyHospitalStaffInfo(bearerToken)
                .hospitalId();

        List<DonationSlotResponse> response = slotService
                .getSlotsByHospital(hospitalId)
                .stream()
                .map(DonationSlotResponse::from)
                .toList();

        return ResponseEntity.ok(response);
    }
}
