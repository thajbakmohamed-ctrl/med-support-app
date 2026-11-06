
package com.app.med_support.controller;

import com.app.med_support.security.JWTUtils;
import com.app.med_support.model.*;
import com.app.med_support.repository.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalTime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
public class DonationBookingRestConcurrencyTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JWTUtils jwtUtils;

    @LocalServerPort
    private int port;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private DonationSlotRepository donationSlotRepository;

    @Autowired
    private BloodRequestRepository bloodRequestRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DonorProfileRepository donorProfileRepository;

    @Autowired
    private DonationBookingRepository donationBookingRepository;

    private Long testSlotId;
    private Long testBloodRequestId;

    private final List<String> donorTokens = new ArrayList<>();

    @BeforeEach
    void setUp() {

        donorTokens.clear();

        // Create a hospital for testing
        Hospital hospital = new Hospital();

        hospital.setHospitalName("REST Concurrency Test Hospital");
        hospital.setHospitalLocation("Manama");
        hospital.setHospitalPhone("17000003");
        hospital.setHospitalDescription("Hospital for REST concurrency testing");
        hospital.setHospitalActive(true);

        hospital = hospitalRepository.save(hospital);

        // Create a slot with capacity of two donors
        DonationSlot slot = new DonationSlot();

        slot.setHospital(hospital);
        slot.setSlotDate(LocalDate.now().plusDays(7));
        slot.setStartTime(LocalTime.of(9, 0));
        slot.setEndTime(LocalTime.of(11, 0));
        slot.setCapacity(2);
        slot.setReservedCount(0);
        slot.setStatus("OPEN");

        slot = donationSlotRepository.save(slot);

        testSlotId = slot.getId();

        // Create an open blood request
        BloodRequest bloodRequest = new BloodRequest();

        bloodRequest.setHospital(hospital);
        bloodRequest.setRequiredBloodType("O+");
        bloodRequest.setRequiredBloodUnits(2);
        bloodRequest.setBloodRequestUrgency("LOW");
        bloodRequest.setBloodRequestStatus("OPEN");
        bloodRequest.setBloodNeededDate(LocalDate.now().plusDays(10));
        bloodRequest.setBloodRequestDescription("REST concurrency test");

        bloodRequest = bloodRequestRepository.save(bloodRequest);

        testBloodRequestId = bloodRequest.getId();

        // Create five different donors
        for (int i = 1; i <= 5; i++) {

            User user = new User();

            user.setName("REST Test Donor " + i);
            user.setEmail(
                    "rest-donor-" + i + "-"
                            + System.nanoTime() + "@test.com"
            );
            user.setHashedPassword("test-password-hash");
            user.setPhoneNumber("3999000" + i);
            user.setRole("DONOR");
            user.setStatus("ACTIVE");
            user.setEmailVerified(true);

            user = userRepository.save(user);

            // Generate a JWT token for each donor
            String token = jwtUtils.generateJwtToken(user.getEmail());

            donorTokens.add(token);

            // Create a donor profile
            DonorProfile donor = new DonorProfile();

            donor.setUser(user);
            donor.setBloodType("O+");
            donor.setDateOfBirth(LocalDate.of(2000, 1, 1));
            donor.setGender("Female");
            donor.setWeightKg(60.0);
            donor.setTotalDonations(0);
            donor.setCprImage("test-cpr.png");
            donor.setAvailableForBloodDonation(true);

            donorProfileRepository.save(donor);
        }
    }

    @Test
    void shouldPreventOverbookingWithConcurrentRestRequests()
            throws Exception {

        // Create five threads
        ExecutorService executor = Executors.newFixedThreadPool(5);

        // Coordinate the start of all requests
        CountDownLatch ready = new CountDownLatch(5);
        CountDownLatch start = new CountDownLatch(1);

        // Count successful and rejected bookings
        AtomicInteger successfulBookings = new AtomicInteger(0);
        AtomicInteger rejectedBookings = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();

        try {

            // Each donor sends one HTTP booking request
            for (String token : donorTokens) {

                Future<?> future = executor.submit(() -> {

                    ready.countDown();
                    start.await();

                    // Prepare JWT authorization
                    HttpHeaders headers = new HttpHeaders();

                    headers.setBearerAuth(token);
                    headers.setContentType(MediaType.APPLICATION_JSON);

                    // Prepare booking request body
                    Map<String, Object> bookingData = new HashMap<>();

                    bookingData.put("bloodRequestId", testBloodRequestId);
                    bookingData.put("donationSlotId", testSlotId);
                    bookingData.put(
                            "donationBookingDate",
                            LocalDate.now().plusDays(7).toString()
                    );
                    bookingData.put("donationBookingTime", "09:00:00");

                    HttpEntity<Map<String, Object>> request =
                            new HttpEntity<>(bookingData, headers);

                    // Send a real HTTP POST request
                    ResponseEntity<String> response = restTemplate.exchange(
                            "http://localhost:" + port + "/api/donation-bookings",
                            HttpMethod.POST,
                            request,
                            String.class
                    );

                    int statusCode = response.getStatusCode().value();

                    // Count responses
                    if (statusCode == 201) {

                        successfulBookings.incrementAndGet();

                        System.out.println("BOOKING SUCCESS - HTTP 201");

                    } else if (statusCode == 409) {

                        rejectedBookings.incrementAndGet();

                        System.out.println("BOOKING REJECTED - HTTP 409");

                    } else {

                        throw new IllegalStateException(
                                "Unexpected HTTP status: " + statusCode
                                        + ", response: " + response.getBody()
                        );
                    }

                    return null;
                });

                futures.add(future);
            }

            // Wait until all threads are ready
            if (!ready.await(30, TimeUnit.SECONDS)) {
                throw new IllegalStateException(
                        "Not all booking threads became ready"
                );
            }

            // Release all threads to send requests
            start.countDown();

            // Wait for all HTTP requests to finish
            for (Future<?> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }

        } finally {

            // Release waiting threads if an error occurs
            start.countDown();

            // Stop the executor
            executor.shutdownNow();
        }

        // Read the final slot data from the database
        DonationSlot updatedSlot = donationSlotRepository
                .findById(testSlotId)
                .orElseThrow();

        // Count bookings saved for this specific slot
        long savedBookings = donationBookingRepository.findAll()
                .stream()
                .filter(booking ->
                        booking.getDonationSlot() != null
                                && testSlotId.equals(
                                booking.getDonationSlot().getId()
                        )
                )
                .count();

        // Print test results
        System.out.println("\n========== REST CONCURRENCY TEST ==========");

        System.out.println("Total Booking Attempts: " + donorTokens.size());
        System.out.println("Successful Bookings: " + successfulBookings.get());
        System.out.println("Rejected Bookings: " + rejectedBookings.get());
        System.out.println("Slot Capacity: " + updatedSlot.getCapacity());
        System.out.println("Final Reserved Count: " + updatedSlot.getReservedCount());
        System.out.println("Saved Bookings in Database: " + savedBookings);

        // Verify that the slot was not overbooked
        assertEquals(5, donorTokens.size());
        assertEquals(2, successfulBookings.get());
        assertEquals(3, rejectedBookings.get());
        assertEquals(2, updatedSlot.getCapacity());
        assertEquals(2, updatedSlot.getReservedCount());
        assertEquals(2, savedBookings);

        System.out.println("RESULT: PASS - No REST Overbooking");
        System.out.println("===========================================\n");
    }
}
