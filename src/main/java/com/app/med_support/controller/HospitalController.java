package com.app.med_support.controller;

import com.app.med_support.model.Hospital;
import com.app.med_support.service.HospitalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hospitals")
@Tag(name = "Hospitals",
description = "APIs for viewing hospitals available in the Med Support system.")
public class HospitalController {

    private final HospitalService hospitalService;

    public HospitalController(HospitalService hospitalService) {
        this.hospitalService = hospitalService;
    }


    // View all hospitals
    @Operation(summary = "Get all hospitals",
            description = "Retrieves all hospitals available in the Med Support system.")
    @ApiResponses(value = {
     @ApiResponse(responseCode = "200", description = "Hospitals retrieved successfully"),
     @ApiResponse(responseCode = "401", description = "Authentication is required or the JWT token is invalid"),
     @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    public ResponseEntity<List<Hospital>> getAllHospitals() {

        List<Hospital> hospitals = hospitalService.getAllHospitals();

        return ResponseEntity.status(HttpStatus.OK).body(hospitals);
    }


    // View selected hospital by ID
    @Operation(summary = "Get hospital by ID",
            description = "Retrieves the details of a specific hospital using its hospital ID.")
    @ApiResponses(value = {
     @ApiResponse(responseCode = "200", description = "Hospital retrieved successfully"),
     @ApiResponse(responseCode = "401", description = "Authentication is required or the JWT token is invalid"),
      @ApiResponse(responseCode = "404", description = "Hospital not found"),
      @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{hospitalId}")
    public ResponseEntity<Hospital> getHospitalById(@PathVariable Long hospitalId) {

        Hospital hospital = hospitalService.getHospitalById(hospitalId);

        if (hospital == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.status(HttpStatus.OK).body(hospital);
    }
}
