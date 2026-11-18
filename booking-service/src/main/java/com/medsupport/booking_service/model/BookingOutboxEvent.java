
package com.medsupport.booking_service.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "booking_outbox_events",
        indexes = { @Index( name = "idx_booking_outbox_status",
                        columnList = "status, created_at"
                )
        }
)
public class BookingOutboxEvent {

    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "recipient_email", nullable = false)
    private String recipientEmail;

    @Column(name = "notification_type", nullable = false)
    private String notificationType;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "message", columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(name = "status", nullable = false)
    private String status = "PENDING";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    public BookingOutboxEvent() {
    }

    public BookingOutboxEvent(Long bookingId, String recipientEmail,
            String notificationType, String title, String message) {
        this.eventId = UUID.randomUUID();
        this.bookingId = bookingId;
        this.recipientEmail = recipientEmail;
        this.notificationType = notificationType;
        this.title = title;
        this.message = message;
        this.status = "PENDING";
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    public void onCreate() {
        if (eventId == null) {
            eventId = UUID.randomUUID();
        }

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (status == null) {
            status = "PENDING";
        }
    }

    public UUID getEventId() {
        return eventId;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }
}
