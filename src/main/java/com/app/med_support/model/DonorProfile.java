package com.app.med_support.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "donor_profile")
@Getter
@Setter

public class DonorProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Links the donor profile to one user --
    // the  relationship between donor profile and user is one to one
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "blood_type", nullable = false)
    private String bloodType;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "gender", nullable = false)
    private String gender;

    @Column(name = "weight_kg", nullable = false)
    private Double weightKg;

    // Stores the donors medical conditions
    @Column(name = "medical_conditions")
    private String medicalConditions;

    // Stores the donors current medications
    @Column(name = "medications")
    private String medications;


    @Column(name = "allergies")
    private String allergies;

    @Column(name = "last_donation_date")
    private LocalDate lastDonationDate;

    @Column(name = "total_donations", nullable = false)
    private Integer totalDonations;

    // Storing the path or url of the donors (cpr image)
    @Column(name = "cpr_image", nullable = false)
    private String cprImage;


    @Column(name = "is_available", nullable = false)
    private boolean isAvailableForBloodDonation;


    // Stores additional notes about the donor
    @Column(name = "notes")
    private String notesAboutTheDonor;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAtDonorProfile;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAtDonorProfile;
}

