package com.app.med_support.response;

import java.time.LocalDate;
import java.time.LocalTime;

public class DonationBookingResponse {

    private Long id;
    private Long bloodRequestId;
    private Long donationSlotId;
    private LocalDate donationBookingDate;
    private LocalTime donationBookingTime;
    private String donationBookingStatus;

    public DonationBookingResponse(Long id, Long bloodRequestId,
            Long donationSlotId, LocalDate donationBookingDate,
            LocalTime donationBookingTime, String donationBookingStatus) {

        this.id = id;
        this.bloodRequestId = bloodRequestId;
        this.donationSlotId = donationSlotId;
        this.donationBookingDate = donationBookingDate;
        this.donationBookingTime = donationBookingTime;
        this.donationBookingStatus = donationBookingStatus;
    }

    public Long getId() {
        return id;
    }

    public Long getBloodRequestId() {
        return bloodRequestId;
    }

    public Long getDonationSlotId() {
        return donationSlotId;
    }

    public LocalDate getDonationBookingDate() {
        return donationBookingDate;
    }

    public LocalTime getDonationBookingTime() {
        return donationBookingTime;
    }

    public String getDonationBookingStatus() {
        return donationBookingStatus;
    }
}