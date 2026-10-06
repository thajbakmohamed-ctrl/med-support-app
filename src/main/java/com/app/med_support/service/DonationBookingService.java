package com.app.med_support.service;

import com.app.med_support.model.BloodRequest;
import com.app.med_support.model.DonationBooking;
import com.app.med_support.model.DonorProfile;
import com.app.med_support.repository.BloodRequestRepository;
import com.app.med_support.repository.DonationBookingRepository;
import com.app.med_support.repository.DonorProfileRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class DonationBookingService {

    private final DonationBookingRepository donationBookingRepository;
    private final BloodRequestRepository bloodRequestRepository;
    private final DonorProfileRepository donorProfileRepository;
    private final BookingNotificationService bookingNotificationService;

    public DonationBookingService(
            DonationBookingRepository donationBookingRepository,
            BloodRequestRepository bloodRequestRepository,
            DonorProfileRepository donorProfileRepository,
            BookingNotificationService bookingNotificationService) {

        this.donationBookingRepository = donationBookingRepository;
        this.bloodRequestRepository = bloodRequestRepository;
        this.donorProfileRepository = donorProfileRepository;
        this.bookingNotificationService = bookingNotificationService;
    }


    // no double booking + validation+ create booking
    public DonationBooking createDonationBooking(DonationBooking donationBooking) {
        Long donorProfileId = donationBooking.getDonorProfile().getId();
        DonorProfile donorProfile = donorProfileRepository.findById(donorProfileId).orElse(null);
        if (donorProfile == null) {
            return null;
        }
        Long bloodRequestId = donationBooking.getBloodRequest().getId();
        BloodRequest bloodRequest = bloodRequestRepository.findById(bloodRequestId).orElse(null);
        if (bloodRequest == null) {
            return null;
        }
        if (!bloodRequest.getBloodRequestStatus().equals("OPEN")) {
            return null;
        }
        if (donationBooking.getDonationBookingDate() != null
        && donationBooking.getDonationBookingDate()
        .isAfter(bloodRequest.getBloodNeededDate())) {
            return null;
        }

        if (donationBooking.getDonationBookingDate() == null
                || donationBooking.getDonationBookingDate().isBefore(LocalDate.now())) {
            return null;
        }

        if (donationBooking.getDonationBookingTime() == null) {
            return null;
        }

        if (donationBooking.getDonationBookingDate().equals(LocalDate.now())
                && donationBooking.getDonationBookingTime().isBefore(LocalTime.now())) {
            return null;
        }
        donationBooking.setDonorProfile(donorProfile);
        donationBooking.setBloodRequest(bloodRequest);

        // Prevent double booking unless the previous booking was cancelled
        boolean alreadyBooked = donationBookingRepository
        .existsByDonorProfileIdAndDonationBookingDateAndDonationBookingTimeAndDonationBookingStatusNot(
        donorProfileId, donationBooking.getDonationBookingDate(), donationBooking.getDonationBookingTime(),
        "CANCELLED");

        if (alreadyBooked) {
            return null;
        }

        donationBooking.setDonationBookingStatus("PENDING");
        return donationBookingRepository.save(donationBooking);
    }


    public DonationBooking updateDonationBookingStatus(Long bookingId, String newStatus) {
        DonationBooking booking = donationBookingRepository.findById(bookingId).orElse(null);
        if (booking == null) {
            return null;
        }
        if (newStatus == null) {
            return null;
        }
        // Only these statuses are allowed
        if (!(newStatus.equals("PENDING")
                || newStatus.equals("CONFIRMED")
                || newStatus.equals("COMPLETED")
                || newStatus.equals("CANCELLED"))) {
            return null;
        }
        String currentStatus = booking.getDonationBookingStatus();
        // Completed or cancelled bookings cannot be changed
        if (currentStatus.equals("COMPLETED")
                || currentStatus.equals("CANCELLED")) {
            return null;
        }
        // Pending can only become confirmed or cancelled
        if (currentStatus.equals("PENDING")
                && !(newStatus.equals("CONFIRMED")
                || newStatus.equals("CANCELLED"))) {
            return null;
        }
        // Confirmed can only become completed or cancelled
        if (currentStatus.equals("CONFIRMED")
                && !(newStatus.equals("COMPLETED")
                || newStatus.equals("CANCELLED"))) {
            return null;
        }
        booking.setDonationBookingStatus(newStatus);
        DonationBooking updatedBooking = donationBookingRepository.save(booking);

        // Send a real-time notification when the booking status changes
        bookingNotificationService.sendDonationBookingStatusNotification(updatedBooking);
        return updatedBooking;
    }


    // showing the booking for selected donor
    public List<DonationBooking> getBookingsByDonor(Long donorProfileId) {
        return donationBookingRepository.findByDonorProfileId(donorProfileId);

    }


    public List<DonationBooking> getBookingsByBloodRequest(Long bloodRequestId, Long hospitalId) {
        BloodRequest bloodRequest = bloodRequestRepository.findById(bloodRequestId).orElse(null);

        if (bloodRequest == null) {
            return List.of();
        }

        // Hospital staff can only view bookings for their own hospital
        if (bloodRequest.getHospital() == null
                || !bloodRequest.getHospital().getId().equals(hospitalId)) {
            return List.of();
        }
        return donationBookingRepository.findByBloodRequestId(bloodRequestId);
    }


    // donor canceled their booking
    public DonationBooking cancelDonationBooking(Long bookingId,Long donorProfileId) {
        DonationBooking booking = donationBookingRepository.findById(bookingId).orElse(null);
        if (booking == null) {
            return null;
        }
        if (!booking.getDonorProfile().getId().equals(donorProfileId)) {
            return null;
        }
        if (booking.getDonationBookingStatus().equals("COMPLETED")
                || booking.getDonationBookingStatus().equals("CANCELLED")) {
            return null;
        }
        booking.setDonationBookingStatus("CANCELLED");
        return donationBookingRepository.save(booking);
    }


}
