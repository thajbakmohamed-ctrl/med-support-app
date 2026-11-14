
package com.medsupport.identity_service.response;

import com.medsupport.identity_service.model.DonorProfile;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DonorProfileResponse(Long id, Long userId, String bloodType,
        LocalDate dateOfBirth, String gender, Double weightKg,
        String medicalConditions, String medications, String allergies,
        LocalDate lastDonationDate, Integer totalDonations,
        boolean availableForBloodDonation, String notesAboutTheDonor,
        LocalDateTime createdAtDonorProfile) {

    public static DonorProfileResponse from(DonorProfile profile) {
        return new DonorProfileResponse(profile.getId(),
                profile.getUser().getId(), profile.getBloodType(),
                profile.getDateOfBirth(), profile.getGender(),
                profile.getWeightKg(), profile.getMedicalConditions(),
                profile.getMedications(), profile.getAllergies(),
                profile.getLastDonationDate(), profile.getTotalDonations(),
                profile.isAvailableForBloodDonation(), profile.getNotesAboutTheDonor(),
                profile.getCreatedAtDonorProfile());
    }
}
