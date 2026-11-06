
package com.app.med_support.repository;

import com.app.med_support.model.HospitalDepartment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HospitalDepartmentRepository
        extends JpaRepository<HospitalDepartment, Long> {

    List<HospitalDepartment> findByHospitalId(Long hospitalId);
}
