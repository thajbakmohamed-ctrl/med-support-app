
package com.medsupport.notification_service.consumer;

import com.medsupport.notification_service.event.BookingNotificationEvent;
import com.medsupport.notification_service.service.NotificationService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class BookingEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(BookingEventConsumer.class);

    private final NotificationService notificationService;

    public BookingEventConsumer(NotificationService notificationService
    ) {
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = "${app.kafka.booking-topic}",
            groupId = "notification-service")
    public void consumeBookingEvent(BookingNotificationEvent event) {

        if (event == null
                || event.eventId() == null
                || event.bookingId() == null
                || event.bookingId() <= 0
                || event.recipientEmail() == null
                || event.recipientEmail().isBlank()
                || event.notificationType() == null
                || event.notificationType().isBlank()
                || event.title() == null
                || event.title().isBlank()
                || event.message() == null
                || event.message().isBlank()) {

            logger.warn("Received an invalid booking notification event");

            throw new IllegalArgumentException("Invalid booking notification event");
        }

        logger.info("Received Kafka event: eventId={}, bookingId={}, type={}",
                event.eventId(), event.bookingId(), event.notificationType());

        notificationService.createNotificationFromEvent(event.eventId(),
                event.recipientEmail(), event.notificationType(),
                event.title(), event.message(), event.bookingId());

        logger.info("Kafka event processed: eventId={}", event.eventId());
    }
}
