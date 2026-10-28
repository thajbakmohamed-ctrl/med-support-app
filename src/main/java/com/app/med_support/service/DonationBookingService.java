package com.app.med_support.service;

import com.app.med_support.model.BloodRequest;
import com.app.med_support.model.DonationBooking;
import com.app.med_support.model.DonationSlot;
import com.app.med_support.model.DonorProfile;
import com.app.med_support.repository.BloodRequestRepository;
import com.app.med_support.repository.DonationBookingRepository;
import com.app.med_support.repository.DonationSlotRepository;
import com.app.med_support.repository.DonorProfileRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.concurrent.ExecutorService;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class DonationBookingService {

    private static final Logger logger = LoggerFactory.getLogger(DonationBookingService.class);

    private final DonationBookingRepository donationBookingRepository;
    private final BloodRequestRepository bloodRequestRepository;
    private final DonorProfileRepository donorProfileRepository;
    private final BookingNotificationService bookingNotificationService;
    private final AuditLogService auditLogService;
    private final DonationSlotRepository donationSlotRepository;
    private final EmailService emailService;
    private final ExecutorService reminderExecutor;

    public DonationBookingService(DonationBookingRepository donationBookingRepository,
            BloodRequestRepository bloodRequestRepository,
            DonorProfileRepository donorProfileRepository,
            BookingNotificationService bookingNotificationService,
            AuditLogService auditLogService,
            DonationSlotRepository donationSlotRepository,EmailService emailService,
                                  ExecutorService reminderExecutor
    ) {

        this.donationBookingRepository = donationBookingRepository;
        this.bloodRequestRepository = bloodRequestRepository;
        this.donorProfileRepository = donorProfileRepository;
        this.bookingNotificationService = bookingNotificationService;
        this.auditLogService = auditLogService;
        this.donationSlotRepository = donationSlotRepository;
        this.emailService = emailService;
        this.reminderExecutor = reminderExecutor;
    }


    @Transactional
    public DonationBooking createDonationBooking(DonationBooking donationBooking) {

        Long donorProfileId = donationBooking.getDonorProfile().getId();

        // Log the start of a booking attempt
        logger.info("Booking attempt started. Donor profile ID: {}", donorProfileId);

        // Find and lock the donor profile
        DonorProfile donorProfile = donorProfileRepository
                .findByIdForUpdate(donorProfileId).orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Donor profile not found"));

        Long bloodRequestId = donationBooking.getBloodRequest().getId();
        logger.info("Booking requested for blood request ID: {}", bloodRequestId);

        BloodRequest bloodRequest = bloodRequestRepository
                .findById(bloodRequestId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Blood request not found"));

        if (!"OPEN".equals(bloodRequest.getBloodRequestStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Blood request is not open");
        }


        if (donationBooking.getDonationBookingDate() == null
                || donationBooking.getDonationBookingDate().isBefore(LocalDate.now())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Booking date is required and cannot be in the past");
        }

        if (donationBooking.getDonationBookingDate().isAfter(bloodRequest.getBloodNeededDate())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Booking date cannot be after the blood needed date");
        }


        if (donationBooking.getDonationBookingTime() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Donation booking time is required");
        }


        if (donationBooking.getDonationBookingDate().equals(LocalDate.now())
                && donationBooking.getDonationBookingTime().isBefore(LocalTime.now())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Booking time cannot be in the past");
        }

        donationBooking.setDonorProfile(donorProfile);
        donationBooking.setBloodRequest(bloodRequest);


        boolean alreadyBooked = donationBookingRepository
                .existsByDonorProfileIdAndDonationBookingDateAndDonationBookingTimeAndDonationBookingStatusNot(
                        donorProfileId, donationBooking.getDonationBookingDate(),
                        donationBooking.getDonationBookingTime(),
                        "CANCELLED");

        if (alreadyBooked) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Donor already has a booking at this date and time");
        }

        if (donationBooking.getDonationSlot() == null
                || donationBooking.getDonationSlot().getId() == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Donation slot ID is required");
        }

        Long slotId = donationBooking.getDonationSlot().getId();


        DonationSlot slot = donationSlotRepository
                .findByIdForUpdate(slotId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Donation slot not found"));

        try {
            logger.info("Demo delay started. Slot ID: {}", slotId);
            Thread.sleep(3000);
            logger.info("Demo delay finished. Slot ID: {}", slotId);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Booking interrupted", e);
        }


        if (bloodRequest.getHospital() == null || slot.getHospital() == null
                || !slot.getHospital().getId().equals(bloodRequest.getHospital().getId())) {

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Donation slot and blood request must belong to the same hospital");
        }


        if (!donationBooking.getDonationBookingDate().equals(slot.getSlotDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Booking date must match the donation slot date");
        }

        LocalTime bookingTime = donationBooking.getDonationBookingTime();


        if (bookingTime.isBefore(slot.getStartTime()) || !bookingTime.isBefore(slot.getEndTime())) {

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Booking time must be within the donation slot time range");
        }

        if (!"OPEN".equals(slot.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Donation slot is not open");
        }


        if (slot.getReservedCount() >= slot.getCapacity()) {

            // Log when the donation slot is full
            logger.warn("Booking rejected because the donation slot is full. Slot ID: {}",
                    slot.getId());

            throw new ResponseStatusException(HttpStatus.CONFLICT, "Donation slot is full");
        }


        donationBooking.setDonationSlot(slot);
        donationBooking.setDonationBookingStatus("PENDING");


        slot.setReservedCount(slot.getReservedCount() + 1);


        DonationBooking savedBooking = donationBookingRepository.save(donationBooking);

        logger.info("Donation booking created. Booking ID: {}, Donor Profile ID: {}",
                savedBooking.getId(), donorProfileId);

        // Record the successful booking action in the audit log
        auditLogService.saveAuditLog(
                "DONATION_BOOKING_CREATED",
                donorProfile.getUser().getEmail(),
                "Donation booking with ID " + savedBooking.getId()
                        + " was created by the donor");



        return savedBooking;
    }


    @Transactional
    public DonationBooking updateDonationBookingStatus(
            Long bookingId, String newStatus,
            Long hospitalId, String performedByEmail) {
       // Find the booking to identify its blood request
        DonationBooking existingBooking = donationBookingRepository
                .findById(bookingId).orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Donation booking not found"));

        // Lock the blood request first
        BloodRequest bloodRequest = bloodRequestRepository
                .findByIdForUpdate(existingBooking.getBloodRequest().getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Blood request not found"));

        // Lock the donation booking second
        DonationBooking booking = donationBookingRepository
                .findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Donation booking not found"));


        // Ensure the booking still belongs to the locked blood request
        if (!booking.getBloodRequest().getId().equals(bloodRequest.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Booking blood request changed during update");
        }


        if (booking.getBloodRequest() == null || booking.getBloodRequest().getHospital() == null
                || !booking.getBloodRequest().getHospital().getId().equals(hospitalId)) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "You cannot update a booking from another hospital");
        }


        if (newStatus == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Booking status is required");
        }


        if (!(newStatus.equals("PENDING") || newStatus.equals("CONFIRMED")
                || newStatus.equals("COMPLETED") || newStatus.equals("CANCELLED"))) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid booking status");
        }

        String currentStatus = booking.getDonationBookingStatus();

          // Log the booking status change request
        logger.info("Booking status update requested. Booking ID: {}, From: {}, To: {}",
                bookingId, currentStatus, newStatus);
        if ("COMPLETED".equals(currentStatus) || "CANCELLED".equals(currentStatus)) {

            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Completed or cancelled bookings cannot be updated");
        }


        if ("PENDING".equals(currentStatus) && !("CONFIRMED".equals(newStatus)
                || "CANCELLED".equals(newStatus))) {

            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Pending booking can only be confirmed or cancelled");
        }


        if ("CONFIRMED".equals(currentStatus) && !("COMPLETED".equals(newStatus)
                || "CANCELLED".equals(newStatus))) {

            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Confirmed booking can only be completed or cancelled");
        }


        if ("CANCELLED".equals(newStatus) && booking.getDonationSlot() != null) {

            DonationSlot slot = donationSlotRepository
                    .findByIdForUpdate(booking.getDonationSlot().getId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Donation slot not found"));

            if (slot.getReservedCount() <= 0) {
                throw new IllegalStateException("Cannot release slot because reserved count is zero");
            }

            slot.setReservedCount(slot.getReservedCount() - 1);
        }


        booking.setDonationBookingStatus(newStatus);

        DonationBooking updatedBooking = donationBookingRepository.save(booking);

        if ("COMPLETED".equals(newStatus)) {
            donationBookingRepository.flush();

            long completedDonations = donationBookingRepository
                    .countByBloodRequestIdAndDonationBookingStatus(bloodRequest.getId(), "COMPLETED");

            if (completedDonations >= bloodRequest.getRequiredBloodUnits()
                    && "OPEN".equals(bloodRequest.getBloodRequestStatus())) {

                bloodRequest.setBloodRequestStatus("COMPLETED");
                bloodRequestRepository.save(bloodRequest);

                logger.info("Blood request completed. Request ID: {}",
                        bloodRequest.getId());
            }
        }

        bookingNotificationService.sendDonationBookingStatusNotification(updatedBooking);

        // Record the staff member who updated the booking
        auditLogService.saveAuditLog(
                "DONATION_BOOKING_" + newStatus,
                performedByEmail,
                "Donation booking with ID " + bookingId
                        + " changed from " + currentStatus + " to " + newStatus);


        if ("CONFIRMED".equals(newStatus)) {

            String donorEmail = updatedBooking.getDonorProfile().getUser().getEmail();
            Long confirmedBookingId = updatedBooking.getId();

            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {

                        @Override
                        public void afterCommit() {
                            reminderExecutor.submit(() -> {
                                try {emailService.sendBookingConfirmationEmail(
                                            donorEmail, confirmedBookingId);
                                } catch (Exception e) {
                                    logger.error("Failed to send confirmation email for booking ID: {}",
                                            confirmedBookingId, e);
                                }
                            });
                        }
                    }
            );
        }

        if ("CANCELLED".equals(newStatus)) {

            String donorEmail = updatedBooking.getDonorProfile().getUser().getEmail();
            Long cancelledBookingId = updatedBooking.getId();

            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {

                        @Override
                        public void afterCommit() { reminderExecutor.submit(() -> {
                                try {
                                    emailService.sendBookingCancellationEmail(
                                            donorEmail, cancelledBookingId);
                                } catch (Exception e) {
                                    logger.error("Failed to send cancellation email for booking ID: {}",
                                            cancelledBookingId, e);
                                }
                            });
                        }
                    }
            );
        }

        return updatedBooking;
    }

    public List<DonationBooking> getBookingsByDonor(Long donorProfileId) {

        return donationBookingRepository.findByDonorProfileId(donorProfileId);
    }

    public List<DonationBooking> getBookingsByBloodRequest(
            Long bloodRequestId, Long hospitalId) {

        BloodRequest bloodRequest = bloodRequestRepository.findById(bloodRequestId).orElse(null);


        if (bloodRequest == null) {
            return List.of();
        }


        if (bloodRequest.getHospital() == null || !bloodRequest.getHospital().getId().equals(hospitalId)) {

            return List.of();
        }

        return donationBookingRepository.findByBloodRequestId(bloodRequestId);
    }


    @Transactional
    public DonationBooking cancelDonationBooking(Long bookingId,
            Long donorProfileId) {


        DonationBooking booking = donationBookingRepository
                .findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Donation booking not found"));


        if (!booking.getDonorProfile().getId().equals(donorProfileId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You cannot cancel another donor's booking");
        }

        if ("COMPLETED".equals(booking.getDonationBookingStatus())
                || "CANCELLED".equals(booking.getDonationBookingStatus())) {

            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Completed or cancelled bookings cannot be cancelled");
        }

        // Log the start of a booking cancellation
        logger.info("Booking cancellation requested. Booking ID: {}", bookingId);

        DonationSlot bookingSlot = booking.getDonationSlot();


        if (bookingSlot != null) {
            bookingSlot = donationSlotRepository.findByIdForUpdate(bookingSlot.getId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Donation slot not found"));
        }


        if (bookingSlot != null) {

            if (bookingSlot.getReservedCount() <= 0) {
                throw new IllegalStateException("Cannot release slot because reserved count is zero");
            }

            bookingSlot.setReservedCount(bookingSlot.getReservedCount() - 1);
        }


        booking.setDonationBookingStatus("CANCELLED");

        DonationBooking cancelledBooking = donationBookingRepository.save(booking);


        auditLogService.saveAuditLog("DONATION_BOOKING_CANCELLED",
                booking.getDonorProfile().getUser().getEmail(),
                "Donation booking with ID " + bookingId + " was cancelled by the donor");

        logger.info("Donation booking cancellation recorded. Booking ID: {}, Donor Profile ID: {}",
                bookingId, donorProfileId);
        bookingNotificationService.sendDonationBookingStatusNotification(cancelledBooking);



        String donorEmail = cancelledBooking.getDonorProfile().getUser().getEmail();
        Long cancelledBookingId = cancelledBooking.getId();

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {

                    @Override
                    public void afterCommit() {
                        reminderExecutor.submit(() -> {
                            try {
                                emailService.sendBookingCancellationEmail(
                                        donorEmail, cancelledBookingId);
                            } catch (Exception e) {
                                logger.error(
                                        "Failed to send cancellation email for booking ID: {}",
                                        cancelledBookingId, e);
                            }
                        });
                    }
                }
        );


        return cancelledBooking;
    }
}