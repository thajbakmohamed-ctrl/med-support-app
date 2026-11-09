
package com.medsupport.blood_request_service.service;

import com.medsupport.blood_request_service.model.BloodRequest;
import com.medsupport.blood_request_service.repository.BloodRequestRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.medsupport.blood_request_service.model.Hospital;
import com.medsupport.blood_request_service.repository.HospitalRepository;
import com.medsupport.blood_request_service.request.BloodRequestRequest;
import com.medsupport.blood_request_service.response.BloodRequestResponse;

import java.util.List;

@Service
public class BloodRequestService {

    private final BloodRequestRepository bloodRequestRepository;
    private final HospitalRepository hospitalRepository;
    public BloodRequestService(BloodRequestRepository bloodRequestRepository,
            HospitalRepository hospitalRepository) {
        this.bloodRequestRepository = bloodRequestRepository;
        this.hospitalRepository = hospitalRepository;
    }


    @Transactional(readOnly = true)
    public BloodRequest getBloodRequestById(Long id) {

        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Blood request ID is required");
        }

        return bloodRequestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Blood request not found"));
    }


    @Transactional(readOnly = true)
    public List<BloodRequest> getAllBloodRequests() {

        return bloodRequestRepository.findAll();
    }

    @Transactional
    public BloodRequestResponse createBloodRequest(
            BloodRequestRequest request,
            Long verifiedHospitalId) {

        if (request == null || verifiedHospitalId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Required blood request information is missing");
        }


        if (!verifiedHospitalId.equals(request.getHospitalId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You cannot create a blood request for another hospital");
        }

        Hospital hospital = hospitalRepository.findById(verifiedHospitalId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Hospital not found"));


        if (!hospital.isHospitalActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Hospital is inactive");
        }


        BloodRequest bloodRequest = new BloodRequest();

        bloodRequest.setHospital(hospital);
        bloodRequest.setRequiredBloodType(request.getRequiredBloodType());
        bloodRequest.setRequiredBloodUnits(request.getRequiredBloodUnits());
        bloodRequest.setBloodRequestUrgency(request.getBloodRequestUrgency());
        bloodRequest.setBloodNeededDate(request.getBloodNeededDate());
        bloodRequest.setBloodRequestDescription(request.getBloodRequestDescription());
        bloodRequest.setBloodRequestStatus("OPEN");

        BloodRequest saved = bloodRequestRepository.save(bloodRequest);

        return BloodRequestResponse.from(saved);
    }

    @Transactional
    public BloodRequestResponse updateBloodRequest(Long bloodRequestId,
            BloodRequestRequest request, Long verifiedHospitalId) {

        if (bloodRequestId == null
                || request == null
                || verifiedHospitalId == null) {

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Required blood request information is missing");
        }

        if (!verifiedHospitalId.equals(request.getHospitalId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "You cannot update another hospital's blood request");
        }

        BloodRequest bloodRequest = bloodRequestRepository
                .findById(bloodRequestId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Blood request not found"));

        if (!verifiedHospitalId.equals(
                bloodRequest.getHospital().getId())) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "This blood request belongs to another hospital");
        }

        if (!"OPEN".equals(bloodRequest.getBloodRequestStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Only open blood requests can be updated");
        }

        bloodRequest.setRequiredBloodType(request.getRequiredBloodType());
        bloodRequest.setRequiredBloodUnits(request.getRequiredBloodUnits());
        bloodRequest.setBloodRequestUrgency(request.getBloodRequestUrgency());
        bloodRequest.setBloodNeededDate(request.getBloodNeededDate());
        bloodRequest.setBloodRequestDescription(request.getBloodRequestDescription());

        BloodRequest updated = bloodRequestRepository.save(bloodRequest);

        return BloodRequestResponse.from(updated);
    }


    @Transactional
    public void deleteBloodRequest(Long bloodRequestId, Long verifiedHospitalId) {

        if (bloodRequestId == null || verifiedHospitalId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Blood request ID and hospital ID are required");
        }

        BloodRequest bloodRequest = bloodRequestRepository
                .findById(bloodRequestId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Blood request not found"));


        if (!verifiedHospitalId.equals(
                bloodRequest.getHospital().getId())) {

            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You cannot delete another hospital's blood request");
        }

        if (!"OPEN".equals(bloodRequest.getBloodRequestStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only open blood requests can be deleted");
        }

        bloodRequestRepository.delete(bloodRequest);
    }



}
