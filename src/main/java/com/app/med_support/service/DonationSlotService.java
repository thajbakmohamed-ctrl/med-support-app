package com.app.med_support.service;

import com.app.med_support.model.Hospital;
import com.app.med_support.repository.DonationSlotRepository;
import com.app.med_support.repository.HospitalRepository;
import com.app.med_support.response.DonationSlotResponse;
import org.springframework.stereotype.Service;
import com.app.med_support.model.DonationSlot;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class DonationSlotService {

    private final DonationSlotRepository donationSlotRepository;
    private final HospitalRepository hospitalRepository;

    public DonationSlotService(
            DonationSlotRepository donationSlotRepository,
            HospitalRepository hospitalRepository) {

        this.donationSlotRepository = donationSlotRepository;
        this.hospitalRepository = hospitalRepository;
    }

    @Transactional
    public void reserveSlot(Long slotId) {

        //find the slot and lock it during this transaction
        DonationSlot slot = donationSlotRepository.findByIdForUpdate(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Donation slot not found"));

        if (!"OPEN".equals(slot.getStatus())) {
            throw new IllegalStateException("Donation slot is not open");
        }

        if (slot.getReservedCount() >= slot.getCapacity()) {
            throw new IllegalStateException("Donation slot is full");
        }

        slot.setReservedCount(slot.getReservedCount() + 1);

    }

    public DonationSlot getDonationSlotById(Long slotId) {
        return donationSlotRepository.findById(slotId).orElse(null);
    }

    public List<DonationSlot> getAllDonationSlots() {
        return donationSlotRepository.findAll();
    }
    @Transactional
    public DonationSlot createDonationSlot(DonationSlot slot) {

        if (slot.getCapacity() == null || slot.getCapacity() < 1) {
            return null;
        }

        if (slot.getSlotDate() == null ||
                slot.getSlotDate().isBefore(LocalDate.now())) {
            return null;
        }

        if (slot.getStartTime() == null || slot.getEndTime() == null ||
                !slot.getEndTime().isAfter(slot.getStartTime())) {
            return null;
        }

        if (slot.getSlotDate().equals(LocalDate.now())
                && !slot.getStartTime().isAfter(LocalTime.now())) {
            return null;
        }

        // Check if the hospital ID is provided
        if (slot.getHospital() == null ||
                slot.getHospital().getId() == null) {
            return null;
        }


        Long hospitalId = slot.getHospital().getId();

        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElse(null);


        if (hospital == null) {
            return null;
        }

        slot.setHospital(hospital);

        slot.setReservedCount(0);
        slot.setStatus("OPEN");

        return donationSlotRepository.save(slot);
    }

    public DonationSlotResponse toResponse(DonationSlot slot) {

        DonationSlotResponse response = new DonationSlotResponse();

        response.setId(slot.getId());
        response.setHospitalId(slot.getHospital().getId());
        response.setSlotDate(slot.getSlotDate());
        response.setStartTime(slot.getStartTime());
        response.setEndTime(slot.getEndTime());
        response.setCapacity(slot.getCapacity());
        response.setReservedCount(slot.getReservedCount());
        response.setStatus(slot.getStatus());

        return response;
    }
    public List<DonationSlot> getAvailableSlotsByHospitalId(Long hospitalId) {

        return donationSlotRepository.findAvailableSlotsByHospitalId(hospitalId)
                .stream().filter(slot -> slot.getSlotDate().isAfter(LocalDate.now())
                        || (slot.getSlotDate().equals(LocalDate.now()) && slot.getStartTime()
                        .isAfter(LocalTime.now()))).toList();
    }

    @Transactional
    public DonationSlot updateDonationSlot(Long slotId, Long hospitalId, DonationSlot updatedSlot) {

        DonationSlot slot = donationSlotRepository.findByIdForUpdate(slotId).orElse(null);

        if (slot == null) {
            return null;
        }

        if (!slot.getHospital().getId().equals(hospitalId)) {
            return null;
        }

        if (updatedSlot.getCapacity() == null || updatedSlot.getCapacity() < 1) {
            return null;
        }

        if (updatedSlot.getCapacity() < slot.getReservedCount()) {
            return null;
        }

        slot.setCapacity(updatedSlot.getCapacity());

        return slot;
    }
    @Transactional
    public DonationSlot closeDonationSlot(Long slotId, Long hospitalId) {

        DonationSlot slot = donationSlotRepository.findByIdForUpdate(slotId).orElse(null);

        if (slot == null) {
            return null;
        }

        if (!slot.getHospital().getId().equals(hospitalId)) {
            return null;
        }

        if ("CLOSED".equals(slot.getStatus())) {
            return slot;
        }

        slot.setStatus("CLOSED");

        return donationSlotRepository.save(slot);
    }


}
