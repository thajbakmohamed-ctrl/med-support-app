package com.app.med_support.repository;

import com.app.med_support.model.BloodRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BloodRequestRepository extends JpaRepository<BloodRequest, Long> {
    List<BloodRequest> findByHospitalId(Long hospitalId);
    List<BloodRequest> findByBloodRequestStatus(String bloodRequestStatus);
    List<BloodRequest> findByRequiredBloodType(String requiredBloodType);
}
