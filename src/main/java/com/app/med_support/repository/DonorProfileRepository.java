package com.app.med_support.repository;

import com.app.med_support.model.DonorProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface DonorProfileRepository extends JpaRepository<DonorProfile, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM DonorProfile d WHERE d.id = :donorProfileId")
    Optional<DonorProfile> findByIdForUpdate(@Param("donorProfileId") Long donorProfileId);
    DonorProfile findByUserId(Long userId);
    List<DonorProfile> findByIsAvailableForBloodDonation(boolean isAvailableForBloodDonation);
    List<DonorProfile> findByBloodType(String bloodType);
}
