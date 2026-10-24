package com.app.med_support.response;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class DonationSlotResponse {

    private Long id;
    private Long hospitalId;
    private LocalDate slotDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer capacity;
    private Integer reservedCount;
    private String status;
}