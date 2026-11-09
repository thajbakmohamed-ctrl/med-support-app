
package com.medsupport.blood_request_service.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.FutureOrPresent;

import java.time.LocalDate;

public class BloodRequestRequest {

    @NotNull
    private Long hospitalId;

    @NotBlank
    private String requiredBloodType;

    @NotNull
    @Min(1)
    private Integer requiredBloodUnits;

    @NotBlank
    private String bloodRequestUrgency;

    @NotNull
    @FutureOrPresent
    private LocalDate bloodNeededDate;

    private String bloodRequestDescription;

    public Long getHospitalId() {
        return hospitalId;
    }

    public void setHospitalId(Long hospitalId) {
        this.hospitalId = hospitalId;
    }

    public String getRequiredBloodType() {
        return requiredBloodType;
    }

    public void setRequiredBloodType(String requiredBloodType) {
        this.requiredBloodType = requiredBloodType;
    }

    public Integer getRequiredBloodUnits() {
        return requiredBloodUnits;
    }

    public void setRequiredBloodUnits(Integer requiredBloodUnits) {
        this.requiredBloodUnits = requiredBloodUnits;
    }

    public String getBloodRequestUrgency() {
        return bloodRequestUrgency;
    }

    public void setBloodRequestUrgency(String bloodRequestUrgency) {
        this.bloodRequestUrgency = bloodRequestUrgency;
    }

    public LocalDate getBloodNeededDate() {
        return bloodNeededDate;
    }

    public void setBloodNeededDate(LocalDate bloodNeededDate) {
        this.bloodNeededDate = bloodNeededDate;
    }

    public String getBloodRequestDescription() {
        return bloodRequestDescription;
    }

    public void setBloodRequestDescription(String bloodRequestDescription) {
        this.bloodRequestDescription = bloodRequestDescription;
    }
}
