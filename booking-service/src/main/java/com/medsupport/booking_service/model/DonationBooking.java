
package com.medsupport.booking_service.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(
        name = "donation_booking",
        indexes = {
                @Index(
                        name = "idx_booking_donor",
                        columnList = "donor_profile_id"
                ),
                @Index(
                        name = "idx_booking_request",
                        columnList = "blood_request_id"
                ),
                @Index(
                        name = "idx_booking_slot",
                        columnList = "donation_slot_id"
                )
        }
)
public class DonationBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "donor_profile_id", nullable = false)
    private Long donorProfileId;

    @Column(name = "blood_request_id", nullable = false)
    private Long bloodRequestId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "donation_slot_id", nullable = false)
    private DonationSlot donationSlot;

    @Column(name = "donation_booking_date", nullable = false)
    private LocalDate donationBookingDate;

    @Column(name = "donation_booking_time", nullable = false)
    private LocalTime donationBookingTime;

    @Column(name = "donation_booking_status", nullable = false)
    private String donationBookingStatus = "PENDING";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public DonationSlot getDonationSlot() {
        return donationSlot;
    }

    public void setDonationSlot(DonationSlot donationSlot) {
        this.donationSlot = donationSlot;
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

    public String getDonationBookingStatus() {
        return donationBookingStatus;
    }

    public void setDonationBookingStatus(String donationBookingStatus) {
        this.donationBookingStatus = donationBookingStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
