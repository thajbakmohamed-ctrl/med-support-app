package com.app.med_support.service;

import com.app.med_support.repository.DonationReminderRepository;
import com.app.med_support.repository.NotificationRepository;
import com.app.med_support.model.*;
import com.app.med_support.repository.*;

import org.junit.jupiter.api.BeforeEach;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;


import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.timeout;
import org.mockito.ArgumentMatchers;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;



@SpringBootTest
@ActiveProfiles("test")
public class DonationReminderConcurrencyTest {

    @Autowired
    private DonationReminderTaskService reminderTaskService;

    @Autowired
    private DonationReminderRepository reminderRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DonorProfileRepository donorProfileRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private BloodRequestRepository bloodRequestRepository;

    @Autowired
    private DonationSlotRepository donationSlotRepository;

    @Autowired
    private DonationBookingRepository donationBookingRepository;

    @Autowired
    private ExecutorService reminderExecutor;

    @MockitoBean
    private EmailService emailService;

    private Long testReminderId;

    @BeforeEach
    void setUp() {

        Hospital hospital = new Hospital();

        hospital.setHospitalName("Reminder Test Hospital");
        hospital.setHospitalLocation("Manama");
        hospital.setHospitalPhone("17000002");
        hospital.setHospitalDescription("Hospital for reminder concurrency testing");
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

        slot = donationSlotRepository.save(slot);


        BloodRequest bloodRequest = new BloodRequest();

        bloodRequest.setHospital(hospital);
        bloodRequest.setRequiredBloodType("O+");
        bloodRequest.setRequiredBloodUnits(2);
        bloodRequest.setBloodRequestUrgency("LOW");
        bloodRequest.setBloodRequestStatus("OPEN");
        bloodRequest.setBloodNeededDate(LocalDate.now().plusDays(10));
        bloodRequest.setBloodRequestDescription("Reminder concurrency test");

        bloodRequest = bloodRequestRepository.save(bloodRequest);


        User user = new User();

        user.setName("Reminder Test Donor");
        user.setEmail("reminder-test-" + System.nanoTime() + "@test.com");
        user.setHashedPassword("test-password-hash");
        user.setPhoneNumber("39990001");
        user.setRole("DONOR");
        user.setStatus("ACTIVE");
        user.setEmailVerified(true);


        user = userRepository.save(user);



        DonorProfile donor = new DonorProfile();

        donor.setUser(user);
        donor.setBloodType("O+");
        donor.setDateOfBirth(LocalDate.of(2000, 1, 1));
        donor.setGender("Female");
        donor.setWeightKg(60.0);
        donor.setTotalDonations(0);
        donor.setCprImage("test-cpr.png");
        donor.setAvailableForBloodDonation(true);


        donor = donorProfileRepository.save(donor);


        DonationBooking booking = new DonationBooking();

        booking.setDonorProfile(donor);
        booking.setBloodRequest(bloodRequest);
        booking.setDonationSlot(slot);


        booking.setDonationBookingDate(slot.getSlotDate());
        booking.setDonationBookingTime(slot.getStartTime());


        booking.setDonationBookingStatus("PENDING");


        booking = donationBookingRepository.save(booking);

        DonationReminder reminder = new DonationReminder();

        reminder.setDonationBooking(booking);

        reminder.setReminderTime(LocalDateTime.now());

        reminder.setStatus("PENDING");

        reminder = reminderRepository.save(reminder);

        testReminderId = reminder.getId();}

        @Test
        void shouldSendOnlyOneNotificationWhenReminderSubmittedMultipleTimes()
        throws Exception {


            DonationReminder reminder = reminderRepository.findById(testReminderId).orElseThrow();

            for (int i = 0; i < 5; i++) {
                reminderTaskService.submitReminder(reminder);
            }

            ThreadPoolExecutor threadPool = (ThreadPoolExecutor) reminderExecutor;

            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);

            DonationReminder updatedReminder;

            do {
                updatedReminder = reminderRepository.findById(testReminderId).orElseThrow();


                boolean allTasksFinished = threadPool.getActiveCount() == 0
                                && threadPool.getQueue().isEmpty();

                if (allTasksFinished) {
                    break;
                }

                Thread.sleep(100);

            } while (System.nanoTime() < deadline);


            assertEquals(0, threadPool.getActiveCount());
            assertTrue(threadPool.getQueue().isEmpty());


            updatedReminder = reminderRepository.findById(testReminderId).orElseThrow();

            Long bookingId = updatedReminder.getDonationBooking().getId();

            long notificationsCount = notificationRepository.countByDonationBookingId(bookingId);

            assertEquals("SENT", updatedReminder.getStatus());
            assertNotNull(updatedReminder.getSentAt());
            assertEquals(1, notificationsCount);


            verify(emailService, timeout(5000).times(1))
                    .sendBookingReminderEmail(ArgumentMatchers.anyString(),
                            ArgumentMatchers.eq(bookingId));


            System.out.println("\n========== REMINDER CONCURRENCY TEST ==========");
            System.out.println("Reminder Submissions: 5");
            System.out.println("Final Reminder Status: " + updatedReminder.getStatus());
            System.out.println("Notifications Created: " + notificationsCount);
            System.out.println("RESULT: PASS - No Duplicate Notifications");
            System.out.println("================================================\n");
        }
    }



