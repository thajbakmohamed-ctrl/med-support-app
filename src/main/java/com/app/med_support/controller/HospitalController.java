package com.app.med_support.controller;

import com.app.med_support.service.HospitalService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hospitals")
public class HospitalController {

    private final HospitalService hospitalService;

    public HospitalController(HospitalService hospitalService) {
        this.hospitalService = hospitalService;
    }
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{hospitalId}/deactivate")
    public String deactivateHospital(@PathVariable Long hospitalId) {
        boolean deactivated = hospitalService.deactivateHospital(hospitalId);
        if (!deactivated) {
            return "Hospital not found";
        }
        return "Hospital deactivated successfully";
    }
    @PutMapping("/{hospitalId}/reactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public String reactivateHospital(@PathVariable Long hospitalId) {
        boolean reactivated = hospitalService.reactivateHospital(hospitalId);
        if (!reactivated) {
            return "Hospital not found";
        }
        return "Hospital reactivated successfully";
    }
}
