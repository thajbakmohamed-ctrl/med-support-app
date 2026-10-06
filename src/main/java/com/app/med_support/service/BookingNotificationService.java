package com.app.med_support.service;

import com.app.med_support.model.BookingNotification;
import com.app.med_support.model.DonationBooking;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class BookingNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public BookingNotificationService(SimpMessagingTemplate messagingTemplate) {

        this.messagingTemplate = messagingTemplate;
    }


    // Send a real-time notification when a donation booking status changes
    public void sendDonationBookingStatusNotification(DonationBooking donationBooking) {
        BookingNotification bookingNotification = new BookingNotification(
        donationBooking.getId(), donationBooking.getDonationBookingStatus(),
                "Donation booking status changed to " + donationBooking.getDonationBookingStatus());

        messagingTemplate.convertAndSend("/topic/donation-bookings", bookingNotification);
    }
}
