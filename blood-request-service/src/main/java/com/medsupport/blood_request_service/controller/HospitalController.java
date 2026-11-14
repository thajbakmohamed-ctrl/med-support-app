
package com.medsupport.blood_request_service.controller;

import com.medsupport.blood_request_service.client.IdentityClient;
import com.medsupport.blood_request_service.request.HospitalRequest;
import com.medsupport.blood_request_service.response.HospitalResponse;
import com.medsupport.blood_request_service.service.HospitalService;

import jakarta.validation.Valid;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hospitals")
public class HospitalController {

    private final HospitalService hospitalService;
    private final IdentityClient identityClient;

    public HospitalController(
            HospitalService hospitalService,
            IdentityClient identityClient) {
        this.hospitalService = hospitalService;
        this.identityClient = identityClient;
    }


    @PostMapping
    public ResponseEntity<HospitalResponse> createHospital(
            @Valid @RequestBody HospitalRequest request,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION,
                    required = false) String authorizationHeader) {

        identityClient.verifyAdmin(authorizationHeader);

        HospitalResponse response = hospitalService.createHospital(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @PutMapping("/{hospitalId}")
    public ResponseEntity<HospitalResponse> updateHospital(
            @PathVariable Long hospitalId,
            @Valid @RequestBody HospitalRequest request,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION,
                    required = false) String authorizationHeader) {

        identityClient.verifyAdmin(authorizationHeader);
        HospitalResponse response = hospitalService.updateHospital(hospitalId, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{hospitalId}/activate")
    public ResponseEntity<HospitalResponse> activateHospital(
            @PathVariable Long hospitalId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION,
                    required = false) String authorizationHeader) {

        identityClient.verifyAdmin(authorizationHeader);
        HospitalResponse response = hospitalService.activateHospital(hospitalId);
        return ResponseEntity.ok(response);
    }


    @PatchMapping("/{hospitalId}/deactivate")
    public ResponseEntity<HospitalResponse> deactivateHospital(
            @PathVariable Long hospitalId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION,
                    required = false) String authorizationHeader) {
        identityClient.verifyAdmin(authorizationHeader);
        HospitalResponse response = hospitalService.deactivateHospital(hospitalId);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/{hospitalId}")
    public ResponseEntity<HospitalResponse> getHospitalById(
            @PathVariable Long hospitalId) {
        return ResponseEntity.ok(hospitalService.getHospitalById(hospitalId));
    }


    @GetMapping
    public ResponseEntity<List<HospitalResponse>> getAllHospitals() {
        return ResponseEntity.ok(hospitalService.getAllHospitals());
    }
}
