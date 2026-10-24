package com.app.med_support.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class DonorProfileRequest {

    private String bloodType;
    private LocalDate dateOfBirth;
    private String gender;
    private Double weightKg;

    private String medicalConditions;
    private String medications;
    private String allergies;

    private LocalDate lastDonationDate;
    private String notesAboutTheDonor;
}