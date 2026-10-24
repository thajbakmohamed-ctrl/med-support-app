package com.app.med_support.repository;

import com.app.med_support.model.DonationSlot;
import com.app.med_support.model.Hospital;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
public class DonationSlotRepositoryTest {

    @Autowired
    private DonationSlotRepository donationSlotRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Test
    void testSaveAndFindDonationSlot() {

        Hospital hospital = new Hospital();
        hospital.setHospitalName("Test Hospital");

        hospital.setHospitalLocation("Manama");
        hospital.setHospitalPhone("39999999");
        hospital.setHospitalDescription("Hospital for testing donation slots");
        hospital.setHospitalActive(true);

        Hospital savedHospital = hospitalRepository.save(hospital);

        DonationSlot slot = new DonationSlot();

        slot.setHospital(savedHospital);

        slot.setSlotDate(LocalDate.now().plusDays(7));

        slot.setStartTime(LocalTime.of(10, 0));
        slot.setEndTime(LocalTime.of(11, 0));

        slot.setCapacity(2);

        slot.setReservedCount(0);

        slot.setStatus("OPEN");

        DonationSlot savedSlot = donationSlotRepository.save(slot);

        DonationSlot foundSlot = donationSlotRepository.findById(savedSlot.getId())
                .orElseThrow();
        // Check that the saved slot has an ID
        assertNotNull(savedSlot.getId());

        assertEquals(2, foundSlot.getCapacity());

        assertEquals(0, foundSlot.getReservedCount());

        assertEquals("OPEN", foundSlot.getStatus());

        assertEquals(savedHospital.getId(), foundSlot.getHospital().getId());

    }

}