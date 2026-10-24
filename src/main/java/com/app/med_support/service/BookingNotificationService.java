package com.app.med_support.service;

import com.app.med_support.model.BookingNotification;
import com.app.med_support.model.DonationBooking;
import com.app.med_support.model.Notification;
import com.app.med_support.model.User;
import com.app.med_support.repository.NotificationRepository;
import com.app.med_support.repository.UserRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookingNotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public BookingNotificationService(SimpMessagingTemplate messagingTemplate,
            NotificationRepository notificationRepository,
            UserRepository userRepository) {

        this.messagingTemplate = messagingTemplate;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    // Send a real-time notification when a donation booking status changes
    public void sendDonationBookingStatusNotification(DonationBooking donationBooking) {
        BookingNotification bookingNotification = new BookingNotification(
        donationBooking.getId(), donationBooking.getDonationBookingStatus(),
                "Donation booking status changed to " + donationBooking.getDonationBookingStatus());

        Notification notification = new Notification();
        notification.setUser(donationBooking.getDonorProfile().getUser());
        notification.setDonationBooking(donationBooking);
        notification.setMessage(bookingNotification.getNotificationMessage());

        // Save the notification in PostgreSQL
        notificationRepository.save(notification);

        // Get the email of the donor who owns this booking
        String donorEmail = donationBooking.getDonorProfile().getUser().getEmail();

        messagingTemplate.convertAndSendToUser(donorEmail,
                "/queue/notifications", bookingNotification);

    }
    // saved notifications for the login user
    public List<Notification> getMyNotifications(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }
}
