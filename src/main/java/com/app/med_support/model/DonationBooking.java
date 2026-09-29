package com.app.med_support.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "donation_booking")
@Getter
@Setter
public class DonationBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // -Many donation - bookings can belong to - one donor profile-
    @ManyToOne
    @JoinColumn(name = "donor_profile_id")
    private DonorProfile donorProfile;

    //- Many donation - bookings can belong to -one blood request-
    @ManyToOne
    @JoinColumn(name = "blood_request_id")
    private BloodRequest bloodRequest;


    @Column(name = "booking_date")
    private LocalDate donationBookingDate;

    @Column(name = "booking_time")
    private LocalTime donationBookingTime;

    //(the hospital staff will change the donation status )
    // Stores the current status of the donation booking
    //PENDING - when the donor booked it will be pending
    //CONFIRMED - after the hospital staff confirm the booking will be confirmed
    //COMPLETED - the donor attend and the donation is complated
    //CANCELLED - the donor canceled the booking
     @Column(name = "status")
     private String donationBookingStatus;

    // Stores additional notes about the donation booking
    @Column(name = "notes")
    private String notesAboutTheDonationBooking;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAtDonationBooking;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAtDonationBooking;
}


