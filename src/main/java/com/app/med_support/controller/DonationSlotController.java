package com.app.med_support.controller;

import com.app.med_support.model.DonationSlot;
import com.app.med_support.model.User;
import com.app.med_support.response.DonationSlotResponse;
import com.app.med_support.service.DonationSlotService;
import com.app.med_support.service.UserService;
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
    @PreAuthorize("hasAnyRole('DONOR', 'HOSPITAL_STAFF')")
    @GetMapping("/{slotId}")
    public ResponseEntity<DonationSlotResponse> getDonationSlotById(
            @PathVariable Long slotId) {

        DonationSlot slot = donationSlotService.getDonationSlotById(slotId);

        if (slot == null) {
            return ResponseEntity.notFound().build();
        }

        DonationSlotResponse response = donationSlotService.toResponse(slot);
        return ResponseEntity.ok(response);
    }
    @PreAuthorize("hasAnyRole('DONOR', 'HOSPITAL_STAFF')")
    @GetMapping
    public ResponseEntity<List<DonationSlotResponse>> getAllDonationSlots() {

        List<DonationSlot> slots = donationSlotService.getAllDonationSlots();

        List<DonationSlotResponse> responses = slots.stream()
                .map(donationSlotService::toResponse).toList();

        return ResponseEntity.ok(responses);
    }
    @PreAuthorize("hasAnyRole('DONOR', 'HOSPITAL_STAFF')")
    @GetMapping("/available/{hospitalId}")
    public ResponseEntity<List<DonationSlotResponse>> getAvailableSlots(
            @PathVariable Long hospitalId) {

        List<DonationSlot> slots = donationSlotService.getAvailableSlotsByHospitalId(hospitalId);

        List<DonationSlotResponse> responses = slots.stream()
                .map(donationSlotService::toResponse).toList();

        return ResponseEntity.ok(responses);
    }

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
    @PreAuthorize("hasRole('HOSPITAL_STAFF')")
    @PutMapping("/{slotId}")
    public ResponseEntity<DonationSlotResponse> updateDonationSlot(
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

    @PreAuthorize("hasRole('HOSPITAL_STAFF')")
    @PatchMapping("/{slotId}/close")
    public ResponseEntity<DonationSlotResponse> closeDonationSlot(
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
