package com.app.med_support.service;

import com.app.med_support.repository.DonationReminderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.app.med_support.model.DonationReminder;
import com.app.med_support.model.DonationBooking;
import com.app.med_support.repository.DonationBookingRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DonationReminderService {

    private final DonationReminderRepository donationReminderRepository;
    private final DonationBookingRepository donationBookingRepository;
    private static final Logger logger = LoggerFactory.getLogger(DonationReminderService.class);

    public DonationReminderService(
            DonationReminderRepository donationReminderRepository,
            DonationBookingRepository donationBookingRepository) {

        this.donationReminderRepository = donationReminderRepository;
        this.donationBookingRepository = donationBookingRepository;

    }

    public DonationReminder createReminder(Long bookingId,
            LocalDateTime reminderTime, Long userId) {

        // Log the start of reminder creation
        logger.info("Reminder creation started. Booking ID: {}", bookingId);


        DonationBooking booking = donationBookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Booking not found"));


        if (!booking.getDonorProfile().getUser().getId().equals(userId)) {

            throw new AccessDeniedException("You cannot create a reminder for another donor's booking");
        }


        if ("CANCELLED".equalsIgnoreCase(booking.getDonationBookingStatus())) {

            throw new IllegalArgumentException(
                    "Cannot create a reminder for a cancelled booking");
        }


        LocalDateTime bookingDateTime = LocalDateTime.of(booking.getDonationBookingDate(),
                booking.getDonationBookingTime());


        if (!reminderTime.isAfter(LocalDateTime.now()) || !reminderTime.isBefore(bookingDateTime)) {

            throw new IllegalArgumentException("Invalid reminder time");
        }


        DonationReminder reminder = new DonationReminder();
        reminder.setDonationBooking(booking);
        reminder.setReminderTime(reminderTime);


        DonationReminder savedReminder = donationReminderRepository.save(reminder);

        // Log the saved reminder
        logger.info("Donation reminder saved. Reminder ID: {}, Booking ID: {}",
                savedReminder.getId(), bookingId);

        return savedReminder;
    }

    public List<DonationReminder> getPendingReminders() {

            // Log when fetching pending reminders
            logger.debug("Fetching pending donation reminders");

            return donationReminderRepository.findByStatusAndReminderTimeLessThanEqual(
                    "PENDING", LocalDateTime.now());

    }
}
