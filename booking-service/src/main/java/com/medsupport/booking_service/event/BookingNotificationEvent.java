
package com.medsupport.booking_service.event;

import java.util.UUID;

public record BookingNotificationEvent(UUID eventId, Long bookingId,
        String recipientEmail, String notificationType,
        String title, String message) {
}
