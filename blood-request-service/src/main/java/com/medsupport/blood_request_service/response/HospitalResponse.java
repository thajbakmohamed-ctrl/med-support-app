
package com.medsupport.blood_request_service.response;

import com.medsupport.blood_request_service.model.Hospital;

import java.time.LocalDateTime;

public record HospitalResponse(
        Long id,
        String hospitalName,
        String hospitalLocation,
        String hospitalPhone,
        String hospitalDescription,
        boolean hospitalActive,
        LocalDateTime createdAtHospital,
        LocalDateTime updatedAtHospital
) {

    public static HospitalResponse from(Hospital hospital) {

        return new HospitalResponse(
                hospital.getId(),
                hospital.getHospitalName(),
                hospital.getHospitalLocation(),
                hospital.getHospitalPhone(),
                hospital.getHospitalDescription(),
                hospital.isHospitalActive(),
                hospital.getCreatedAtHospital(),
                hospital.getUpdatedAtHospital()
        );
    }
}
