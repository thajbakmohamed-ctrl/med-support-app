package com.app.med_support.repository;

import com.app.med_support.model.Hospital;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HospitalRepository extends JpaRepository<Hospital, Long> {
    Hospital findByHospitalName(String hospitalName);
    List<Hospital> findByHospitalActive(boolean hospitalActive);
    boolean existsByHospitalPhone(String hospitalPhone);
}
