
package com.medsupport.blood_request_service.service;

import com.medsupport.blood_request_service.model.Hospital;
import com.medsupport.blood_request_service.repository.HospitalRepository;
import com.medsupport.blood_request_service.request.HospitalRequest;
import com.medsupport.blood_request_service.response.HospitalResponse;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class HospitalService {

    private final HospitalRepository hospitalRepository;

    public HospitalService(HospitalRepository hospitalRepository) {
        this.hospitalRepository = hospitalRepository;
    }

    @Transactional
    public HospitalResponse createHospital(HospitalRequest request) {

        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Hospital information is required");
        }

        Hospital hospital = new Hospital();

        hospital.setHospitalName(request.getHospitalName());
        hospital.setHospitalLocation(request.getHospitalLocation());
        hospital.setHospitalPhone(request.getHospitalPhone());
        hospital.setHospitalDescription(request.getHospitalDescription());


        hospital.setHospitalActive(false);

        return HospitalResponse.from(hospitalRepository.save(hospital));
    }

    @Transactional
    public HospitalResponse updateHospital(Long hospitalId,
            HospitalRequest request) {

        if (hospitalId == null || request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Hospital ID and information are required");
        }

        Hospital hospital = findHospital(hospitalId);

        hospital.setHospitalName(request.getHospitalName());
        hospital.setHospitalLocation(request.getHospitalLocation());
        hospital.setHospitalPhone(request.getHospitalPhone());
        hospital.setHospitalDescription(request.getHospitalDescription());

        return HospitalResponse.from(hospitalRepository.save(hospital));
    }

    @Transactional
    public HospitalResponse activateHospital(Long hospitalId) {

        Hospital hospital = findHospital(hospitalId);

        hospital.setHospitalActive(true);

        return HospitalResponse.from(hospitalRepository.save(hospital));
    }

    @Transactional
    public HospitalResponse deactivateHospital(Long hospitalId) {

        Hospital hospital = findHospital(hospitalId);

        hospital.setHospitalActive(false);

        return HospitalResponse.from(hospitalRepository.save(hospital));
    }

    @Transactional(readOnly = true)
    public HospitalResponse getHospitalById(Long hospitalId) {

        return HospitalResponse.from(findHospital(hospitalId));
    }

    @Transactional(readOnly = true)
    public List<HospitalResponse> getAllHospitals() {

        return hospitalRepository.findAll()
                .stream()
                .map(HospitalResponse::from)
                .toList();
    }

    private Hospital findHospital(Long hospitalId) {

        if (hospitalId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Hospital ID is required");
        }

        return hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Hospital not found"));
    }
}
