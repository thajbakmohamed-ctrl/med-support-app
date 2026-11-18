
package com.medsupport.notification_service.service;

import com.medsupport.notification_service.model.Notification;
import com.medsupport.notification_service.repository.NotificationRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final JavaMailSender mailSender;
    private final NotificationWorkerLock notificationWorkerLock;
    @Value("${app.notification.recovery.enabled:false}")
    private boolean recoveryEnabled;

    @Value("${app.notification.recovery.stale-after-seconds:300}")
    private long recoveryStaleAfterSeconds;

    @Value("${app.notification.recovery.batch-size:20}")
    private int recoveryBatchSize;
    public NotificationService(NotificationRepository notificationRepository,
            JavaMailSender mailSender, NotificationWorkerLock notificationWorkerLock) {
        this.notificationRepository = notificationRepository;
        this.mailSender = mailSender;
        this.notificationWorkerLock = notificationWorkerLock;
    }

    public Notification createNotification(String recipientEmail,
            String notificationType, String title,
            String message, Long bookingId) {

        if (recipientEmail == null || recipientEmail.isBlank()
                || notificationType == null || notificationType.isBlank()
                || title == null || title.isBlank()
                || message == null || message.isBlank()) {

            throw new IllegalArgumentException("Notification fields must not be empty");
        }

        if (bookingId != null) {

            Optional<Notification> existingNotification = notificationRepository
                    .findFirstByBookingIdAndNotificationTypeOrderByIdAsc(
                            bookingId, notificationType);

            if (existingNotification.isPresent()) {

                logger.info("Duplicate notification ignored: bookingId={}, type={}",
                        bookingId, notificationType);

                return existingNotification.get();
            }
        }

        Notification notification = new Notification(recipientEmail,
                notificationType, title,message, bookingId);

        Notification savedNotification = notificationRepository.save(notification);

        logger.info("Notification created: id={}, bookingId={}, type={}",
                savedNotification.getId(), bookingId, notificationType);

        return savedNotification;
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW,
            isolation = Isolation.READ_COMMITTED
    )
    public Notification createNotificationFromEvent(UUID eventId,
            String recipientEmail, String notificationType, String title,
            String message, Long bookingId) {

        if (eventId == null
                || bookingId == null
                || bookingId <= 0
                || recipientEmail == null
                || recipientEmail.isBlank()
                || notificationType == null
                || notificationType.isBlank()
                || title == null
                || title.isBlank()
                || message == null
                || message.isBlank()) {

            throw new IllegalArgumentException(
                    "Invalid Kafka notification event");
        }

        int insertedRows = notificationRepository.insertKafkaNotificationIfAbsent(
                eventId, recipientEmail, notificationType, title,
                message, bookingId, LocalDateTime.now());

        Notification notification = notificationRepository.findByEventId(eventId)
                .orElseThrow(() -> new IllegalStateException(
                        "Notification not found after Kafka event insert: " + eventId));

        logger.info("Kafka notification persistence: eventId={}, bookingId={}, "
                        + "type={}, inserted={}", eventId, bookingId, notificationType,
                insertedRows == 1);

        return notification;
    }


    @Transactional(propagation = Propagation.NEVER)
    public Notification sendNotification(Long notificationId) {

        if (notificationId == null || notificationId <= 0) {
            throw new IllegalArgumentException("Valid notification ID is required");
        }

        Optional<Notification> result = notificationWorkerLock.tryWithLock(
                notificationId, () -> sendNotificationWhileLocked(notificationId));

        if (result.isPresent()) {
            return result.get();
        }


        logger.info("Email sending skipped because worker lock is held: id={}",
                notificationId);

        return notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NoSuchElementException(
                                "Notification not found"));
    }


    private Notification sendNotificationWhileLocked(
            Long notificationId
    ) {

        final int maxAttempts = 3;
        final UUID processingToken = UUID.randomUUID();


        int claimedRows = notificationRepository.claimNotificationForProcessing(
                notificationId, LocalDateTime.now(), processingToken);

        if (claimedRows == 0) {

            logger.info("Notification was not claimed: id={}", notificationId);

            return notificationRepository.findById(notificationId)
                    .orElseThrow(() -> new NoSuchElementException("Notification not found"));
        }

        if (claimedRows != 1) {
            throw new IllegalStateException("Unexpected notification claim count: "
                            + claimedRows);
        }


        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NoSuchElementException("Notification not found"));


        if (!"PROCESSING".equals(notification.getStatus()) || !processingToken.equals(
                notification.getProcessingToken())) {

            throw new IllegalStateException("Notification claim ownership was lost: id="
                            + notificationId);
        }


        SimpleMailMessage mailMessage = new SimpleMailMessage();

        mailMessage.setTo(notification.getRecipientEmail());
        mailMessage.setSubject(notification.getTitle());
        mailMessage.setText(notification.getMessage());

        MailException smtpFailure = null;

        try {
            mailSender.send(mailMessage);
        } catch (MailException ex) {
            smtpFailure = ex;
        }

        final int updatedRows;

        if (smtpFailure == null) {

            updatedRows = notificationRepository.markSentIfOwned(
                    notificationId, processingToken, LocalDateTime.now());

        } else {

            int attempts = notification.getRetryCount();
            LocalDateTime nextRetryAt = null;

            if (attempts < maxAttempts) {

                long delaySeconds = 30L * (1L << (attempts - 1));

                nextRetryAt = LocalDateTime.now().plusSeconds(delaySeconds);
            }

            updatedRows = notificationRepository.markFailedIfOwned(notificationId,
                    processingToken, nextRetryAt);
        }

        if (updatedRows == 0) {

            logger.warn("Email completion rejected: notificationId={}, "
                            + "processingToken={}, smtpAccepted={}",
                    notificationId, processingToken, smtpFailure == null);

            throw new IllegalStateException("Notification completion rejected: claim ownership lost for id="
                    + notificationId, smtpFailure);
        }

        if (updatedRows != 1) {

            throw new IllegalStateException("Unexpected notification completion count: "
                            + updatedRows);
        }

        if (smtpFailure == null) {

            logger.info("SMTP accepted email and SENT was recorded: notificationId={}",
                    notificationId);

        } else {

            logger.warn("Email failure recorded: notificationId={}, attempt={}, "
                            + "retryAvailable={}",
                    notificationId, notification.getRetryCount(),
                    notification.getRetryCount() < maxAttempts,
                    smtpFailure);
        }


        return notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Notification not found after email completion: " + notificationId));
    }


    public List<Notification> getNotificationsByRecipient(String recipientEmail) {

        if (recipientEmail == null || recipientEmail.isBlank()) {
            throw new IllegalArgumentException("Recipient email is required");
        }

        return notificationRepository.findByRecipientEmailOrderByCreatedAtDesc(recipientEmail);
    }


    @Transactional(propagation = Propagation.NEVER)
    public int recoverStaleProcessingNotifications() {

        if (!recoveryEnabled) {
            return 0;
        }

        if (recoveryStaleAfterSeconds <= 0) {
            throw new IllegalStateException("Recovery stale-after-seconds must be positive");
        }

        if (recoveryBatchSize < 1 || recoveryBatchSize > 100) {
            throw new IllegalStateException("Recovery batch-size must be between 1 and 100");
        }

        final LocalDateTime staleBefore = LocalDateTime.now()
                .minusSeconds(recoveryStaleAfterSeconds);

        List<Notification> candidates = notificationRepository.findStaleProcessingCandidates(
                staleBefore, PageRequest.of(0, recoveryBatchSize));

        int recoveredCount = 0;

        for (Notification candidate : candidates) {

            final Long notificationId = candidate.getId();
            final UUID expectedToken = candidate.getProcessingToken();
            final LocalDateTime expectedStartedAt = candidate.getProcessingStartedAt();

            if (notificationId == null
                    || notificationId <= 0
                    || expectedToken == null
                    || expectedStartedAt == null) {

                logger.warn("Skipping recovery candidate with missing ownership data: id={}",
                        notificationId);
                continue;
            }

            try {

                Optional<Integer> result = notificationWorkerLock.tryWithLock(
                        notificationId, () -> notificationRepository.recoverProcessingAttemptIfUnchanged(
                                notificationId, expectedToken, expectedStartedAt,
                                staleBefore, LocalDateTime.now()));

                if (result.isEmpty()) {

                    logger.debug("Recovery skipped because worker lock is held: id={}",
                            notificationId);

                    continue;
                }

                int updatedRows = result.get();

                if (updatedRows == 0) {

                    logger.debug("Recovery skipped because candidate no longer matches: id={}",
                            notificationId);

                    continue;
                }

                if (updatedRows != 1) {
                    throw new IllegalStateException("Unexpected notification recovery count: "
                                    + updatedRows);
                }

                recoveredCount++;

                logger.info("Abandoned notification attempt recovered: "
                                + "id={}, previousProcessingToken={}, attempt={}",
                        notificationId, expectedToken, candidate.getRetryCount());

            } catch (Exception ex) {
                logger.error("Notification recovery failed or its result is uncertain: id={}",
                        notificationId, ex);
            }
        }

        return recoveredCount;
    }


    public List<Notification> getNotificationsByBooking(Long bookingId) {

        if (bookingId == null || bookingId <= 0) {
            throw new IllegalArgumentException("Valid booking ID is required");
        }

        return notificationRepository.findByBookingId(bookingId);
    }

    public List<Notification> getPendingNotifications() {

        return notificationRepository.findByStatus("PENDING");
    }

    public List<Notification> getNotificationsReadyForSending() {

        LocalDateTime now = LocalDateTime.now();

        return notificationRepository.findNotificationsReadyForSending(now);
    }
}
