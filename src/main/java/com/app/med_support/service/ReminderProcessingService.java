
package com.app.med_support.service;

import com.app.med_support.model.DonationBooking;
import com.app.med_support.model.DonationReminder;
import com.app.med_support.model.Notification;
import com.app.med_support.repository.DonationReminderRepository;
import com.app.med_support.repository.NotificationRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

@Service
public class ReminderProcessingService {

    private static final Logger logger =
            LoggerFactory.getLogger(ReminderProcessingService.class);

    private final DonationReminderRepository reminderRepository;
    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public ReminderProcessingService(
            DonationReminderRepository reminderRepository,
            NotificationRepository notificationRepository,
            SimpMessagingTemplate messagingTemplate) {

        this.reminderRepository = reminderRepository;
        this.notificationRepository = notificationRepository;
        this.messagingTemplate = messagingTemplate;
    }


    @Transactional
    public boolean claimReminder(Long reminderId) {

        int updatedRows = reminderRepository.claimReminder(reminderId);

        return updatedRows == 1;
    }


    @Transactional(readOnly = true)
    public ReminderEmailDetails getReminderEmailDetails(Long reminderId) {

        DonationReminder reminder = reminderRepository.findById(reminderId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Reminder not found"));

        if (!"PROCESSING".equals(reminder.getStatus())) {
            throw new IllegalStateException("Reminder is not being processed");
        }

        DonationBooking booking = reminder.getDonationBooking();

        if ("CANCELLED".equalsIgnoreCase(booking.getDonationBookingStatus())) {
            throw new IllegalStateException("Booking is cancelled");
        }

        String donorEmail = booking.getDonorProfile().getUser().getEmail();
        Long bookingId = booking.getId();

        return new ReminderEmailDetails(donorEmail, bookingId);
    }



    @Transactional
    public void completeReminder(Long reminderId) {

        DonationReminder reminder = reminderRepository.findById(reminderId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Reminder not found"));

        if (!"PROCESSING".equals(reminder.getStatus())) {
            throw new IllegalStateException(
                    "Reminder is not being processed");
        }

        reminder.setStatus("SENT");
        reminder.setSentAt(LocalDateTime.now());

        reminderRepository.save(reminder);
    }

    // Save the notification and send it through WebSocket after commit
    @Transactional
    public void sendReminderNotification(Long reminderId) {

        DonationReminder reminder = reminderRepository.findById(reminderId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Reminder not found"));

        if (!"PROCESSING".equals(reminder.getStatus())) {
            throw new IllegalStateException(
                    "Reminder is not being processed");
        }

        DonationBooking booking = reminder.getDonationBooking();

        if ("CANCELLED".equalsIgnoreCase(
                booking.getDonationBookingStatus())) {
            throw new IllegalStateException("Booking is cancelled");
        }

        Notification notification = new Notification();
        notification.setUser(booking.getDonorProfile().getUser());
        notification.setDonationBooking(booking);
        notification.setMessage(
                "Reminder: You have an upcoming blood donation appointment.");


        notificationRepository.save(notification);

        String donorEmail = booking.getDonorProfile().getUser().getEmail();

        String message = notification.getMessage();


        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {

                    @Override
                    public void afterCommit() {

                        try {
                            messagingTemplate.convertAndSendToUser(
                                    donorEmail,
                                    "/queue/notifications",
                                    message);

                            logger.info(
                                    "Reminder WebSocket notification published. Reminder ID: {}",
                                    reminderId);

                        } catch (Exception e) {
                            logger.error(
                                    "Failed to publish reminder WebSocket notification. Reminder ID: {}",
                                    reminderId,
                                    e
                            );
                        }
                    }
                }
        );
    }


    @Transactional
    public void failReminder(Long reminderId) {

        DonationReminder reminder = reminderRepository.findById(reminderId)
                .orElseThrow(() -> new IllegalArgumentException("Reminder not found"));

        if (!"PROCESSING".equals(reminder.getStatus())) {
            return;
        }

        reminder.setStatus("FAILED");
        reminderRepository.save(reminder);
    }

    public record ReminderEmailDetails(String donorEmail, Long bookingId) {
    }

}
