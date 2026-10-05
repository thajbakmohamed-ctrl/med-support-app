package com.app.med_support.controller;

import com.app.med_support.model.BloodRequest;
import com.app.med_support.service.BloodRequestService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/blood-requests")
public class BloodRequestController {

    private final BloodRequestService bloodRequestService;

    public BloodRequestController(BloodRequestService bloodRequestService) {
        this.bloodRequestService = bloodRequestService;
    }


    @PostMapping
    public BloodRequest createBloodRequest(@RequestBody BloodRequest bloodRequest) {
        return bloodRequestService.createBloodRequest(bloodRequest);
    }


    @GetMapping
    public List<BloodRequest> getAllBloodRequests() {
        return bloodRequestService.getAllBloodRequests();
    }

    @GetMapping("/open")
    public List<BloodRequest> getOpenBloodRequests() {
        return bloodRequestService.getOpenBloodRequests();
    }

    @GetMapping("/open/blood-type/{bloodType}")
    public List<BloodRequest> getOpenBloodRequestsByBloodType(@PathVariable String bloodType) {
        return bloodRequestService.getOpenBloodRequestsByBloodType(bloodType);
    }

    @GetMapping("/{bloodRequestId}")
    public BloodRequest getBloodRequestById(@PathVariable Long bloodRequestId) {
        return bloodRequestService.getBloodRequestById(bloodRequestId);
    }

    @PutMapping("/{bloodRequestId}/status")
    public BloodRequest updateBloodRequestStatus(@PathVariable Long bloodRequestId,
            @RequestParam String newStatus) {
        return bloodRequestService.updateBloodRequestStatus(bloodRequestId, newStatus);
    }


}
