
package com.medsupport.blood_request_service.repository;

import com.medsupport.blood_request_service.model.BloodRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BloodRequestRepository extends JpaRepository<BloodRequest, Long> {
}
