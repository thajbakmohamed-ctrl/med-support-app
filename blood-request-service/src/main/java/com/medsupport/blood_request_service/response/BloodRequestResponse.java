
package com.medsupport.blood_request_service.response;

import com.medsupport.blood_request_service.model.BloodRequest;

import java.time.LocalDate;

public record BloodRequestResponse(
        Long id,
        Long hospitalId,
        LocalDate neededDate,
        String bloodRequestStatus
) {

    public static BloodRequestResponse from(BloodRequest bloodRequest) {

        return new BloodRequestResponse(
                bloodRequest.getId(),
                bloodRequest.getHospital().getId(),
                bloodRequest.getBloodNeededDate(),
                bloodRequest.getBloodRequestStatus()
        );
    }
}
