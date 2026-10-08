package com.app.med_support.service;

import com.app.med_support.model.Hospital;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminService {
    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);

    private final UserService userService;
    private final HospitalService hospitalService;
    private final AuditLogService auditLogService;

    public AdminService(UserService userService, HospitalService hospitalService,
            AuditLogService auditLogService) {

        this.userService = userService;
        this.hospitalService = hospitalService;
        this.auditLogService = auditLogService;
    }

    // Admin reactivates a user account
    public boolean reactivateUser(Long userId, String adminEmail) {
        boolean reactivated = userService.reactivateUser(userId);

        if (!reactivated) {
            return false;
        }

        auditLogService.saveAuditLog("USER_REACTIVATED",
                adminEmail, "User account with ID " + userId + " was reactivated");
        logger.info("Admin {} reactivated user account with ID: {}",
                adminEmail, userId);
        return true;
    }

    // Admin creates a new hospital
    public Hospital createHospital(Hospital hospital, String adminEmail) {

        Hospital createdHospital = hospitalService.createHospital(hospital);

        if (createdHospital == null) {
            return null;
        }

        auditLogService.saveAuditLog("HOSPITAL_CREATED",
                adminEmail, "Hospital with ID " + createdHospital.getId() + " was created");

        return createdHospital;
    }

    // Admin deactivates a hospital
    public boolean deactivateHospital(Long hospitalId, String adminEmail) {

        boolean deactivated = hospitalService.deactivateHospital(hospitalId);

        if (!deactivated) {
            return false;
        }

        auditLogService.saveAuditLog("HOSPITAL_DEACTIVATED",
                adminEmail, "Hospital with ID " + hospitalId + " was deactivated");

        return true;
    }

    // Admin reactivates a hospital
    public boolean reactivateHospital(Long hospitalId, String adminEmail) {

        boolean reactivated = hospitalService.reactivateHospital(hospitalId);

        if (!reactivated) {
            return false;
        }

        auditLogService.saveAuditLog("HOSPITAL_REACTIVATED",
                adminEmail, "Hospital with ID " + hospitalId + " was reactivated");

        return true;
    }
    // Admin deactivates a user account
    public boolean deactivateUser(Long userId, String adminEmail) {

        boolean deactivated = userService.deleteUser(userId);

        if (!deactivated) {
            return false;
        }

        auditLogService.saveAuditLog("USER_DEACTIVATED",
                adminEmail, "User account with ID " + userId + " was deactivated");
        return true;
    }

    // Admin views all hospitals
    public List<Hospital> getAllHospitals() {
        return hospitalService.getAllHospitals();
    }

    // Admin updates hospital information
    public Hospital updateHospital(Long hospitalId, Hospital hospital, String adminEmail) {

        Hospital updatedHospital = hospitalService.updateHospital(hospitalId, hospital);

        if (updatedHospital == null) {
            return null;
        }

        auditLogService.saveAuditLog("HOSPITAL_UPDATED",
                adminEmail, "Hospital with ID " + hospitalId + " was updated");

        return updatedHospital;
    }
}
