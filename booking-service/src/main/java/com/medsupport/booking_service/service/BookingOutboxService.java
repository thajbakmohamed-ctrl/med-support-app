
package com.medsupport.booking_service.service;

import com.medsupport.booking_service.model.BookingOutboxEvent;
import com.medsupport.booking_service.repository.BookingOutboxRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingOutboxService {

    private final BookingOutboxRepository outboxRepository;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    @org.springframework.scheduling.annotation.Scheduled(fixedDelayString = "${app.reminders.poll-interval-ms:60000}")
    @Transactional
    public void queueAppointmentReminders() {
        @SuppressWarnings("unchecked")
        java.util.List<Object[]> due = entityManager.createNativeQuery("""
                SELECT b.id, o.recipient_email, b.donation_booking_date, b.donation_booking_time
                FROM donation_booking b
                JOIN LATERAL (SELECT recipient_email FROM booking_outbox_events
                    WHERE booking_id=b.id AND notification_type='BOOKING_CREATED'
                    ORDER BY created_at, event_id LIMIT 1) o ON true
                WHERE b.donation_booking_status IN ('PENDING','CONFIRMED')
                  AND b.donation_booking_date + b.donation_booking_time > LOCALTIMESTAMP
                  AND b.donation_booking_date + b.donation_booking_time <= LOCALTIMESTAMP + INTERVAL '24 hours'
                  AND NOT EXISTS (SELECT 1 FROM p4_donation_reminder WHERE booking_id=b.id)
                ORDER BY b.id LIMIT 20 FOR UPDATE OF b SKIP LOCKED
                """).getResultList();
        for (Object[] row : due) {
            Long bookingId = ((Number)row[0]).longValue();
            int inserted = entityManager.createNativeQuery(
                    "INSERT INTO p4_donation_reminder(booking_id) VALUES(:id) ON CONFLICT DO NOTHING")
                    .setParameter("id", bookingId).executeUpdate();
            if (inserted == 1) saveEvent(bookingId, (String)row[1], "APPOINTMENT_REMINDER",
                    "Upcoming donation appointment", "Reminder: your donation appointment is on "
                            + row[2] + " at " + row[3] + " UTC (Bahrain is UTC+3). Check My Donations for its current status.");
        }
    }

    public BookingOutboxService(
            BookingOutboxRepository outboxRepository
    ) {
        this.outboxRepository = outboxRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public BookingOutboxEvent saveEvent(Long bookingId, String recipientEmail,
            String notificationType, String title, String message) {
        if (bookingId == null || bookingId <= 0
                || recipientEmail == null || recipientEmail.isBlank()
                || notificationType == null || notificationType.isBlank()
                || title == null || title.isBlank()
                || message == null || message.isBlank()) {

            throw new IllegalArgumentException("Valid booking event information is required");
        }

        BookingOutboxEvent event = new BookingOutboxEvent(bookingId, recipientEmail,
                notificationType, title, message);

        return outboxRepository.save(event);
    }
}
