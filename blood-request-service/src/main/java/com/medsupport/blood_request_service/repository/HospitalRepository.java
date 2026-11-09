
package com.medsupport.blood_request_service.repository;

import com.medsupport.blood_request_service.model.Hospital;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HospitalRepository extends JpaRepository<Hospital, Long> {
}
