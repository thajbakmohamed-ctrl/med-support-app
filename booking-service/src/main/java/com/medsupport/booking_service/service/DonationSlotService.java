
package com.medsupport.booking_service.service;

import com.medsupport.booking_service.model.DonationSlot;
import com.medsupport.booking_service.repository.DonationSlotRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class DonationSlotService {

    private final DonationSlotRepository slotRepository;

    public DonationSlotService(DonationSlotRepository slotRepository) {
        this.slotRepository = slotRepository;
    }

    @Transactional
    public DonationSlot createSlot(
            Long verifiedHospitalId,
            LocalDate slotDate,
            LocalTime startTime,
            LocalTime endTime,
            Integer capacity) {

        if (verifiedHospitalId == null || verifiedHospitalId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Valid hospital ID is required");
        }

        if (slotDate == null || slotDate.isBefore(LocalDate.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Slot date cannot be in the past");
        }

        if (startTime == null || endTime == null
                || !endTime.isAfter(startTime)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "End time must be after start time");
        }

        if (slotDate.equals(LocalDate.now())
                && !startTime.isAfter(LocalTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Slot start time must be in the future");
        }

        if (capacity == null || capacity < 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Capacity must be at least 1");
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
    public DonationSlot updateCapacity(
            Long slotId,
            Integer newCapacity,
            Long verifiedHospitalId) {

        DonationSlot slot = slotRepository.findByIdForUpdate(slotId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Donation slot not found"));

        if (!slot.getHospitalId().equals(verifiedHospitalId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You cannot modify another hospital's slot");
        }

        if (newCapacity == null || newCapacity < 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Capacity must be at least 1");
        }

        if (newCapacity < slot.getReservedCount()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Capacity cannot be less than existing reservations");
        }

        slot.setCapacity(newCapacity);
        return slotRepository.save(slot);
    }

    @Transactional(readOnly = true)
    public List<DonationSlot> getSlotsByHospital(Long hospitalId) {
        if (hospitalId == null || hospitalId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Valid hospital ID is required");
        }

        return slotRepository.findAll().stream()
                .filter(slot -> hospitalId.equals(slot.getHospitalId()))
                .toList();
    }
}
