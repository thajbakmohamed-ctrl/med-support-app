
package com.app.med_support.service;

import com.app.med_support.model.HospitalDepartment;
import com.app.med_support.repository.HospitalDepartmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HospitalDepartmentService {

    private final HospitalDepartmentRepository departmentRepository;

    public HospitalDepartmentService(
            HospitalDepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    public HospitalDepartment createDepartment(
            HospitalDepartment department) {

        return departmentRepository.save(department);
    }

    public List<HospitalDepartment> getDepartmentsByHospital(Long hospitalId) {

        return departmentRepository.findByHospitalId(hospitalId);
    }
}

