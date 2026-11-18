
package com.medsupport.booking_service.service;

import com.medsupport.booking_service.model.DonationSlot;
import com.medsupport.booking_service.repository.DonationSlotRepository;
import com.medsupport.booking_service.response.DonationSlotResponse;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class DonationSlotService {

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    public record SlotLifecycle(DonationSlotResponse slot,
            long completedBookings, long cancelledBookings, long pendingBookings,
            long confirmedBookings, long nonCancelledBookings) {}

    @Transactional(readOnly = true)
    public List<SlotLifecycle> getLifecycle(Long hospitalId, Long donorProfileId) {
        if (hospitalId == null && donorProfileId == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A verified owner is required");
        List<DonationSlot> slots = hospitalId != null ? getSlotsByHospital(hospitalId)
                : entityManager.createQuery("SELECT DISTINCT s FROM DonationSlot s, DonationBooking b "
                        + "WHERE b.donationSlot=s AND b.donorProfileId=:donor", DonationSlot.class)
                        .setParameter("donor", donorProfileId).getResultList();
        if (slots.isEmpty()) return List.of();
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("""
                SELECT donation_slot_id,
                  COUNT(*) FILTER (WHERE donation_booking_status='COMPLETED'),
                  COUNT(*) FILTER (WHERE donation_booking_status='CANCELLED'),
                  COUNT(*) FILTER (WHERE donation_booking_status='PENDING'),
                  COUNT(*) FILTER (WHERE donation_booking_status='CONFIRMED'),
                  COUNT(*) FILTER (WHERE donation_booking_status<>'CANCELLED')
                FROM donation_booking WHERE donation_slot_id IN (:ids) GROUP BY donation_slot_id
                """).setParameter("ids", slots.stream().map(DonationSlot::getId).toList()).getResultList();
        java.util.Map<Long, long[]> counts = new java.util.HashMap<>();
        for (Object[] row : rows) counts.put(((Number)row[0]).longValue(), new long[]{
                ((Number)row[1]).longValue(), ((Number)row[2]).longValue(), ((Number)row[3]).longValue(),
                ((Number)row[4]).longValue(), ((Number)row[5]).longValue()});
        return slots.stream().map(slot -> {
            long[] count = counts.getOrDefault(slot.getId(), new long[5]);
            return new SlotLifecycle(DonationSlotResponse.from(slot),
                    count[0], count[1], count[2], count[3], count[4]);
        }).toList();
    }

    private final DonationSlotRepository slotRepository;

    public DonationSlotService(DonationSlotRepository slotRepository) {
        this.slotRepository = slotRepository;
    }

    @Transactional
    public DonationSlot createSlot(Long verifiedHospitalId, LocalDate slotDate,
            LocalTime startTime, LocalTime endTime, Integer capacity) {

        if (verifiedHospitalId == null || verifiedHospitalId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Valid hospital ID is required");
        }

        if (slotDate == null || slotDate.isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Slot date cannot be in the past");
        }

        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "End time must be after start time");
        }

        if (capacity == null || capacity < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Capacity must be at least 1");
        }

        if (slotDate.equals(LocalDate.now()) && !startTime.isAfter(LocalTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Slot start time must be in the future");
        }

        DonationSlot slot = new DonationSlot();
        slot.setHospitalId(verifiedHospitalId);
        slot.setSlotDate(slotDate);
        slot.setStartTime(startTime);
        slot.setEndTime(endTime);
        slot.setCapacity(capacity);
        slot.setReservedCount(0);
        slot.setStatus("OPEN");

        return slotRepository.save(slot);
    }

    @Transactional
    public DonationSlot updateCapacity(Long slotId, Integer newCapacity,
            Long verifiedHospitalId) {

        DonationSlot slot = slotRepository.findByIdForUpdate(slotId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Donation slot not found"));

        if (!slot.getHospitalId().equals(verifiedHospitalId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You cannot modify another hospital's slot");
        }

        if (newCapacity == null || newCapacity < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Capacity must be at least 1");
        }

        if (newCapacity < slot.getReservedCount()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Capacity cannot be less than existing reservations");
        }

        slot.setCapacity(newCapacity);
        return slotRepository.save(slot);
    }

    @Transactional(readOnly = true)
    public List<DonationSlot> getSlotsByHospital(Long hospitalId) {

        if (hospitalId == null || hospitalId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Valid hospital ID is required");
        }

        return slotRepository.findByHospitalId(hospitalId);
    }

    @Transactional(readOnly = true)
    public List<DonationSlot> getAvailableSlotsForBloodRequest(
            Long verifiedHospitalId, LocalDate neededDate) {

        if (verifiedHospitalId == null || verifiedHospitalId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Valid hospital ID is required");
        }

        if (neededDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Blood request needed date is required");
        }

        LocalDate today = LocalDate.now();
        LocalTime currentTime = LocalTime.now();

        if (neededDate.isBefore(today)) {
            return List.of();
        }

        return slotRepository.findAvailableSlotsForBloodRequest(verifiedHospitalId,
                neededDate, today, currentTime);
    }
}
