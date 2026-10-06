package com.app.med_support.controller;

import com.app.med_support.model.Hospital;
import com.app.med_support.service.HospitalService;
import com.app.med_support.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin",
        description = "APIs for administrators to manage user accounts and hospitals. " +
                "All endpoints require ADMIN access.")
public class AdminController {

    private final UserService userService;
    private final HospitalService hospitalService;

    public AdminController(UserService userService, HospitalService hospitalService) {

        this.userService = userService;
        this.hospitalService = hospitalService;
    }

    // Admin reactivates a user account
    @Operation(summary = "Reactivate user account",
            description = "Allows an authenticated ADMIN to reactivate an inactive user account.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User reactivated successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication is required or the JWT token is invalid"),
            @ApiResponse(responseCode = "403", description = "Access denied. ADMIN role is required"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/users/{userId}/reactivate")
    public ResponseEntity<String> reactivateUser(@PathVariable Long userId) {

        boolean reactivated = userService.reactivateUser(userId);

        if (!reactivated) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        return ResponseEntity.status(HttpStatus.OK).body("User reactivated successfully");
    }


    // Admin creates a new hospital
    @Operation(summary = "Create hospital",
            description = "Allows an authenticated ADMIN to create a new hospital in the Med Support system.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Hospital created successfully"),
            @ApiResponse(responseCode = "400", description = "Hospital creation failed because the submitted data is invalid"),
            @ApiResponse(responseCode = "401", description = "Authentication is required or the JWT token is invalid"),
            @ApiResponse(responseCode = "403", description = "Access denied. ADMIN role is required"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/hospitals")
    public ResponseEntity<Hospital> createHospital(
            @RequestBody Hospital hospital) {

        Hospital createdHospital = hospitalService.createHospital(hospital);

        if (createdHospital == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(createdHospital);
    }


    // Admin deactivates a hospital
    @Operation(summary = "Deactivate hospital",
            description = "Allows an authenticated ADMIN to deactivate a hospital without permanently removing it from the database.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Hospital deactivated successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication is required or the JWT token is invalid"),
            @ApiResponse(responseCode = "403", description = "Access denied. ADMIN role is required"),
            @ApiResponse(responseCode = "404", description = "Hospital not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/hospitals/{hospitalId}/deactivate")
    public ResponseEntity<String> deactivateHospital(
            @PathVariable Long hospitalId) {

        boolean deactivated = hospitalService.deactivateHospital(hospitalId);

        if (!deactivated) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Hospital not found");
        }

        return ResponseEntity.status(HttpStatus.OK).body("Hospital deactivated successfully");
    }


    // Admin reactivates a hospital
    @Operation(summary = "Reactivate hospital",
            description = "Allows an authenticated ADMIN to reactivate a previously deactivated hospital.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Hospital reactivated successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication is required or the JWT token is invalid"),
            @ApiResponse(responseCode = "403", description = "Access denied. ADMIN role is required"),
            @ApiResponse(responseCode = "404", description = "Hospital not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/hospitals/{hospitalId}/reactivate")
    public ResponseEntity<String> reactivateHospital(
            @PathVariable Long hospitalId) {

        boolean reactivated = hospitalService.reactivateHospital(hospitalId);

        if (!reactivated) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Hospital not found");
        }

        return ResponseEntity.status(HttpStatus.OK).body("Hospital reactivated successfully");
    }


    // Admin views all hospitals
    @Operation(summary = "Get all hospitals",
            description = "Allows an authenticated ADMIN to view all hospitals in the Med Support system.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Hospitals retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication is required or the JWT token is invalid"),
            @ApiResponse(responseCode = "403", description = "Access denied. ADMIN role is required"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/hospitals")
    public ResponseEntity<List<Hospital>> getAllHospitals() {

        List<Hospital> hospitals = hospitalService.getAllHospitals();

        return ResponseEntity.status(HttpStatus.OK).body(hospitals);
    }


    // Admin updates hospital information
    @Operation(summary = "Update hospital",
            description = "Allows an authenticated ADMIN to update the information of an existing hospital.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Hospital updated successfully"),
            @ApiResponse(responseCode = "400", description = "Hospital update failed because the submitted data is invalid"),
            @ApiResponse(responseCode = "401", description = "Authentication is required or the JWT token is invalid"),
            @ApiResponse(responseCode = "403", description = "Access denied. ADMIN role is required"),
            @ApiResponse(responseCode = "404", description = "Hospital not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/hospitals/{hospitalId}")
    public ResponseEntity<Hospital> updateHospital(
            @PathVariable Long hospitalId,
            @RequestBody Hospital hospital) {

        Hospital updatedHospital = hospitalService.updateHospital(hospitalId, hospital);

        if (updatedHospital == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.status(HttpStatus.OK).body(updatedHospital);
    }
}