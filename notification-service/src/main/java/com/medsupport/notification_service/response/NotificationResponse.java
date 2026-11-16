
package com.medsupport.notification_service.response;

import com.medsupport.notification_service.model.Notification;

import java.time.LocalDateTime;

public record NotificationResponse(Long id, String recipientEmail,
        String notificationType, String title,
        String message, String status,
        Long bookingId, LocalDateTime createdAt, LocalDateTime sentAt
) {

    public static NotificationResponse from(Notification notification) {

        return new NotificationResponse(notification.getId(),
                notification.getRecipientEmail(),
                notification.getNotificationType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getStatus(),
                notification.getBookingId(),
                notification.getCreatedAt(),
                notification.getSentAt());
    }
}
