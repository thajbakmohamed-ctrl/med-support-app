
package com.medsupport.identity_service.service;

import com.medsupport.identity_service.exception.ResourceConflictException;
import com.medsupport.identity_service.model.DonorProfile;
import com.medsupport.identity_service.model.User;
import com.medsupport.identity_service.repository.DonorProfileRepository;
import com.medsupport.identity_service.repository.UserRepository;
import com.medsupport.identity_service.request.DonorProfileRequest;
import com.medsupport.identity_service.response.DonorProfileResponse;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.NoSuchElementException;

@Service
public class DonorProfileService {

    private final DonorProfileRepository donorProfileRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    public DonorProfileService(DonorProfileRepository donorProfileRepository,
            UserRepository userRepository, AuditLogService auditLogService) {

        this.donorProfileRepository = donorProfileRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public DonorProfile createDonorProfile(String email, DonorProfileRequest request) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        if (!"DONOR".equals(user.getRole())) {
            throw new IllegalArgumentException("Only donors can create a donor profile");
        }

        if (!"ACTIVE".equals(user.getStatus()) || !user.isEmailVerified()) {
            throw new IllegalArgumentException("User account must be active and verified");
        }

        if (donorProfileRepository.existsByUserId(user.getId())) {
            throw new ResourceConflictException("Donor profile already exists");
        }

        if (request == null
                || request.getBloodType() == null
                || request.getBloodType().isBlank()
                || request.getDateOfBirth() == null
                || request.getGender() == null
                || request.getGender().isBlank()
                || request.getWeightKg() == null
                || request.getWeightKg() <= 0) {

            throw new IllegalArgumentException("Please complete all required donor information");
        }

        if (!request.getDateOfBirth().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Invalid date of birth");
        }

        if (user.getCprDocument() == null || user.getCprDocument().isBlank()) {
            throw new IllegalArgumentException("Please upload your CPR document first");
        }

        DonorProfile profile = new DonorProfile();

        profile.setUser(user);
        profile.setBloodType(request.getBloodType());
        profile.setDateOfBirth(request.getDateOfBirth());
        profile.setGender(request.getGender());
        profile.setWeightKg(request.getWeightKg());

        profile.setMedicalConditions(request.getMedicalConditions());
        profile.setMedications(request.getMedications());
        profile.setAllergies(request.getAllergies());
        profile.setLastDonationDate(request.getLastDonationDate());
        profile.setNotesAboutTheDonor(request.getNotesAboutTheDonor());

        profile.setTotalDonations(0);
        profile.setAvailableForBloodDonation(false);
        profile.setCprImage(user.getCprDocument());


        DonorProfile savedProfile = donorProfileRepository.save(profile);


        auditLogService.saveAuditLog(
                "DONOR_PROFILE_CREATED", email,
                "Donor profile created for user ID " + user.getId());

        return savedProfile;

    }

    @Transactional(readOnly = true)
    public DonorProfileResponse getMyDonorProfile(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        DonorProfile profile = donorProfileRepository
                .findByUserId(user.getId())
                .orElseThrow(() ->
                        new NoSuchElementException("Donor profile not found"));

        return DonorProfileResponse.from(profile);
    }

    @Transactional
    public DonorProfileResponse updateMyDonorProfile(String email, DonorProfileRequest request) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        DonorProfile profile = donorProfileRepository
                .findByUserId(user.getId()).orElseThrow(() ->
                        new NoSuchElementException("Donor profile not found"));

        if (!"DONOR".equals(user.getRole())) {
            throw new IllegalArgumentException("Only donors can update a donor profile");
        }

        if (request == null) {
            throw new IllegalArgumentException("Donor profile information is required");
        }

        profile.setBloodType(request.getBloodType());
        profile.setDateOfBirth(request.getDateOfBirth());
        profile.setGender(request.getGender());
        profile.setWeightKg(request.getWeightKg());

        profile.setMedicalConditions(request.getMedicalConditions());
        profile.setMedications(request.getMedications());
        profile.setAllergies(request.getAllergies());
        profile.setLastDonationDate(request.getLastDonationDate());
        profile.setNotesAboutTheDonor(request.getNotesAboutTheDonor());


        DonorProfile updatedProfile = donorProfileRepository.save(profile);
        auditLogService.saveAuditLog(
                "DONOR_PROFILE_UPDATED", email,
                "Donor profile updated for user ID " + user.getId());

        return DonorProfileResponse.from(updatedProfile);

    }
}
