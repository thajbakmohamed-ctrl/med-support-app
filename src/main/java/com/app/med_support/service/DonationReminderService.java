package com.app.med_support.service;

import com.app.med_support.repository.DonationReminderRepository;
import org.springframework.stereotype.Service;
import com.app.med_support.model.DonationReminder;
import com.app.med_support.model.DonationBooking;
import com.app.med_support.repository.DonationBookingRepository;

import java.time.LocalDateTime;

@Service
public class DonationReminderService {

    private final DonationReminderRepository donationReminderRepository;
    private final DonationBookingRepository donationBookingRepository;

    public DonationReminderService(
            DonationReminderRepository donationReminderRepository,
            DonationBookingRepository donationBookingRepository) {

        this.donationReminderRepository = donationReminderRepository;
        this.donationBookingRepository = donationBookingRepository;
    }

    public DonationReminder createReminder(Long bookingId, LocalDateTime reminderTime) {

        DonationBooking booking = donationBookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));


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

        return donationReminderRepository.save(reminder);
    }
}
