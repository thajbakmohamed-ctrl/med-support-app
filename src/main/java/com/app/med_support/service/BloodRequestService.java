package com.app.med_support.service;

import com.app.med_support.model.BloodRequest;
import com.app.med_support.model.Hospital;
import com.app.med_support.repository.BloodRequestRepository;
import com.app.med_support.repository.DonationBookingRepository;
import com.app.med_support.repository.HospitalRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
public class BloodRequestService {

    private final DonationBookingRepository donationBookingRepository;
    private final BloodRequestRepository bloodRequestRepository;
    private final HospitalRepository hospitalRepository;

    private static final Logger logger = LoggerFactory.getLogger(BloodRequestService.class);

    public BloodRequestService(DonationBookingRepository donationBookingRepository,
            BloodRequestRepository bloodRequestRepository,
            HospitalRepository hospitalRepository) {

        this.donationBookingRepository = donationBookingRepository;
        this.bloodRequestRepository = bloodRequestRepository;
        this.hospitalRepository = hospitalRepository;
    }


    public BloodRequest createBloodRequest(BloodRequest bloodRequest) {


        logger.info("Blood request creation started");

        if (bloodRequest.getHospital() == null || bloodRequest.getHospital().getId() == null) {

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Hospital ID is required");
        }

        Long hospitalId = bloodRequest.getHospital().getId();

        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Hospital not found"));

        if (!hospital.isHospitalActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hospital is not active");
        }

        bloodRequest.setHospital(hospital);

        if (bloodRequest.getRequiredBloodUnits() == null || bloodRequest.getRequiredBloodUnits() <= 0) {

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Required blood units must be greater than zero");
        }


        if (bloodRequest.getBloodNeededDate() == null || bloodRequest.getBloodNeededDate().isBefore(LocalDate.now())) {

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Blood needed date is required and cannot be in the past");
        }


        String bloodType = bloodRequest.getRequiredBloodType();

        if (bloodType == null
                || !(bloodType.equals("A+")
                || bloodType.equals("A-")
                || bloodType.equals("B+")
                || bloodType.equals("B-")
                || bloodType.equals("AB+")
                || bloodType.equals("AB-")
                || bloodType.equals("O+")
                || bloodType.equals("O-"))) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid blood type");
        }


        String urgency = bloodRequest.getBloodRequestUrgency();

        if (urgency == null
                || !(urgency.equals("LOW")
                || urgency.equals("MEDIUM")
                || urgency.equals("HIGH")
                || urgency.equals("CRITICAL"))) {

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid blood request urgency");
        }

        bloodRequest.setBloodRequestStatus("OPEN");


        BloodRequest savedRequest = bloodRequestRepository.save(bloodRequest);


        logger.info("Blood request saved. Request ID: {}", savedRequest.getId());

        return savedRequest;
    }

    public BloodRequest updateBloodRequestStatus(
            Long bloodRequestId, String newStatus, Long hospitalId) {


        BloodRequest bloodRequest = bloodRequestRepository
                .findById(bloodRequestId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Blood request not found"));


        if (bloodRequest.getHospital() == null || !bloodRequest.getHospital().getId().equals(hospitalId)) {

            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You cannot update a blood request from another hospital");
        }


        if (newStatus == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Blood request status is required");
        }


        if (!(newStatus.equals("OPEN") || newStatus.equals("CLOSED")
                || newStatus.equals("CANCELLED"))) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid blood request status");
        }


        logger.info("Blood request status change requested. Request ID: {}, From: {}, To: {}",
                bloodRequest.getId(), bloodRequest.getBloodRequestStatus(),
                newStatus);
        bloodRequest.setBloodRequestStatus(newStatus);

        return bloodRequestRepository.save(bloodRequest);
    }

    public BloodRequest getBloodRequestById(Long bloodRequestId) {

        return bloodRequestRepository.findById(bloodRequestId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Blood request not found"));
    }

    public List<BloodRequest> getOpenBloodRequests() {

        return bloodRequestRepository.findByBloodRequestStatus("OPEN");
    }


    public List<BloodRequest> getOpenBloodRequestsByBloodType(String bloodType) {

        return bloodRequestRepository.findByRequiredBloodTypeAndBloodRequestStatus(
                        bloodType, "OPEN");
    }


    public List<BloodRequest> getAllBloodRequests() {

        return bloodRequestRepository.findAll();
    }


    public List<BloodRequest> getBloodRequestsByHospital(Long hospitalId) {

        return bloodRequestRepository.findByHospitalId(hospitalId);
    }

    public Page<BloodRequest> getAllBloodRequests(Pageable pageable) {

        return bloodRequestRepository.findAll(pageable);
    }
}