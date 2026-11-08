
package com.medsupport.booking_service.response;

import com.medsupport.booking_service.model.DonationSlot;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record DonationSlotResponse(
        Long id,
        Long hospitalId,
        LocalDate slotDate,
        LocalTime startTime,
        LocalTime endTime,
        Integer capacity,
        Integer reservedCount,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static DonationSlotResponse from(DonationSlot slot) {
        return new DonationSlotResponse(
                slot.getId(),
                slot.getHospitalId(),
                slot.getSlotDate(),
                slot.getStartTime(),
                slot.getEndTime(),
                slot.getCapacity(),
                slot.getReservedCount(),
                slot.getStatus(),
                slot.getCreatedAt(),
                slot.getUpdatedAt()
        );
    }
}
