package com.app.med_support.service;

import com.app.med_support.model.BloodRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import com.app.med_support.repository.DonationSlotRepository;
import com.app.med_support.repository.DonationBookingRepository;
import com.app.med_support.repository.BloodRequestRepository;
import com.app.med_support.repository.DonorProfileRepository;
import com.app.med_support.model.DonationSlot;
import com.app.med_support.model.Hospital;
import com.app.med_support.repository.HospitalRepository;
import com.app.med_support.model.User;
import com.app.med_support.model.DonorProfile;
import com.app.med_support.repository.UserRepository;
import com.app.med_support.model.DonationBooking;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;

import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
public class DonationBookingConcurrencyTest {

    @Autowired
    private DonationBookingService donationBookingService;

    @Autowired
    private DonationSlotRepository donationSlotRepository;

    @Autowired
    private DonationBookingRepository donationBookingRepository;

    @Autowired
    private BloodRequestRepository bloodRequestRepository;

    @Autowired
    private DonorProfileRepository donorProfileRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    private DonationSlot testSlot;

    private BloodRequest testBloodRequest;

    @Autowired
    private UserRepository userRepository;

    private List<DonorProfile> testDonors;

    @BeforeEach
    void setUp() {


        Hospital hospital = new Hospital();

        hospital.setHospitalName("Test Hospital");
        hospital.setHospitalLocation("Manama");
        hospital.setHospitalPhone("17000001");
        hospital.setHospitalDescription("Hospital for concurrency testing");
        hospital.setHospitalActive(true);

        hospital = hospitalRepository.save(hospital);
        DonationSlot slot = new DonationSlot();

        slot.setHospital(hospital);
        slot.setSlotDate(LocalDate.now().plusDays(7));
        slot.setStartTime(LocalTime.of(9, 0));
        slot.setEndTime(LocalTime.of(11, 0));
        slot.setCapacity(2);
        slot.setReservedCount(0);
        slot.setStatus("OPEN");

        testSlot = donationSlotRepository.save(slot);
        BloodRequest bloodRequest = new BloodRequest();

        bloodRequest.setHospital(hospital);
        bloodRequest.setRequiredBloodType("O+");
        bloodRequest.setRequiredBloodUnits(2);
        bloodRequest.setBloodRequestUrgency("LOW");
        bloodRequest.setBloodRequestStatus("OPEN");
        bloodRequest.setBloodNeededDate(LocalDate.now().plusDays(10));
        bloodRequest.setBloodRequestDescription("Concurrency test");

        testBloodRequest = bloodRequestRepository.save(bloodRequest);

        testDonors = new ArrayList<>();

        for (int i = 1; i <= 5; i++) {

            User user = new User();
            user.setName("Test Donor " + i);
            user.setEmail("concurrency-donor-" + i + "-" + System.nanoTime() + "@test.com");
            user.setHashedPassword("test-password-hash");
            user.setPhoneNumber("3999" + System.nanoTime() % 10000000);
            user.setRole("DONOR");
            user.setStatus("ACTIVE");
            user.setEmailVerified(true);

            User savedUser = userRepository.save(user);

            DonorProfile donor = new DonorProfile();
            donor.setUser(savedUser);
            donor.setBloodType("O+");
            donor.setDateOfBirth(LocalDate.of(2000, 1, 1));
            donor.setGender("Female");
            donor.setWeightKg(60.0);
            donor.setTotalDonations(0);
            donor.setCprImage("test-cpr.png");
            donor.setAvailableForBloodDonation(true);

            testDonors.add(donorProfileRepository.save(donor));
        }

    }
    @Test
    void shouldNotExceedSlotCapacityWithConcurrentBookings() throws Exception {

        ExecutorService executor = Executors.newFixedThreadPool(5);

        CountDownLatch start = new CountDownLatch(1);

        AtomicInteger successfulBookings = new AtomicInteger(0);


        AtomicInteger rejectedBookings = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();

        for (DonorProfile donor : testDonors) {

            Future<?> future = executor.submit(() -> {

                start.await();

                DonationBooking booking = new DonationBooking();

                DonorProfile donorReference = new DonorProfile();
                donorReference.setId(donor.getId());

                BloodRequest requestReference = new BloodRequest();
                requestReference.setId(testBloodRequest.getId());

                DonationSlot slotReference = new DonationSlot();
                slotReference.setId(testSlot.getId());

                booking.setDonorProfile(donorReference);
                booking.setBloodRequest(requestReference);
                booking.setDonationSlot(slotReference);

                booking.setDonationBookingDate(testSlot.getSlotDate());
                booking.setDonationBookingTime(testSlot.getStartTime());

                try {

                    DonationBooking result = donationBookingService.createDonationBooking(booking);

                    if (result != null) {
                        successfulBookings.incrementAndGet();

                        System.out.println("BOOKING SUCCESS - Donor ID: " + donor.getId());
                    }


                } catch (ResponseStatusException e) {

                    if (e.getStatusCode().value() == 409) {

                        rejectedBookings.incrementAndGet();

                        System.out.println("BOOKING REJECTED - Slot is full");

                    } else {
                        throw e;
                    }
                }

                return null;
            });

            futures.add(future);
        }

        start.countDown();

        try {
            for (Future<?> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        DonationSlot updatedSlot = donationSlotRepository.findById(testSlot.getId()).orElseThrow();


        long savedBookings = donationBookingRepository.findAll()
                .stream()
                .filter(booking ->
                        booking.getDonationSlot() != null &&
                                booking.getDonationSlot().getId().equals(testSlot.getId())
                )
                .count();


        System.out.println("\n========== CONCURRENCY TEST ==========");

        System.out.println("Total Booking Attempts: " + testDonors.size());
        System.out.println("Successful Bookings: " + successfulBookings.get());
        System.out.println("Rejected Bookings: " + rejectedBookings.get());
        System.out.println("Slot Capacity: " + updatedSlot.getCapacity());
        System.out.println("Final Reserved Count: " + updatedSlot.getReservedCount());
        System.out.println("Saved Bookings in Database: " + savedBookings);

        assertEquals(2, successfulBookings.get());
        assertEquals(3, rejectedBookings.get());
        assertEquals(2, updatedSlot.getReservedCount());
        assertEquals(2, updatedSlot.getCapacity());
        // Verify that only two bookings were saved in the database
        assertEquals(2, savedBookings);

        System.out.println("RESULT: PASS - No Overbooking");
        System.out.println("======================================\n");
    }


}
