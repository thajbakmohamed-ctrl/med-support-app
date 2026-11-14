
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;

@Service
public class BloodRequestService {

    private final BloodRequestRepository bloodRequestRepository;
    private final HospitalRepository hospitalRepository;
    private final JdbcTemplate jdbcTemplate;
    private final AuditLogService auditLogService;
    public BloodRequestService(BloodRequestRepository bloodRequestRepository,
            HospitalRepository hospitalRepository,
            JdbcTemplate jdbcTemplate, AuditLogService auditLogService) {

        this.bloodRequestRepository = bloodRequestRepository;
        this.hospitalRepository = hospitalRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.auditLogService = auditLogService;
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
    public BloodRequestResponse createBloodRequest(BloodRequestRequest request,
            Long verifiedHospitalId, String performedByEmail) {

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

        auditLogService.saveAuditLog("BLOOD_REQUEST_CREATED",
                performedByEmail, "Blood request ID " + saved.getId()
                        + " created for hospital " + verifiedHospitalId);

        return BloodRequestResponse.from(saved);

    }


    @Transactional
    public BloodRequestResponse updateBloodRequest(Long bloodRequestId, BloodRequestRequest request,
            Long verifiedHospitalId, String performedByEmail) {

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
                .findByIdForUpdate(bloodRequestId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Blood request not found"));

        if (!verifiedHospitalId.equals(bloodRequest.getHospital().getId())) {

            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This blood request belongs to another hospital");
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

        auditLogService.saveAuditLog(
                "BLOOD_REQUEST_UPDATED",
                performedByEmail,
                "Blood request ID " + bloodRequestId
                        + " updated by hospital " + verifiedHospitalId
        );

        return BloodRequestResponse.from(updated);

    }

    @Transactional
    public void deleteBloodRequest(
            Long bloodRequestId,
            Long verifiedHospitalId,
            String performedByEmail) {

        if (bloodRequestId == null || verifiedHospitalId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Blood request ID and hospital ID are required");
        }

        BloodRequest bloodRequest = bloodRequestRepository
                .findByIdForUpdate(bloodRequestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Blood request not found"));


        if (!verifiedHospitalId.equals(bloodRequest.getHospital().getId())) {

            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You cannot delete another hospital's blood request");
        }

        if (!"OPEN".equals(bloodRequest.getBloodRequestStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only open blood requests can be deleted");
        }

        Long bookingCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM donation_booking
                WHERE blood_request_id = ?
                """,
                Long.class, bloodRequestId);

        if (bookingCount != null && bookingCount > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot delete a blood request with existing bookings");
        }


        bloodRequestRepository.delete(bloodRequest);


        auditLogService.saveAuditLog(
                "BLOOD_REQUEST_DELETED", performedByEmail,
                "Blood request ID " + bloodRequestId
                        + " deleted by hospital " + verifiedHospitalId);

    }

    @Transactional
    public BloodRequestResponse updateBloodRequestStatus(Long bloodRequestId, String newStatus,
            Long verifiedHospitalId, String performedByEmail) {

        if (bloodRequestId == null || verifiedHospitalId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Blood request ID and hospital ID are required");
        }

        BloodRequest bloodRequest = bloodRequestRepository
                .findByIdForUpdate(bloodRequestId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Blood request not found"));


        if (bloodRequest.getHospital() == null
                || !verifiedHospitalId.equals(
                bloodRequest.getHospital().getId())) {

            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You cannot update another hospital's blood request");
        }

        String currentStatus = bloodRequest.getBloodRequestStatus();

        if ("COMPLETED".equals(currentStatus) || "CANCELLED".equals(currentStatus)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Completed or cancelled blood requests cannot be updated");
        }

        if ("CLOSED".equals(currentStatus) && "OPEN".equals(newStatus)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Closed blood requests cannot be reopened");
        }



        if (newStatus == null || newStatus.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Blood request status is required");
        }

        if (!newStatus.equals("OPEN") && !newStatus.equals("CLOSED")
                && !newStatus.equals("CANCELLED")) {

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid blood request status");
        }

        bloodRequest.setBloodRequestStatus(newStatus);
        BloodRequest saved = bloodRequestRepository.save(bloodRequest);
        auditLogService.saveAuditLog(
                "BLOOD_REQUEST_" + newStatus,
                performedByEmail,
                "Blood request ID " + bloodRequestId
                        + " changed to " + newStatus);

        return BloodRequestResponse.from(saved);

    }


    @Transactional(readOnly = true)
    public List<BloodRequest> getOpenBloodRequests() {
        return bloodRequestRepository.findByBloodRequestStatus("OPEN");
    }


    @Transactional(readOnly = true)
    public List<BloodRequest> getOpenBloodRequestsByBloodType(String bloodType) {

        if (bloodType == null || bloodType.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Blood type is required");
        }

        if (!bloodType.matches("^(A|B|AB|O)[+-]$")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid blood type");
        }

        return bloodRequestRepository.findByRequiredBloodTypeAndBloodRequestStatus(
                        bloodType, "OPEN");
    }


    @Transactional(readOnly = true)
    public Page<BloodRequest> getAllBloodRequests(Pageable pageable) {

        return bloodRequestRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public List<BloodRequest> getBloodRequestsByHospital(Long hospitalId) {

        if (hospitalId == null || hospitalId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Valid hospital ID is required");
        }

        return bloodRequestRepository.findByHospital_Id(hospitalId);
    }







}
