
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


    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name="bearerAuth")
    @PostMapping
    public ResponseEntity<HospitalResponse> createHospital(
            @Valid @RequestBody HospitalRequest request,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION,
                    required = false) String authorizationHeader) {

        identityClient.verifyAdmin(authorizationHeader);

        HospitalResponse response = hospitalService.createHospital(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name="bearerAuth")
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

    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name="bearerAuth")
    @PatchMapping("/{hospitalId}/activate")
    public ResponseEntity<HospitalResponse> activateHospital(
            @PathVariable Long hospitalId,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION,
                    required = false) String authorizationHeader) {

        identityClient.verifyAdmin(authorizationHeader);
        HospitalResponse response = hospitalService.activateHospital(hospitalId);
        return ResponseEntity.ok(response);
    }


    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name="bearerAuth")
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

    public record DepartmentRequest(
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=100) String name,
            @jakarta.validation.constraints.Size(max=500) String description) {}

    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name="bearerAuth")
    @GetMapping("/{hospitalId}/departments")
    public List<HospitalService.Department> departments(@PathVariable Long hospitalId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String bearer) {
        identityClient.verifyUser(bearer);
        return hospitalService.getDepartments(hospitalId);
    }

    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name="bearerAuth")
    @PostMapping("/{hospitalId}/departments")
    public ResponseEntity<HospitalService.Department> createDepartment(@PathVariable Long hospitalId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String bearer, @Valid @RequestBody DepartmentRequest request) {
        Long assigned = identityClient.getVerifiedHospitalId(bearer);
        if (!hospitalId.equals(assigned)) throw new org.springframework.web.server.ResponseStatusException(
                HttpStatus.FORBIDDEN, "You cannot manage another hospital's departments");
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    hospitalService.createDepartment(assigned, request.name(), request.description()));
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.CONFLICT,
                    "A department with this name already exists");
        }
    }
}
