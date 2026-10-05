package com.app.med_support.service;

import com.app.med_support.model.Hospital;
import com.app.med_support.repository.HospitalRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HospitalService {
    private final HospitalRepository hospitalRepository;

    public HospitalService(HospitalRepository hospitalRepository) {
        this.hospitalRepository = hospitalRepository;
    }


    public List<Hospital> getAllHospitals() {
        return hospitalRepository.findAll();
    }
    //soft delete
    public boolean deactivateHospital(Long hospitalId) {
        Hospital hospital = hospitalRepository.findById(hospitalId).orElse(null);
        if (hospital == null) {
            return false;
        }
        hospital.setHospitalActive(false);
        hospitalRepository.save(hospital);
        return true;
    }
    public boolean reactivateHospital(Long hospitalId) {
        Hospital hospital = hospitalRepository.findById(hospitalId).orElse(null);
        if (hospital == null) {
            return false;
        }
        hospital.setHospitalActive(true);
        hospitalRepository.save(hospital);
        return true;
    }
}
