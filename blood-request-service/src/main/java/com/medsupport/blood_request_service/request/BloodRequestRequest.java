
package com.medsupport.blood_request_service.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public class BloodRequestRequest {

    @NotNull(message = "Hospital ID is required")
    private Long hospitalId;

    @NotBlank(message = "Blood type is required")
    @Pattern(regexp = "^(A|B|AB|O)[+-]$",
            message = "Blood type must be A+, A-, B+, B-, AB+, AB-, O+, or O-")
    private String requiredBloodType;

    @NotNull(message = "Required blood units are required")
    @Min(value = 1, message = "Required blood units must be at least 1")
    private Integer requiredBloodUnits;

    @NotBlank(message = "Urgency is required")
    @Pattern(regexp = "^(LOW|MEDIUM|HIGH|CRITICAL)$",
            message = "Urgency must be LOW, MEDIUM, HIGH, or CRITICAL")
    private String bloodRequestUrgency;

    @NotNull(message = "Blood needed date is required")
    @FutureOrPresent(message = "Blood needed date cannot be in the past")
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
