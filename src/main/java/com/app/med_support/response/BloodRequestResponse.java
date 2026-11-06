package com.app.med_support.response;

import java.time.LocalDate;

public class BloodRequestResponse {

    private Long id;
    private Long hospitalId;
    private String requiredBloodType;
    private Integer requiredBloodUnits;
    private LocalDate bloodNeededDate;
    private String bloodRequestUrgency;
    private String bloodRequestStatus;
    private String bloodRequestDescription;

    public BloodRequestResponse(
            Long id,
            Long hospitalId,
            String requiredBloodType,
            Integer requiredBloodUnits,
            LocalDate bloodNeededDate,
            String bloodRequestUrgency,
            String bloodRequestStatus,
            String bloodRequestDescription) {

        this.id = id;
        this.hospitalId = hospitalId;
        this.requiredBloodType = requiredBloodType;
        this.requiredBloodUnits = requiredBloodUnits;
        this.bloodNeededDate = bloodNeededDate;
        this.bloodRequestUrgency = bloodRequestUrgency;
        this.bloodRequestStatus = bloodRequestStatus;
        this.bloodRequestDescription = bloodRequestDescription;
    }

    public Long getId() {
        return id;
    }

    public Long getHospitalId() {
        return hospitalId;
    }

    public String getRequiredBloodType() {
        return requiredBloodType;
    }

    public Integer getRequiredBloodUnits() {
        return requiredBloodUnits;
    }

    public LocalDate getBloodNeededDate() {
        return bloodNeededDate;
    }

    public String getBloodRequestUrgency() {
        return bloodRequestUrgency;
    }

    public String getBloodRequestStatus() {
        return bloodRequestStatus;
    }

    public String getBloodRequestDescription() {
        return bloodRequestDescription;
    }
}
