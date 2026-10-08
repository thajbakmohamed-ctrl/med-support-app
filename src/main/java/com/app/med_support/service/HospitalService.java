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


    public Hospital getHospitalById(Long hospitalId) {

        return hospitalRepository.findById(hospitalId).orElse(null);
    }
    // Create new hospital
    public Hospital createHospital(Hospital hospital) {

        if (hospital == null) {
            return null;
        }
        // Hospital phone number must contain exactly 8 digits
        if (hospital.getHospitalPhone() == null
                || !hospital.getHospitalPhone().matches("\\d{8}")) {
            return null;

        }
        // Hospital phone number must be unique
        if (hospitalRepository.existsByHospitalPhone(hospital.getHospitalPhone())) {
            return null;
        }
        hospital.setHospitalActive(true);
        return hospitalRepository.save(hospital);
    }
    // Update hospital information
    public Hospital updateHospital(Long hospitalId, Hospital hospitalDetails) {
        Hospital hospital = hospitalRepository.findById(hospitalId).orElse(null);
        if (hospital == null) {
            return null;
        }
        // Hospital phone number must contain exactly 8 digits
        if (hospitalDetails.getHospitalPhone() == null
                || !hospitalDetails.getHospitalPhone().matches("\\d{8}")) {
            return null;
        }
        // Hospital phone number must be unique if it is changed
        if (!hospital.getHospitalPhone().equals(hospitalDetails.getHospitalPhone())
                && hospitalRepository.existsByHospitalPhone(hospitalDetails.getHospitalPhone())) {
            return null;
        }

        hospital.setHospitalName(hospitalDetails.getHospitalName());
        hospital.setHospitalLocation(hospitalDetails.getHospitalLocation());
        hospital.setHospitalPhone(hospitalDetails.getHospitalPhone());
        hospital.setHospitalDescription(hospitalDetails.getHospitalDescription());
        return hospitalRepository.save(hospital);
    }

}
