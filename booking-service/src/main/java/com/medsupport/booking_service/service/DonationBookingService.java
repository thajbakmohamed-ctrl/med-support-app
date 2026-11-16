
package com.medsupport.booking_service.service;

import com.medsupport.booking_service.model.DonationBooking;
import com.medsupport.booking_service.model.DonationSlot;
import com.medsupport.booking_service.repository.DonationBookingRepository;
import com.medsupport.booking_service.repository.DonationSlotRepository;
import com.medsupport.booking_service.request.DonationBookingRequest;
import com.medsupport.booking_service.response.DonationBookingResponse;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DonationBookingService {

    @Transactional
    public DonationBookingResponse rescheduleBooking(Long bookingId, DonationBookingRequest request,
            Long donorId, Long hospitalId, LocalDate neededDate, String email) {
        if (bookingId == null || request == null || donorId == null || hospitalId == null || neededDate == null
                || request.getDonationSlotId() == null || request.getDonationBookingDate() == null
                || request.getDonationBookingTime() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Required rescheduling information is missing");
        validateAuditEmail(email);
        DonationBooking snapshot = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));
        if (!donorId.equals(snapshot.getDonorProfileId()) || !donorId.equals(request.getDonorProfileId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot reschedule another donor's booking");
        Long requestId = snapshot.getBloodRequestId(), oldSlotId = snapshot.getDonationSlot().getId();
        Long newSlotId = request.getDonationSlotId();
        if (!requestId.equals(request.getBloodRequestId()) || oldSlotId.equals(newSlotId))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Keep the same blood request and choose a different slot");

        @SuppressWarnings("unchecked")
        List<Object[]> requests = entityManager.createNativeQuery("""
                SELECT r.hospital_id, r.needed_date, r.status, h.is_active
                FROM blood_request r JOIN hospital h ON h.id=r.hospital_id
                WHERE r.id=:requestId FOR UPDATE OF r
                """).setParameter("requestId", requestId).getResultList();
        if (requests.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Blood request not found");
        Object[] row = requests.get(0);
        LocalDate lockedDate = row[1] instanceof LocalDate date ? date : ((java.sql.Date)row[1]).toLocalDate();
        if (!hospitalId.equals(((Number)row[0]).longValue()) || !neededDate.equals(lockedDate)
                || !"OPEN".equals(row[2]) || !Boolean.TRUE.equals(row[3]))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Blood request or hospital is no longer available. Refresh requests.");
        entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(:donorId)")
                .setParameter("donorId", donorId).getSingleResult();

        // Match existing request-first locking; two-slot operations always lock IDs ascending.
        DonationSlot first = slotRepository.findByIdForUpdate(Math.min(oldSlotId, newSlotId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Slot not found"));
        entityManager.refresh(first, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        DonationSlot second = slotRepository.findByIdForUpdate(Math.max(oldSlotId, newSlotId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Slot not found"));
        entityManager.refresh(second, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        DonationSlot oldSlot = oldSlotId.equals(first.getId()) ? first : second;
        DonationSlot newSlot = newSlotId.equals(first.getId()) ? first : second;
        DonationBooking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));
        entityManager.refresh(booking, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if (!donorId.equals(booking.getDonorProfileId()) || !requestId.equals(booking.getBloodRequestId())
                || !oldSlotId.equals(booking.getDonationSlot().getId()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Booking changed. Refresh before rescheduling.");
        if (!("PENDING".equals(booking.getDonationBookingStatus()) || "CONFIRMED".equals(booking.getDonationBookingStatus()))
                || !LocalDateTime.of(booking.getDonationBookingDate(), booking.getDonationBookingTime()).isAfter(LocalDateTime.now()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only upcoming pending or confirmed bookings can be rescheduled");
        Number receipt = (Number) entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM p4_donation_reminder WHERE booking_id=:id")
                .setParameter("id", bookingId).getSingleResult();
        if (receipt.longValue() > 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Rescheduling is unavailable after an appointment reminder has been queued");
        LocalDate date = request.getDonationBookingDate();
        LocalTime time = request.getDonationBookingTime();
        if (!hospitalId.equals(oldSlot.getHospitalId()) || !hospitalId.equals(newSlot.getHospitalId())
                || !"OPEN".equals(newSlot.getStatus()) || !date.equals(newSlot.getSlotDate())
                || date.isAfter(lockedDate) || time.isBefore(newSlot.getStartTime()) || !time.isBefore(newSlot.getEndTime())
                || !LocalDateTime.of(date, time).isAfter(LocalDateTime.now()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a future open slot in the same hospital, before the request deadline");
        if (newSlot.getReservedCount() >= newSlot.getCapacity())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Donation slot is full. Your original appointment is unchanged.");
        if (oldSlot.getReservedCount() <= 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invalid original slot reservation count");
        if (bookingRepository.existsByDonorProfileIdAndDonationBookingDateAndDonationBookingTimeAndDonationBookingStatusNotAndIdNot(
                donorId, date, time, "CANCELLED", bookingId))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Donor already has a booking at this date and time");

        newSlot.setReservedCount(newSlot.getReservedCount() + 1);
        oldSlot.setReservedCount(oldSlot.getReservedCount() - 1);
        slotRepository.save(newSlot); slotRepository.save(oldSlot);
        booking.setDonationSlot(newSlot); booking.setDonationBookingDate(date); booking.setDonationBookingTime(time);
        booking.setDonationBookingStatus("PENDING");
        DonationBooking saved = bookingRepository.saveAndFlush(booking);
        auditLogService.saveAuditLog("DONATION_BOOKING_RESCHEDULED", email,
                "Booking ID " + bookingId + " moved from slot " + oldSlotId + " to " + newSlotId);
        bookingOutboxService.saveEvent(bookingId, email, "BOOKING_RESCHEDULED", "Donation Booking Rescheduled",
                "Your appointment is now on " + date + " at " + time + " UTC (Bahrain is UTC+3), pending hospital confirmation.");
        return DonationBookingResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<DonationBookingResponse> getHospitalBookings(Long hospitalId) {
        return bookingRepository.findByDonationSlot_HospitalId(hospitalId).stream()
                .map(DonationBookingResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public java.util.Map<String, Long> getHospitalSummary(Long hospitalId) {
        java.util.Map<String, Long> counts = new java.util.HashMap<>();
        for (Object[] row : bookingRepository.countHospitalBookingsByStatus(hospitalId))
            counts.put((String) row[0], ((Number) row[1]).longValue());
        return java.util.Map.of("pendingBookings", counts.getOrDefault("PENDING", 0L),
                "confirmedBookings", counts.getOrDefault("CONFIRMED", 0L),
                "completedDonations", counts.getOrDefault("COMPLETED", 0L));
    }

    @PersistenceContext
    private EntityManager entityManager;
    private final DonationBookingRepository bookingRepository;
    private final DonationSlotRepository slotRepository;
    private final AuditLogService auditLogService;
    private final BookingOutboxService bookingOutboxService;

    public DonationBookingService(DonationBookingRepository bookingRepository,
            DonationSlotRepository slotRepository, AuditLogService auditLogService,
            BookingOutboxService bookingOutboxService) {

        this.bookingRepository = bookingRepository;
        this.slotRepository = slotRepository;
        this.auditLogService = auditLogService;
        this.bookingOutboxService = bookingOutboxService;
    }
    @Transactional
    public DonationBookingResponse createBooking(DonationBookingRequest request,
            Long verifiedDonorProfileId, Long verifiedHospitalId,
            LocalDate verifiedBloodNeededDate, String verifiedBloodRequestStatus, String performedByEmail) {

        if (request == null
                || request.getDonationSlotId() == null
                || request.getBloodRequestId() == null
                || verifiedDonorProfileId == null
                || verifiedHospitalId == null
                || verifiedBloodNeededDate == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Required booking information is missing");
        }

        validateAuditEmail(performedByEmail);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(
                        """
                        SELECT hospital_id, needed_date, status
                        FROM blood_request
                        WHERE id = :requestId
                        FOR UPDATE
                        """)
                .setParameter("requestId", request.getBloodRequestId()).getResultList();

        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Blood request not found");
        }

        Object[] row = rows.get(0);
        Long lockedHospitalId = ((Number) row[0]).longValue();

        LocalDate lockedNeededDate = row[1] instanceof LocalDate date
                        ? date
                        : ((java.sql.Date) row[1]).toLocalDate();

        String lockedStatus = (String) row[2];

        if (!"OPEN".equals(lockedStatus)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Blood request is not open");
        }

        if (!lockedHospitalId.equals(verifiedHospitalId)
                || !lockedNeededDate.equals(verifiedBloodNeededDate)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Blood request changed during booking");
        }

        if (request.getDonorProfileId() == null
                || !request.getDonorProfileId()
                .equals(verifiedDonorProfileId)) {

            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You cannot book for another donor");
        }

        entityManager.createNativeQuery(
                        "SELECT pg_advisory_xact_lock(:donorId)")
                .setParameter("donorId", verifiedDonorProfileId).getSingleResult();


        DonationSlot slot = slotRepository
                .findByIdForUpdate(request.getDonationSlotId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Donation slot not found"));

        LocalDate bookingDate = request.getDonationBookingDate();

        LocalTime bookingTime = request.getDonationBookingTime();

        if (bookingDate == null || bookingTime == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Booking date and time are required");
        }

        if (bookingDate.isBefore(LocalDate.now())
                || (bookingDate.equals(LocalDate.now())
                && !bookingTime.isAfter(LocalTime.now()))) {

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Booking date and time must be in the future");
        }

        if (bookingDate.isAfter(lockedNeededDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Booking date cannot be after blood needed date");
        }

        if (!verifiedHospitalId.equals(slot.getHospitalId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Slot and blood request must belong to the same hospital");
        }

        if (!bookingDate.equals(slot.getSlotDate())
                || bookingTime.isBefore(slot.getStartTime())
                || !bookingTime.isBefore(slot.getEndTime())) {

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Booking must match the slot date and time");
        }

        if (!"OPEN".equals(slot.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Donation slot is not open");
        }

        boolean alreadyBooked = bookingRepository
                .existsByDonorProfileIdAndDonationBookingDateAndDonationBookingTimeAndDonationBookingStatusNot(
                        verifiedDonorProfileId, bookingDate, bookingTime, "CANCELLED");

        if (alreadyBooked) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Donor already has a booking at this date and time");
        }

        if (slot.getReservedCount() >= slot.getCapacity()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Donation slot is full");
        }

        DonationBooking booking = new DonationBooking();

        booking.setDonorProfileId(verifiedDonorProfileId);
        booking.setBloodRequestId(request.getBloodRequestId());
        booking.setDonationSlot(slot);
        booking.setDonationBookingDate(bookingDate);
        booking.setDonationBookingTime(bookingTime);
        booking.setDonationBookingStatus("PENDING");

        slot.setReservedCount(slot.getReservedCount() + 1);
        slotRepository.save(slot);

        DonationBooking savedBooking = bookingRepository.saveAndFlush(booking);

        auditLogService.saveAuditLog(
                "DONATION_BOOKING_CREATED",
                performedByEmail,
                "Booking ID " + savedBooking.getId()
                        + " created for blood request "
                        + savedBooking.getBloodRequestId()
                        + " by donor profile " + verifiedDonorProfileId);

        bookingOutboxService.saveEvent(savedBooking.getId(), performedByEmail,
                "BOOKING_CREATED",
                "Donation Booking Created",
                "Your blood donation booking has been created successfully.");

        return DonationBookingResponse.from(savedBooking);
    }

    @Transactional
    public DonationBookingResponse cancelBooking(Long bookingId, Long verifiedDonorProfileId, String performedByEmail) {

        if (bookingId == null || verifiedDonorProfileId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Booking ID and donor profile are required");
        }

        validateAuditEmail(performedByEmail);

        DonationBooking existingBooking = bookingRepository
                .findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Booking not found"));

        if (!verifiedDonorProfileId.equals(
                existingBooking.getDonorProfileId())) {

            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You cannot cancel another donor's booking");
        }

        Long bloodRequestId = existingBooking.getBloodRequestId();
        Long slotId = existingBooking.getDonationSlot().getId();
        List<?> requestRows = entityManager.createNativeQuery(
                        """
                        SELECT id
                        FROM blood_request
                        WHERE id = :requestId
                        FOR UPDATE
                        """)
                .setParameter("requestId", bloodRequestId).getResultList();

        if (requestRows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Blood request not found");
        }

        DonationSlot slot = slotRepository.findByIdForUpdate(slotId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Donation slot not found"));

        DonationBooking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Booking not found"));

        // The earlier lookup may be cached; reload state after waiting for the lock.
        entityManager.refresh(booking, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);

        if (!verifiedDonorProfileId.equals(booking.getDonorProfileId())) {

            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You cannot cancel another donor's booking");
        }

        if (!bloodRequestId.equals(booking.getBloodRequestId())
                || !slotId.equals(booking.getDonationSlot().getId())) {

            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Booking changed during processing");
        }

        String currentStatus = booking.getDonationBookingStatus();

        if ("CANCELLED".equals(currentStatus)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Booking is already cancelled");
        }

        if ("COMPLETED".equals(currentStatus)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Completed bookings cannot be cancelled");
        }

        if (!"PENDING".equals(currentStatus)
                && !"CONFIRMED".equals(currentStatus)) {

            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Invalid booking status for cancellation");
        }

        if (slot.getReservedCount() <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Invalid slot reservation count");
        }

        booking.setDonationBookingStatus("CANCELLED");
        slot.setReservedCount(slot.getReservedCount() - 1);

        slotRepository.save(slot);
        DonationBooking savedBooking = bookingRepository.saveAndFlush(booking);

        auditLogService.saveAuditLog(
                "DONATION_BOOKING_CANCELLED",
                performedByEmail,
                "Booking ID " + bookingId
                        + " cancelled by donor profile "
                        + verifiedDonorProfileId
                        + ". Previous status: " + currentStatus);

        bookingOutboxService.saveEvent(savedBooking.getId(), performedByEmail,
                "BOOKING_CANCELLED",
                "Donation Booking Cancelled",
                "Your blood donation booking has been cancelled.");

        return DonationBookingResponse.from(savedBooking);
    }




    @Transactional
    public DonationBookingResponse updateBookingStatus(Long bookingId,
            String newStatus, Long verifiedHospitalId,
            String performedByEmail, Long expectedDonorProfileId, String donorEmail) {

        if (bookingId == null
                || verifiedHospitalId == null
                || newStatus == null
                || expectedDonorProfileId == null
                || donorEmail == null
                || donorEmail.isBlank()) {

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Required information is missing");
        }

        validateAuditEmail(performedByEmail);

        DonationBooking existingBooking = bookingRepository
                .findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Booking not found"));

        Long bloodRequestId = existingBooking.getBloodRequestId();

        Long slotId = existingBooking.getDonationSlot().getId();


        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(
                        """
                        SELECT hospital_id, units_needed, status
                        FROM blood_request
                        WHERE id = :requestId
                        FOR UPDATE
                        """)
                .setParameter("requestId", bloodRequestId)
                .getResultList();

        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Blood request not found");
        }

        Object[] row = rows.get(0);

        Long requestHospitalId = ((Number) row[0]).longValue();

        int requiredBloodUnits = ((Number) row[1]).intValue();

        String bloodRequestStatus = (String) row[2];

        if (!verifiedHospitalId.equals(requestHospitalId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You cannot update another hospital's booking");
        }

        DonationSlot slot = slotRepository.findByIdForUpdate(slotId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Donation slot not found"));


        DonationBooking booking = bookingRepository
                .findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Booking not found"));

        // The earlier lookup may be cached; reload state after waiting for the lock.
        entityManager.refresh(booking, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);

        if (!bloodRequestId.equals(booking.getBloodRequestId())
                || !slotId.equals(booking.getDonationSlot().getId())) {

            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Booking changed during processing");
        }

        if (!verifiedHospitalId.equals(slot.getHospitalId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Booking does not belong to your hospital");
        }

        if (!expectedDonorProfileId.equals(
                booking.getDonorProfileId())) {

            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Booking donor changed during processing");
        }

        String currentStatus = booking.getDonationBookingStatus();

        boolean validTransition = switch (currentStatus) {
            case "PENDING" -> newStatus.equals("CONFIRMED")
                            || newStatus.equals("CANCELLED");

            case "CONFIRMED" -> newStatus.equals("COMPLETED")
                            || newStatus.equals("CANCELLED");

            default -> false;
        };

        if (!validTransition) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Invalid booking status transition");
        }


        if ("CANCELLED".equals(newStatus)) {

            if (slot.getReservedCount() <= 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Invalid slot reservation count");
            }

            slot.setReservedCount(slot.getReservedCount() - 1);
            slotRepository.save(slot);
        }

        booking.setDonationBookingStatus(newStatus);

        DonationBooking savedBooking = bookingRepository.saveAndFlush(booking);
        if ("COMPLETED".equals(newStatus)) {

            long completedDonations = bookingRepository
                    .countByBloodRequestIdAndDonationBookingStatus(
                            bloodRequestId, "COMPLETED");

            if (completedDonations >= requiredBloodUnits
                    && "OPEN".equals(bloodRequestStatus)) {

                int updatedRows = entityManager.createNativeQuery(
                                """
                                UPDATE blood_request
                                SET status = 'COMPLETED',
                                    updated_at = CURRENT_TIMESTAMP
                                WHERE id = :requestId
                                  AND status = 'OPEN'
                                """)
                        .setParameter("requestId", bloodRequestId)
                        .executeUpdate();

                if (updatedRows == 1) {

                    auditLogService.saveAuditLog(
                            "BLOOD_REQUEST_COMPLETED",
                            performedByEmail,
                            "Blood request ID " + bloodRequestId
                                    + " automatically completed after "
                                    + completedDonations
                                    + " completed donations");
                }
            }
        }
        auditLogService.saveAuditLog(
                "DONATION_BOOKING_" + newStatus,
                performedByEmail,
                "Booking ID " + bookingId
                        + " changed from " + currentStatus
                        + " to " + newStatus
                        + " for blood request " + bloodRequestId);


        String notificationType;
        String notificationTitle;
        String notificationMessage;

        switch (newStatus) {

            case "CONFIRMED" -> {
                notificationType = "BOOKING_CONFIRMED";
                notificationTitle = "Donation Booking Confirmed";
                notificationMessage = "Your blood donation booking has been confirmed.";
            }

            case "COMPLETED" -> {
                notificationType = "BOOKING_COMPLETED";
                notificationTitle = "Blood Donation Completed";
                notificationMessage = "Thank you! Your blood donation has been completed.";
            }

            case "CANCELLED" -> {
                notificationType = "BOOKING_CANCELLED";
                notificationTitle = "Donation Booking Cancelled";
                notificationMessage = "Your blood donation booking has been cancelled by the hospital.";
            }

            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unsupported notification status");
        }

        bookingOutboxService.saveEvent(savedBooking.getId(),
                donorEmail, notificationType, notificationTitle, notificationMessage);

        return DonationBookingResponse.from(savedBooking);
    }
    @Transactional(readOnly = true)
    public List<DonationBookingResponse> getBookingsByDonor(Long verifiedDonorProfileId) {

        return bookingRepository
                .findByDonorProfileId(verifiedDonorProfileId)
                .stream()
                .map(DonationBookingResponse::from)
                .toList();
    }
    @Transactional(readOnly = true)
    public List<DonationBookingResponse> getBookingsByBloodRequest(Long bloodRequestId) {

        return bookingRepository
                .findByBloodRequestId(bloodRequestId)
                .stream()
                .map(DonationBookingResponse::from)
                .toList();
    }
    private void validateAuditEmail(String performedByEmail) {

        if (performedByEmail == null || performedByEmail.isBlank()) {

            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Authenticated user email is unavailable");
        }
    }

    @Transactional(readOnly = true)
    public Long getDonorProfileIdForStaffNotification(Long bookingId, Long verifiedHospitalId) {

        if (bookingId == null || verifiedHospitalId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Booking ID and hospital ID are required");
        }

        DonationBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Booking not found"));

        DonationSlot slot = booking.getDonationSlot();

        if (slot == null || !verifiedHospitalId.equals(slot.getHospitalId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Booking does not belong to your hospital");
        }
        List<?> rows = entityManager.createNativeQuery(
                        """
                        SELECT hospital_id
                        FROM blood_request
                        WHERE id = :requestId
                        """)
                .setParameter(
                        "requestId",
                        booking.getBloodRequestId()).getResultList();

        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Blood request not found");
        }

        Long requestHospitalId = ((Number) rows.get(0)).longValue();

        if (!verifiedHospitalId.equals(requestHospitalId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Blood request does not belong to your hospital");
        }

        if (booking.getDonorProfileId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Booking donor profile is missing");
        }

        return booking.getDonorProfileId();
    }

}
