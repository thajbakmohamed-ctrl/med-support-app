
package com.medsupport.identity_service.service;

import com.medsupport.identity_service.model.DonorProfile;
import com.medsupport.identity_service.model.User;
import com.medsupport.identity_service.repository.DonorProfileRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DonorContactLookupService {

    private final DonorProfileRepository donorProfileRepository;

    public DonorContactLookupService(DonorProfileRepository donorProfileRepository) {
        this.donorProfileRepository = donorProfileRepository;
    }

    @Transactional(readOnly = true)
    public String getDonorEmail(Long donorProfileId) {

        if (donorProfileId == null || donorProfileId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Valid donor profile ID is required");
        }

        DonorProfile donorProfile = donorProfileRepository
                .findWithUserById(donorProfileId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Donor profile not found"));

        User user = donorProfile.getUser();

        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {

            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Donor email is unavailable");
        }

        return user.getEmail();
    }
}
