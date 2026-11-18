
package com.medsupport.booking_service.scheduler;

import com.medsupport.booking_service.service.BookingOutboxPublisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BookingOutboxScheduler {

    private static final Logger logger = LoggerFactory.getLogger(BookingOutboxScheduler.class);
    private final BookingOutboxPublisher outboxPublisher;

    public BookingOutboxScheduler(BookingOutboxPublisher outboxPublisher
    ) {
        this.outboxPublisher = outboxPublisher;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:5000}")
    public void publishPendingEvents() {

        try {
            outboxPublisher.publishPendingEvents();

        } catch (Exception exception) {
            logger.error("Outbox publishing failed. Pending events will be retried.", exception);
        }
    }
}
