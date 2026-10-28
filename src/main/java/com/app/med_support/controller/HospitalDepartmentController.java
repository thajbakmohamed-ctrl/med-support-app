
package com.app.med_support.controller;

import com.app.med_support.model.Hospital;
import com.app.med_support.model.HospitalDepartment;
import com.app.med_support.model.User;
import com.app.med_support.response.HospitalDepartmentResponse;
import com.app.med_support.service.HospitalDepartmentService;
import com.app.med_support.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hospital-departments")
public class HospitalDepartmentController {

    private final HospitalDepartmentService departmentService;
    private final UserService userService;

    public HospitalDepartmentController(
            HospitalDepartmentService departmentService,
            UserService userService) {
        this.departmentService = departmentService;
        this.userService = userService;
    }


    @PreAuthorize("hasRole('HOSPITAL_STAFF')")
    @PostMapping
    public ResponseEntity<HospitalDepartmentResponse> createDepartment(
            @RequestBody HospitalDepartment department,
            Authentication authentication) {

        User user = userService.getUserByEmail(authentication.getName());

        if (user == null || user.getHospital() == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Hospital hospital = user.getHospital();
        department.setHospital(hospital);

        HospitalDepartment saved = departmentService.createDepartment(department);

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }


    @PreAuthorize("hasAnyRole('DONOR', 'HOSPITAL_STAFF')")
    @GetMapping("/hospital/{hospitalId}")
    public ResponseEntity<List<HospitalDepartmentResponse>> getDepartments(
            @PathVariable Long hospitalId) {

        List<HospitalDepartmentResponse> responses = departmentService
                .getDepartmentsByHospital(hospitalId)
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(responses);
    }


    private HospitalDepartmentResponse toResponse(
            HospitalDepartment department) {

        return new HospitalDepartmentResponse(department.getId(),
                department.getDepartmentName(), department.getDescription(),
                department.getHospital().getId());
    }
}
