
package com.medsupport.booking_service.service;

import com.medsupport.booking_service.event.BookingNotificationEvent;
import com.medsupport.booking_service.model.BookingOutboxEvent;
import com.medsupport.booking_service.producer.BookingEventProducer;
import com.medsupport.booking_service.repository.BookingOutboxRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class BookingOutboxPublisher {

    private static final Logger logger = LoggerFactory.getLogger(BookingOutboxPublisher.class);

    private final BookingOutboxRepository outboxRepository;
    private final BookingEventProducer bookingEventProducer;

    public BookingOutboxPublisher(BookingOutboxRepository outboxRepository,
            BookingEventProducer bookingEventProducer) {
        this.outboxRepository = outboxRepository;
        this.bookingEventProducer = bookingEventProducer;
    }

    @Transactional
    public void publishPendingEvents() {

        List<BookingOutboxEvent> pendingEvents = outboxRepository.findPendingEventsForUpdate(10);

        for (BookingOutboxEvent outboxEvent : pendingEvents) {

            BookingNotificationEvent kafkaEvent = new BookingNotificationEvent(
                    outboxEvent.getEventId(), outboxEvent.getBookingId(),
                    outboxEvent.getRecipientEmail(), outboxEvent.getNotificationType(),
                    outboxEvent.getTitle(), outboxEvent.getMessage());

            try {

                bookingEventProducer.publishBookingEvent(kafkaEvent)
                        .get(15, TimeUnit.SECONDS);

                outboxEvent.setStatus("PUBLISHED");
                outboxEvent.setPublishedAt(LocalDateTime.now());

                outboxRepository.save(outboxEvent);

                logger.info("Outbox event published: eventId={}, bookingId={}",
                        outboxEvent.getEventId(), outboxEvent.getBookingId());

            } catch (InterruptedException exception) {

                Thread.currentThread().interrupt();

                throw new IllegalStateException("Outbox publishing was interrupted", exception);

            } catch (Exception exception) {

                logger.error("Failed to publish outbox event: eventId={}",
                        outboxEvent.getEventId(), exception);

                throw new IllegalStateException("Failed to publish booking event", exception);
            }
        }
    }
}
