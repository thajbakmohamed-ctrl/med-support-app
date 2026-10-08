package com.app.med_support.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class DonationBookingRequest {

    @NotNull(message = "Blood request ID is required")
    private Long bloodRequestId;

    @NotNull(message = "Donation booking date is required")
    private LocalDate donationBookingDate;

    @NotNull(message = "Donation booking time is required")
    private LocalTime donationBookingTime;

    private String notesAboutTheDonationBooking;
}
