package com.app.med_support.repository;

import com.app.med_support.model.BloodRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.List;

public interface BloodRequestRepository extends JpaRepository<BloodRequest, Long> {
    List<BloodRequest> findByHospitalId(Long hospitalId);
    List<BloodRequest> findByBloodRequestStatus(String bloodRequestStatus);
    List<BloodRequest> findByRequiredBloodType(String requiredBloodType);
    List<BloodRequest> findByRequiredBloodTypeAndBloodRequestStatus(
            String requiredBloodType, String bloodRequestStatus);
    // Lock blood request while checking completed donations
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM BloodRequest b WHERE b.id = :requestId")
    Optional<BloodRequest> findByIdForUpdate(@Param("requestId") Long requestId);



}
