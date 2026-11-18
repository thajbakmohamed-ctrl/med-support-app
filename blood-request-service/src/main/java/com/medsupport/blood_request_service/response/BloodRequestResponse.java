
package com.medsupport.blood_request_service.response;

import com.medsupport.blood_request_service.model.BloodRequest;

import java.time.LocalDate;

public record BloodRequestResponse(
        Long id,
        Long hospitalId,
        LocalDate neededDate,
        String bloodRequestStatus,
        String requiredBloodType,
        Integer requiredBloodUnits,
        String bloodRequestUrgency,
        String bloodRequestDescription
) {
    public record Shared(Long id, Long hospitalId, String hospitalName,
            LocalDate neededDate, String bloodRequestStatus, String requiredBloodType,
            Integer requiredBloodUnits, String bloodRequestUrgency) {}

    public BloodRequestResponse(Long id, Long hospitalId, LocalDate neededDate,
            String bloodRequestStatus) {
        this(id, hospitalId, neededDate, bloodRequestStatus, null, null, null, null);
    }

    public static BloodRequestResponse from(BloodRequest bloodRequest) {

        return new BloodRequestResponse(bloodRequest.getId(),
                bloodRequest.getHospital().getId(), bloodRequest.getBloodNeededDate(),
                bloodRequest.getBloodRequestStatus(),
                bloodRequest.getRequiredBloodType(),
                bloodRequest.getRequiredBloodUnits(),
                bloodRequest.getBloodRequestUrgency(),
                bloodRequest.getBloodRequestDescription());
    }
}
