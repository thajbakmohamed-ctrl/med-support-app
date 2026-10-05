package com.app.med_support.service;

import com.app.med_support.model.BloodRequest;
import com.app.med_support.repository.BloodRequestRepository;
import com.app.med_support.repository.DonationBookingRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class BloodRequestService {
    private final DonationBookingRepository donationBookingRepository;
    private final BloodRequestRepository bloodRequestRepository;

    public BloodRequestService(
            DonationBookingRepository donationBookingRepository,
            BloodRequestRepository bloodRequestRepository) {

        this.donationBookingRepository = donationBookingRepository;
        this.bloodRequestRepository = bloodRequestRepository;
    }


    // blood req should not be null or 0 or any num with minus
    public BloodRequest createBloodRequest(BloodRequest bloodRequest) {
        if (bloodRequest.getRequiredBloodUnits() == null
                || bloodRequest.getRequiredBloodUnits() <= 0) {
            return null;
        }
        // blood req date cant be null or yesterday
        if (bloodRequest.getBloodNeededDate() == null
                || bloodRequest.getBloodNeededDate().isBefore(LocalDate.now())) {
            return null;
        }
        // only actual blood type
        String bloodType = bloodRequest.getRequiredBloodType();
        if (bloodType == null ||
                !(bloodType.equals("A+")
                        || bloodType.equals("A-")
                        || bloodType.equals("B+")
                        || bloodType.equals("B-")
                        || bloodType.equals("AB+")
                        || bloodType.equals("AB-")
                        || bloodType.equals("O+")
                        || bloodType.equals("O-"))) {
            return null;
        }
        // urgency
        String urgency = bloodRequest.getBloodRequestUrgency();
        if (urgency == null ||
                !(urgency.equals("LOW")
                        || urgency.equals("MEDIUM")
                        || urgency.equals("HIGH")
                        || urgency.equals("CRITICAL"))) {
            return null;
        }
        bloodRequest.setBloodRequestStatus("OPEN");
        return bloodRequestRepository.save(bloodRequest);
    }


    public BloodRequest updateBloodRequestStatus(Long bloodRequestId, String newStatus) {
        BloodRequest bloodRequest = bloodRequestRepository.findById(bloodRequestId).orElse(null);
        if (bloodRequest == null) {
            return null;
        }
        if (newStatus == null) {
            return null;
        }
        if (!(newStatus.equals("OPEN")
                || newStatus.equals("CLOSED")
                || newStatus.equals("CANCELLED"))) {
            return null;
        }
        bloodRequest.setBloodRequestStatus(newStatus);
        return bloodRequestRepository.save(bloodRequest);
    }


    // Get one blood request by its ID
    public BloodRequest getBloodRequestById(Long bloodRequestId) {
        return bloodRequestRepository.findById(bloodRequestId).orElse(null);
    }

    // Show all open blood requests
    public List<BloodRequest> getOpenBloodRequests() {

        return bloodRequestRepository.findByBloodRequestStatus("OPEN");
    }


    // Show open blood requests by blood type
    public List<BloodRequest> getOpenBloodRequestsByBloodType(String bloodType) {

        return bloodRequestRepository.findByRequiredBloodTypeAndBloodRequestStatus(
                        bloodType, "OPEN");
    }

    // Get all blood requests
    public List<BloodRequest> getAllBloodRequests() {
        return bloodRequestRepository.findAll();
    }

}
