
package com.medsupport.notification_service.scheduler;

import com.medsupport.notification_service.model.Notification;
import com.medsupport.notification_service.service.NotificationService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EmailNotificationScheduler {

    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationScheduler.class);

    private final NotificationService notificationService;

    public EmailNotificationScheduler(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Scheduled(fixedDelayString = "${app.notification.email.poll-interval-ms:60000}")
    public void sendPendingEmails() {

        List<Notification> pendingNotifications = notificationService.getNotificationsReadyForSending();

        if (pendingNotifications.isEmpty()) {
            return;
        }

        logger.info("Found {} pending email notifications",
                pendingNotifications.size());

        for (Notification notification : pendingNotifications) {

            try {
                if (notification.getId() == null) {
                    logger.warn("Skipping notification without ID");
                    continue;
                }

                Notification result = notificationService.sendNotification(
                                notification.getId());

                logger.info("Email processing finished: notificationId={}, status={}", result.getId(),
                        result.getStatus());

            } catch (Exception ex) {

                logger.error("Failed to process notification ID: {}",
                        notification.getId(), ex);
            }
        }
    }


    @Scheduled(fixedDelayString = "${app.notification.recovery.poll-interval-ms:60000}")
    public void recoverStaleNotifications() {

        try {

            int recoveredCount = notificationService.recoverStaleProcessingNotifications();

            if (recoveredCount > 0) {
                logger.info("Recovered {} abandoned email notifications",
                        recoveredCount);
            }

        } catch (Exception ex) {

            logger.error("Email recovery scheduler failed", ex);
        }
    }

}
