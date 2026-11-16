
package com.medsupport.notification_service.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "p4_notifications", schema = "public")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", unique = true)
    private UUID eventId;

    @Column(name = "recipient_email", nullable = false)
    private String recipientEmail;

    @Column(name = "notification_type", nullable = false)
    private String notificationType;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(nullable = false)
    private String status = "PENDING";

    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;


    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "processing_started_at")
    private LocalDateTime processingStartedAt;


    @Column(name = "processing_token")
    private UUID processingToken;


    public Notification() {
    }


    public Notification(String recipientEmail, String notificationType,
            String title, String message, Long bookingId) {

        this.recipientEmail = recipientEmail;
        this.notificationType = notificationType;
        this.title = title;
        this.message = message;
        this.bookingId = bookingId;
        this.status = "PENDING";
        this.createdAt = LocalDateTime.now();
    }

    public Notification(UUID eventId, String recipientEmail, String notificationType,
            String title, String message, Long bookingId) {

        this(recipientEmail, notificationType, title, message, bookingId);

        this.eventId = eventId;
    }

    @PrePersist
    public void onCreate() {

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (status == null) {
            status = "PENDING";
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }


    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public LocalDateTime getNextRetryAt() {
        return nextRetryAt;
    }

    public void setNextRetryAt(LocalDateTime nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
    }




    public LocalDateTime getProcessingStartedAt() {
        return processingStartedAt;
    }

    public void setProcessingStartedAt(
            LocalDateTime processingStartedAt
    ) {
        this.processingStartedAt = processingStartedAt;
    }
    public UUID getProcessingToken() {
        return processingToken;
    }

    public void setProcessingToken(UUID processingToken) {
        this.processingToken = processingToken;
    }


}
