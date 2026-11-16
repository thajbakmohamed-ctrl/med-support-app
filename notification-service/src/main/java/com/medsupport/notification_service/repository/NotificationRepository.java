
package com.medsupport.notification_service.repository;

import com.medsupport.notification_service.model.Notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    org.springframework.data.domain.Page<Notification> findByRecipientEmailOrderByCreatedAtDescIdDesc(
            String email, org.springframework.data.domain.Pageable pageable);

    List<Notification> findByRecipientEmailOrderByCreatedAtDesc(String recipientEmail);

    List<Notification> findByStatus(String status);

    List<Notification> findByBookingId(Long bookingId);

    boolean existsByBookingIdAndNotificationType(Long bookingId, String notificationType);

    Optional<Notification> findFirstByBookingIdAndNotificationTypeOrderByIdAsc(
            Long bookingId, String notificationType);

    Optional<Notification> findByEventId(UUID eventId);

    boolean existsByEventId(UUID eventId);
    @Modifying
    @Transactional(propagation = Propagation.MANDATORY)
    @Query(
            value = """
                    INSERT INTO public.p4_notifications (
                        event_id,
                        recipient_email,
                        notification_type,
                        title,
                        message,
                        booking_id,
                        status,
                        created_at,
                        retry_count
                    )
                    VALUES (
                        :eventId,
                        :recipientEmail,
                        :notificationType,
                        :title,
                        :message,
                        :bookingId,
                        'PENDING',
                        :createdAt,
                        0
                    )
                    ON CONFLICT (event_id) DO NOTHING
                    """,
            nativeQuery = true
    )
    int insertKafkaNotificationIfAbsent(
            @Param("eventId") UUID eventId,
            @Param("recipientEmail") String recipientEmail,
            @Param("notificationType") String notificationType,
            @Param("title") String title,
            @Param("message") String message,
            @Param("bookingId") Long bookingId,
            @Param("createdAt") LocalDateTime createdAt
    );


    @Query("""
            SELECT n
            FROM Notification n
            WHERE
                (n.status = 'PENDING' AND n.retryCount < 3)
                OR
                (n.status = 'FAILED'
                    AND n.retryCount < 3
                    AND n.nextRetryAt IS NOT NULL
                    AND n.nextRetryAt <= :now)
            ORDER BY n.createdAt ASC
            """)
    List<Notification> findNotificationsReadyForSending(
            @Param("now") LocalDateTime now);


    @Modifying(clearAutomatically = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Query("""
            UPDATE Notification n
            SET n.status = 'PROCESSING',
                n.processingStartedAt = :now,
                n.processingToken = :processingToken,
                n.retryCount = n.retryCount + 1,
                n.nextRetryAt = NULL
            WHERE n.id = :notificationId
              AND n.retryCount < 3
              AND (
                  (n.status = 'PENDING'
                      AND (n.nextRetryAt IS NULL
                           OR n.nextRetryAt <= :now))
                  OR
                  (n.status = 'FAILED'
                      AND n.nextRetryAt IS NOT NULL
                      AND n.nextRetryAt <= :now)
              )
            """)
    int claimNotificationForProcessing(
            @Param("notificationId") Long notificationId,
            @Param("now") LocalDateTime now,
            @Param("processingToken") UUID processingToken);


    @Modifying(clearAutomatically = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Query("""
            UPDATE Notification n
            SET n.status = 'SENT',
                n.sentAt = :sentAt,
                n.nextRetryAt = NULL,
                n.processingStartedAt = NULL,
                n.processingToken = NULL
            WHERE n.id = :notificationId
              AND n.status = 'PROCESSING'
              AND n.processingToken = :processingToken
            """)
    int markSentIfOwned(
            @Param("notificationId") Long notificationId,
            @Param("processingToken") UUID processingToken,
            @Param("sentAt") LocalDateTime sentAt);


    @Modifying(clearAutomatically = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Query("""
            UPDATE Notification n
            SET n.status = 'FAILED',
                n.sentAt = NULL,
                n.nextRetryAt = :nextRetryAt,
                n.processingStartedAt = NULL,
                n.processingToken = NULL
            WHERE n.id = :notificationId
              AND n.status = 'PROCESSING'
              AND n.processingToken = :processingToken
            """)
    int markFailedIfOwned(
            @Param("notificationId") Long notificationId,
            @Param("processingToken") UUID processingToken,
            @Param("nextRetryAt") LocalDateTime nextRetryAt);


    @Query("""
            SELECT n
            FROM Notification n
            WHERE n.status = 'PROCESSING'
              AND n.processingToken IS NOT NULL
              AND n.processingStartedAt IS NOT NULL
              AND n.processingStartedAt <= :staleBefore
              AND n.retryCount BETWEEN 1 AND 3
            ORDER BY n.processingStartedAt ASC, n.id ASC
            """)
    List<Notification> findStaleProcessingCandidates(@Param("staleBefore") LocalDateTime staleBefore,
            Pageable pageable);


    @Modifying(clearAutomatically = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Query(value = """
                    UPDATE public.p4_notifications
                    SET status = 'FAILED',
                        sent_at = NULL,
                        next_retry_at = CASE
                            WHEN retry_count = 1
                                THEN CAST(:recoveredAt AS timestamp)
                                     + INTERVAL '30 seconds'
                            WHEN retry_count = 2
                                THEN CAST(:recoveredAt AS timestamp)
                                     + INTERVAL '60 seconds'
                            ELSE NULL
                        END,
                        processing_started_at = NULL,
                        processing_token = NULL
                    WHERE id = :notificationId
                      AND status = 'PROCESSING'
                      AND processing_token = :expectedProcessingToken
                      AND processing_started_at = :expectedProcessingStartedAt
                      AND processing_started_at <= :staleBefore
                      AND retry_count BETWEEN 1 AND 3
                    """,
            nativeQuery = true)
    int recoverProcessingAttemptIfUnchanged(
            @Param("notificationId") Long notificationId,
            @Param("expectedProcessingToken") UUID expectedProcessingToken,
            @Param("expectedProcessingStartedAt")
            LocalDateTime expectedProcessingStartedAt,
            @Param("staleBefore") LocalDateTime staleBefore,
            @Param("recoveredAt") LocalDateTime recoveredAt);



}
