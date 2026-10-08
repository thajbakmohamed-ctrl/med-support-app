package com.app.med_support.model;

import java.time.LocalDateTime;

public class BookingNotification {

    private Long donationBookingId;
    private String donationBookingStatus;
    private String notificationMessage;
    private LocalDateTime notificationTime;

    public BookingNotification() {
    }


    public BookingNotification(
            Long donationBookingId,
            String donationBookingStatus,
            String notificationMessage) {

        this.donationBookingId = donationBookingId;
        this.donationBookingStatus = donationBookingStatus;
        this.notificationMessage = notificationMessage;
        this.notificationTime = LocalDateTime.now();
    }


    public Long getDonationBookingId() {
        return donationBookingId;
    }


    public void setDonationBookingId(Long donationBookingId) {
        this.donationBookingId = donationBookingId;
    }


    public String getDonationBookingStatus() {
        return donationBookingStatus;
    }


    public void setDonationBookingStatus(String donationBookingStatus) {
        this.donationBookingStatus = donationBookingStatus;
    }


    public String getNotificationMessage() {
        return notificationMessage;
    }


    public void setNotificationMessage(String notificationMessage) {
        this.notificationMessage = notificationMessage;
    }


    public LocalDateTime getNotificationTime() {
        return notificationTime;
    }


    public void setNotificationTime(LocalDateTime notificationTime) {
        this.notificationTime = notificationTime;
    }
}
