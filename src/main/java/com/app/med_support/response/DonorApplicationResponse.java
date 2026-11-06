package com.app.med_support.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class DonorApplicationResponse {

    // Booking information
    private Long id;
    private Long bloodRequestId;
    private Long donationSlotId;
    private LocalDate donationBookingDate;
    private LocalTime donationBookingTime;
    private String donationBookingStatus;

    // Donor information
    private String donorName;
    private String donorEmail;
    private String donorPhone;
    private String bloodType;


    // Donation slot information
    private LocalDate slotDate;
    private LocalTime slotStartTime;
    private LocalTime slotEndTime;
    private String slotStatus;
    private Integer slotCapacity;
    private Integer reservedPlaces;

    // Booking additional information
    private String bookingNotes;
    private LocalDateTime applicationReceived;
    private LocalDateTime lastUpdated;


    public DonorApplicationResponse(
            Long id,
            Long bloodRequestId,
            Long donationSlotId,
            LocalDate donationBookingDate,
            LocalTime donationBookingTime,
            String donationBookingStatus,
            String donorName,
            String donorEmail,
            String donorPhone,
            String bloodType,
            LocalDate slotDate,
            LocalTime slotStartTime,
            LocalTime slotEndTime,
            String slotStatus,
            Integer slotCapacity,
            Integer reservedPlaces,
            String bookingNotes,
            LocalDateTime applicationReceived,
            LocalDateTime lastUpdated)  {

        this.id = id;
        this.bloodRequestId = bloodRequestId;
        this.donationSlotId = donationSlotId;
        this.donationBookingDate = donationBookingDate;
        this.donationBookingTime = donationBookingTime;
        this.donationBookingStatus = donationBookingStatus;

        this.donorName = donorName;
        this.donorEmail = donorEmail;
        this.donorPhone = donorPhone;
        this.bloodType = bloodType;


        this.slotDate = slotDate;
        this.slotStartTime = slotStartTime;
        this.slotEndTime = slotEndTime;
        this.slotStatus = slotStatus;
        this.slotCapacity = slotCapacity;
        this.reservedPlaces = reservedPlaces;


        this.bookingNotes = bookingNotes;
        this.applicationReceived = applicationReceived;
        this.lastUpdated = lastUpdated;
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

    public String getDonorName() {
        return donorName;
    }

    public String getDonorEmail() {
        return donorEmail;
    }

    public String getDonorPhone() {
        return donorPhone;
    }

    public String getBloodType() {
        return bloodType;
    }

    public LocalDate getSlotDate() {
        return slotDate;
    }

    public LocalTime getSlotStartTime() {
        return slotStartTime;
    }

    public LocalTime getSlotEndTime() {
        return slotEndTime;
    }

    public String getSlotStatus() {
        return slotStatus;
    }

    public Integer getSlotCapacity() {
        return slotCapacity;
    }

    public Integer getReservedPlaces() {
        return reservedPlaces;
    }

    public String getBookingNotes() {
        return bookingNotes;
    }

    public LocalDateTime getApplicationReceived() {
        return applicationReceived;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }
}
