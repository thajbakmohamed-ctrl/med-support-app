package com.app.med_support.service;

import com.app.med_support.model.DonorProfile;
import com.app.med_support.model.User;
import com.app.med_support.repository.DonorProfileRepository;
import com.app.med_support.request.DonorProfileRequest;
import org.springframework.stereotype.Service;

@Service
public class DonorProfileService {

    private final DonorProfileRepository donorProfileRepository;

    public DonorProfileService(DonorProfileRepository donorProfileRepository) {
        this.donorProfileRepository = donorProfileRepository;
    }
    // Save a new donor profile in the database
    public DonorProfile createDonorProfile(DonorProfile donorProfile) {

        return donorProfileRepository.save(donorProfile);
    }
    public DonorProfile createDonorProfile(User user,
            DonorProfileRequest request) {

        DonorProfile donorProfile = new DonorProfile();


        if (user == null || !"DONOR".equals(user.getRole())) {
            throw new IllegalArgumentException("Only donors can create a donor profile");
        }

        DonorProfile existingProfile = donorProfileRepository.findByUserId(user.getId());

        if (existingProfile != null) {
            throw new IllegalArgumentException("Donor profile already exists");
        }

        if (request == null) {
            throw new IllegalArgumentException("Donor information is required");
        }

        if (request.getBloodType() == null || request.getBloodType().isBlank()
                || request.getDateOfBirth() == null
                || request.getGender() == null || request.getGender().isBlank()
                || request.getWeightKg() == null || request.getWeightKg() <= 0) {

            throw new IllegalArgumentException("Please complete all required donor information");
        }

        if (!request.getDateOfBirth().isBefore(java.time.LocalDate.now())) {
            throw new IllegalArgumentException("Invalid date of birth");
        }

        // Link the donor profile to its owner
        donorProfile.setUser(user);


        donorProfile.setBloodType(request.getBloodType());
        donorProfile.setDateOfBirth(request.getDateOfBirth());
        donorProfile.setGender(request.getGender());
        donorProfile.setWeightKg(request.getWeightKg());

        donorProfile.setMedicalConditions(request.getMedicalConditions());
        donorProfile.setMedications(request.getMedications());
        donorProfile.setAllergies(request.getAllergies());
        donorProfile.setLastDonationDate(request.getLastDonationDate());
        donorProfile.setNotesAboutTheDonor(request.getNotesAboutTheDonor());

        // Default values for a new donor
        donorProfile.setTotalDonations(0);
        donorProfile.setAvailableForBloodDonation(false);

        // Use the CPR document already uploaded by the user
        if (user.getCprDocument() == null || user.getCprDocument().isBlank()) {
            throw new IllegalArgumentException("Please upload your CPR document first");
        }

        donorProfile.setCprImage(user.getCprDocument());

        return donorProfileRepository.save(donorProfile);
    }
}