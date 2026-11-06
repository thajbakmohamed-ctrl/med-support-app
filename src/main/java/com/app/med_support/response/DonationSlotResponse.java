package com.app.med_support.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@Schema(description = "Response containing donation slot details and reservation capacity.")
public class DonationSlotResponse {

    @Schema(description = "Unique ID of the donation slot.",
            example = "3")

    private Long id;

    @Schema(description = "ID of the hospital that owns this donation slot.",
            example = "1")

    private Long hospitalId;


    @Schema(
            description = "Date of the donation slot.",
            example = "2026-11-05"
    )
    private LocalDate slotDate;


    @Schema(description = "Time when the donation slot starts.",
            example = "09:00:00")

    private LocalTime startTime;


    @Schema(description = "Time when the donation slot ends.",
            example = "12:00:00")

    private LocalTime endTime;

    @Schema(description = "Maximum number of bookings allowed for this slot.",
            example = "5")

    private Integer capacity;


    @Schema(description = "Number of places currently reserved in this slot.",
            example = "2")

    private Integer reservedCount;

    @Schema(description = "Current slot status stored by the backend, such as OPEN or CLOSED.",
            example = "OPEN")

    private String status;
}