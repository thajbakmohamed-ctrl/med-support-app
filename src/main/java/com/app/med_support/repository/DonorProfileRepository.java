package com.app.med_support.repository;

import com.app.med_support.model.DonorProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DonorProfileRepository extends JpaRepository<DonorProfile, Long> {
    DonorProfile findByUserId(Long userId);
    List<DonorProfile> findByIsAvailableForBloodDonation(boolean isAvailableForBloodDonation);
    List<DonorProfile> findByBloodType(String bloodType);
}
