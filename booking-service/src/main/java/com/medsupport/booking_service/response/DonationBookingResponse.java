
package com.medsupport.booking_service.response;

import com.medsupport.booking_service.model.DonationBooking;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record DonationBookingResponse(
        Long id,
        Long donorProfileId,
        Long bloodRequestId,
        Long donationSlotId,
        LocalDate donationBookingDate,
        LocalTime donationBookingTime,
        String donationBookingStatus,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static DonationBookingResponse from(
            DonationBooking booking) {

        return new DonationBookingResponse(
                booking.getId(),
                booking.getDonorProfileId(),
                booking.getBloodRequestId(),
                booking.getDonationSlot().getId(),
                booking.getDonationBookingDate(),
                booking.getDonationBookingTime(),
                booking.getDonationBookingStatus(),
                booking.getCreatedAt(),
                booking.getUpdatedAt()
        );
    }
}
