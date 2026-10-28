package com.app.med_support.service;

import com.app.med_support.model.BookingNotification;
import com.app.med_support.model.DonationBooking;
import com.app.med_support.model.Notification;
import com.app.med_support.model.User;
import com.app.med_support.repository.NotificationRepository;
import com.app.med_support.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Service
public class BookingNotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    private static final Logger logger = LoggerFactory.getLogger(BookingNotificationService.class);

    public BookingNotificationService(SimpMessagingTemplate messagingTemplate,
            NotificationRepository notificationRepository,
            UserRepository userRepository) {

        this.messagingTemplate = messagingTemplate;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    // Send a real-time notification when a donation booking status changes
    public void sendDonationBookingStatusNotification(DonationBooking donationBooking) {


        logger.info("Booking notification started. Booking ID: {}, Status: {}",
                donationBooking.getId(), donationBooking.getDonationBookingStatus());

        BookingNotification bookingNotification = new BookingNotification(
        donationBooking.getId(), donationBooking.getDonationBookingStatus(),
                "Donation booking status changed to " + donationBooking.getDonationBookingStatus());

        Notification notification = new Notification();
        notification.setUser(donationBooking.getDonorProfile().getUser());
        notification.setDonationBooking(donationBooking);
        notification.setMessage(bookingNotification.getNotificationMessage());


        notificationRepository.save(notification);

        String donorEmail = donationBooking.getDonorProfile().getUser().getEmail();

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {messagingTemplate.convertAndSendToUser(
                                donorEmail,
                                    "/queue/notifications", bookingNotification);
                            logger.info("WebSocket notification published. Booking ID: {}",
                                    donationBooking.getId());
                        } catch (Exception e) {
                            logger.error("WebSocket notification failed. Booking ID: {}",
                                    donationBooking.getId(), e);
                        }
                    }
                }
        );

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
