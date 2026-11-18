
package com.medsupport.blood_request_service.repository;

import com.medsupport.blood_request_service.model.BloodRequest;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BloodRequestRepository
        extends JpaRepository<BloodRequest, Long> {

    List<BloodRequest> findByBloodRequestStatus(String status);


    List<BloodRequest> findByRequiredBloodTypeAndBloodRequestStatus(
            String requiredBloodType, String status);

    List<BloodRequest> findByHospital_Id(Long hospitalId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM BloodRequest b WHERE b.id = :requestId")
    Optional<BloodRequest> findByIdForUpdate(@Param("requestId") Long requestId);
}
