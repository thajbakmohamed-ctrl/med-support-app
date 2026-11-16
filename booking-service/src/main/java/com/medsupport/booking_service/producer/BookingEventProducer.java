
package com.medsupport.booking_service.producer;

import com.medsupport.booking_service.event.BookingNotificationEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class BookingEventProducer {

    private static final Logger logger = LoggerFactory.getLogger(BookingEventProducer.class);

    private final KafkaTemplate<String, BookingNotificationEvent> kafkaTemplate;
    private final String bookingTopic;

    public BookingEventProducer(KafkaTemplate<String, BookingNotificationEvent> kafkaTemplate,
            @Value("${app.kafka.booking-topic}") String bookingTopic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.bookingTopic = bookingTopic;
    }

    public CompletableFuture<Void> publishBookingEvent(BookingNotificationEvent event
    ) {
        if (event == null || event.bookingId() == null) {
            throw new IllegalArgumentException(
                    "Booking event and booking ID are required");
        }

        String key = event.bookingId().toString();

        return kafkaTemplate.send(bookingTopic, key, event)
                .thenAccept(result ->
                        logger.info(
                                "Booking event published: bookingId={}, type={}, partition={}, offset={}",
                                event.bookingId(), event.notificationType(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset()))
                .whenComplete((unused, exception) -> {
                    if (exception != null) {
                        logger.error("Failed to publish booking event: bookingId={}, type={}",
                                event.bookingId(), event.notificationType(), exception);
                    }
                });
    }
}
