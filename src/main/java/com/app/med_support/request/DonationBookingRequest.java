package com.app.med_support.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class DonationBookingRequest {


    @Schema(description = "ID of the OPEN blood request selected by the donor.",
            example = "5",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Blood request ID is required")
    private Long bloodRequestId;


    @Schema(description = "Donation date. Must match the selected slot date.",
            example = "2026-11-05",
            requiredMode = Schema.RequiredMode.REQUIRED)

    @NotNull(message = "Donation booking date is required")
    private LocalDate donationBookingDate;

    @Schema(description = "Donation time. Must be within the selected slot's time range.",
            example = "11:00:00",
            requiredMode = Schema.RequiredMode.REQUIRED)

    @NotNull(message = "Donation booking time is required")
    private LocalTime donationBookingTime;


    @Schema(description = "Optional notes provided by the donor.",
            example = "Available for donation in the morning.",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)

    private String notesAboutTheDonationBooking;


    @Schema(description = "ID of the available donation slot selected by the donor.",
            example = "3",
            requiredMode = Schema.RequiredMode.REQUIRED)

    @NotNull(message = "Donation slot ID is required")
    private Long donationSlotId;
}