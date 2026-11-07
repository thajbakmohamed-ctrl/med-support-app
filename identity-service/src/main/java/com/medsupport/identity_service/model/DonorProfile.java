
package com.medsupport.identity_service.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "donor_profile")
public class DonorProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "blood_type", nullable = false)
    private String bloodType;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "gender", nullable = false)
    private String gender;

    @Column(name = "weight_kg", nullable = false)
    private Double weightKg;

    @Column(name = "medical_conditions")
    private String medicalConditions;

    @Column(name = "medications")
    private String medications;

    @Column(name = "allergies")
    private String allergies;

    @Column(name = "last_donation_date")
    private LocalDate lastDonationDate;

    @Column(name = "total_donations", nullable = false)
    private Integer totalDonations = 0;

    @JsonIgnore
    @Column(name = "cpr_image", nullable = false)
    private String cprImage;

    @Column(name = "is_available", nullable = false)
    private boolean isAvailableForBloodDonation = true;

    @Column(name = "notes")
    private String notesAboutTheDonor;

    @CreationTimestamp
    @Column(name = "created_at_donor_profile", nullable = false)
    private LocalDateTime createdAtDonorProfile;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAtDonorProfile;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getBloodType() {
        return bloodType;
    }

    public void setBloodType(String bloodType) {
        this.bloodType = bloodType;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public Double getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(Double weightKg) {
        this.weightKg = weightKg;
    }

    public String getMedicalConditions() {
        return medicalConditions;
    }

    public void setMedicalConditions(String medicalConditions) {
        this.medicalConditions = medicalConditions;
    }

    public String getMedications() {
        return medications;
    }

    public void setMedications(String medications) {
        this.medications = medications;
    }

    public String getAllergies() {
        return allergies;
    }

    public void setAllergies(String allergies) {
        this.allergies = allergies;
    }

    public LocalDate getLastDonationDate() {
        return lastDonationDate;
    }

    public void setLastDonationDate(LocalDate lastDonationDate) {
        this.lastDonationDate = lastDonationDate;
    }

    public Integer getTotalDonations() {
        return totalDonations;
    }

    public void setTotalDonations(Integer totalDonations) {
        this.totalDonations = totalDonations;
    }

    public String getCprImage() {
        return cprImage;
    }

    public void setCprImage(String cprImage) {
        this.cprImage = cprImage;
    }

    public boolean isAvailableForBloodDonation() {
        return isAvailableForBloodDonation;
    }

    public void setAvailableForBloodDonation(boolean available) {
        this.isAvailableForBloodDonation = available;
    }

    public String getNotesAboutTheDonor() {
        return notesAboutTheDonor;
    }

    public void setNotesAboutTheDonor(String notes) {
        this.notesAboutTheDonor = notes;
    }

    public LocalDateTime getCreatedAtDonorProfile() {
        return createdAtDonorProfile;
    }

    public LocalDateTime getUpdatedAtDonorProfile() {
        return updatedAtDonorProfile;
    }
}
