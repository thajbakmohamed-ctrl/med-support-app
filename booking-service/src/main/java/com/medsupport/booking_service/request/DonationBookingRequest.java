
package com.medsupport.booking_service.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.time.LocalTime;

public class DonationBookingRequest {

    @NotNull(message = "Donor profile ID is required")
    @Positive(message = "Donor profile ID must be positive")
    private Long donorProfileId;

    @NotNull(message = "Blood request ID is required")
    @Positive(message = "Blood request ID must be positive")
    private Long bloodRequestId;

    @NotNull(message = "Donation slot ID is required")
    @Positive(message = "Donation slot ID must be positive")
    private Long donationSlotId;

    @NotNull(message = "Booking date is required")
    @FutureOrPresent(message = "Booking date cannot be in the past")
    private LocalDate donationBookingDate;

    @NotNull(message = "Booking time is required")
    private LocalTime donationBookingTime;

    public Long getDonorProfileId() {
        return donorProfileId;
    }

    public void setDonorProfileId(Long donorProfileId) {
        this.donorProfileId = donorProfileId;
    }

    public Long getBloodRequestId() {
        return bloodRequestId;
    }

    public void setBloodRequestId(Long bloodRequestId) {
        this.bloodRequestId = bloodRequestId;
    }

    public Long getDonationSlotId() {
        return donationSlotId;
    }

    public void setDonationSlotId(Long donationSlotId) {
        this.donationSlotId = donationSlotId;
    }

    public LocalDate getDonationBookingDate() {
        return donationBookingDate;
    }

    public void setDonationBookingDate(LocalDate donationBookingDate) {
        this.donationBookingDate = donationBookingDate;
    }

    public LocalTime getDonationBookingTime() {
        return donationBookingTime;
    }

    public void setDonationBookingTime(LocalTime donationBookingTime) {
        this.donationBookingTime = donationBookingTime;
    }
}
