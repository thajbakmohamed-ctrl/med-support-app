package com.medsupport.notification_service.service;

import com.medsupport.notification_service.model.Notification;
import com.medsupport.notification_service.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    private static final Long NOTIFICATION_ID = 1L;
    private static final Long BOOKING_ID = 10L;
    private static final String EMAIL = "donor@example.com";
    private static final String TYPE = "BOOKING_CONFIRMED";
    private static final String TITLE = "Booking Confirmed";
    private static final String MESSAGE = "Your booking has been confirmed.";

    @Mock private NotificationRepository repository;
    @Mock private JavaMailSender mailSender;
    @Mock private NotificationWorkerLock workerLock;

    private NotificationService service;
    private Notification notification;

    @BeforeEach
    void setUp() {
        service = new NotificationService(repository, mailSender, workerLock);
        notification = new Notification(EMAIL, TYPE, TITLE, MESSAGE, BOOKING_ID);
        notification.setId(NOTIFICATION_ID);
    }

    @Test
    void shouldCreateLegacyNotification() {
        when(repository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        Notification result = service.createNotification(EMAIL, TYPE, TITLE, MESSAGE, BOOKING_ID);
        assertEquals(EMAIL, result.getRecipientEmail());
        assertEquals("PENDING", result.getStatus());
        assertEquals(0, result.getRetryCount());
        assertNull(result.getEventId());
        verify(repository).findFirstByBookingIdAndNotificationTypeOrderByIdAsc(BOOKING_ID, TYPE);
        verify(repository).save(result);
        verifyNoInteractions(mailSender, workerLock);
    }

    @Test
    void shouldReturnExistingLegacyNotification() {
        when(repository.findFirstByBookingIdAndNotificationTypeOrderByIdAsc(BOOKING_ID, TYPE))
                .thenReturn(Optional.of(notification));
        assertSame(notification, service.createNotification(EMAIL, TYPE, TITLE, MESSAGE, BOOKING_ID));
        verify(repository, never()).save(any(Notification.class));
        verifyNoInteractions(mailSender, workerLock);
    }

    @Test
    void shouldRejectBlankLegacyRecipient() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createNotification(" ", TYPE, TITLE, MESSAGE, BOOKING_ID));
        verifyNoInteractions(repository, mailSender, workerLock);
    }

    @Test
    void shouldCreateKafkaNotificationUsingAtomicInsert() {
        UUID eventId = UUID.randomUUID();
        notification.setEventId(eventId);
        stubKafkaInsert(eventId, 1, notification);
        assertSame(notification, createFromEvent(eventId));
        InOrder order = inOrder(repository);
        order.verify(repository).insertKafkaNotificationIfAbsent(eq(eventId), eq(EMAIL), eq(TYPE),
                eq(TITLE), eq(MESSAGE), eq(BOOKING_ID), any(LocalDateTime.class));
        order.verify(repository).findByEventId(eventId);
        verify(repository, never()).save(any(Notification.class));
        verifyNoInteractions(mailSender, workerLock);
    }

    @Test
    void shouldReturnExistingNotificationForDuplicateEventId() {
        UUID eventId = UUID.randomUUID();
        notification.setEventId(eventId);
        stubKafkaInsert(eventId, 0, notification);
        assertSame(notification, createFromEvent(eventId));
        verify(repository).insertKafkaNotificationIfAbsent(eq(eventId), eq(EMAIL), eq(TYPE),
                eq(TITLE), eq(MESSAGE), eq(BOOKING_ID), any(LocalDateTime.class));
        verify(repository, never()).save(any(Notification.class));
    }

    @Test
    void shouldPreserveDifferentEventsForSameBookingAndType() {
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        Notification first = new Notification(firstId, EMAIL, TYPE, TITLE, MESSAGE, BOOKING_ID);
        Notification second = new Notification(secondId, EMAIL, TYPE, TITLE, MESSAGE, BOOKING_ID);
        stubKafkaInsert(firstId, 1, first);
        stubKafkaInsert(secondId, 1, second);
        assertSame(first, createFromEvent(firstId));
        assertSame(second, createFromEvent(secondId));
        assertNotEquals(first.getEventId(), second.getEventId());
        verify(repository, never()).findFirstByBookingIdAndNotificationTypeOrderByIdAsc(anyLong(), anyString());
        verify(repository, never()).save(any(Notification.class));
    }

    @Test
    void shouldRejectKafkaEventWithoutEventId() {
        assertThrows(IllegalArgumentException.class, () -> createFromEvent(null));
        verifyNoInteractions(repository, mailSender, workerLock);
    }

    @Test
    void shouldRejectKafkaEventWithoutMessage() {
        assertThrows(IllegalArgumentException.class, () -> service.createNotificationFromEvent(
                UUID.randomUUID(), EMAIL, TYPE, TITLE, " ", BOOKING_ID));
        verifyNoInteractions(repository, mailSender, workerLock);
    }

    @Test
    void shouldPropagateUnrelatedKafkaDatabaseFailure() {
        DataIntegrityViolationException failure = new DataIntegrityViolationException("Unrelated constraint");
        when(repository.insertKafkaNotificationIfAbsent(any(UUID.class), eq(EMAIL), eq(TYPE),
                eq(TITLE), eq(MESSAGE), eq(BOOKING_ID), any(LocalDateTime.class))).thenThrow(failure);
        assertSame(failure, assertThrows(DataIntegrityViolationException.class,
                () -> createFromEvent(UUID.randomUUID())));
        verify(repository, never()).findByEventId(any(UUID.class));
    }

    @Test
    void shouldFailIfKafkaNotificationCannotBeReloaded() {
        UUID eventId = UUID.randomUUID();
        when(repository.insertKafkaNotificationIfAbsent(eq(eventId), eq(EMAIL), eq(TYPE),
                eq(TITLE), eq(MESSAGE), eq(BOOKING_ID), any(LocalDateTime.class))).thenReturn(1);
        assertThrows(IllegalStateException.class, () -> createFromEvent(eventId));
    }

    @Test
    void shouldSendAndCompleteUsingClaimToken() {
        acquireWorkerLock();
        claimSuccessfully(0);
        completeSuccessfully();
        Notification result = service.sendNotification(NOTIFICATION_ID);
        assertEquals("SENT", result.getStatus());
        assertEquals(1, result.getRetryCount());
        assertNotNull(result.getSentAt());
        assertNull(result.getProcessingToken());
        assertNull(result.getProcessingStartedAt());
        assertNull(result.getNextRetryAt());
        ArgumentCaptor<UUID> token = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<SimpleMailMessage> mail = ArgumentCaptor.forClass(SimpleMailMessage.class);
        InOrder order = inOrder(workerLock, repository, mailSender);
        order.verify(workerLock).tryWithLock(eq(NOTIFICATION_ID), any());
        order.verify(repository).claimNotificationForProcessing(eq(NOTIFICATION_ID), any(LocalDateTime.class), token.capture());
        order.verify(repository).findById(NOTIFICATION_ID);
        order.verify(mailSender).send(mail.capture());
        order.verify(repository).markSentIfOwned(eq(NOTIFICATION_ID), eq(token.getValue()), any(LocalDateTime.class));
        order.verify(repository).findById(NOTIFICATION_ID);
        assertArrayEquals(new String[]{EMAIL}, mail.getValue().getTo());
        assertEquals(TITLE, mail.getValue().getSubject());
        assertEquals(MESSAGE, mail.getValue().getText());
        verify(repository, never()).markFailedIfOwned(anyLong(), any(UUID.class), nullable(LocalDateTime.class));
        verify(repository, never()).save(any(Notification.class));
    }

    @Test
    void shouldScheduleThirtySecondBackoffAfterFirstFailure() {
        assertFailureBackoff(0, 30);
    }

    @Test
    void shouldScheduleSixtySecondBackoffAfterSecondFailure() {
        assertFailureBackoff(1, 60);
    }

    @Test
    void shouldStopSchedulingRetriesAfterThirdFailure() {
        acquireWorkerLock();
        claimSuccessfully(2);
        failSmtp();
        completeFailure();
        Notification result = service.sendNotification(NOTIFICATION_ID);
        assertEquals("FAILED", result.getStatus());
        assertEquals(3, result.getRetryCount());
        assertNull(result.getNextRetryAt());
        assertNull(result.getProcessingToken());
        assertNull(result.getProcessingStartedAt());
        verify(repository).markFailedIfOwned(eq(NOTIFICATION_ID), any(UUID.class), isNull());
        verify(repository, never()).save(any(Notification.class));
    }

    @Test
    void shouldNotSendWhenAttemptLimitPreventsClaim() {
        notification.setStatus("FAILED");
        notification.setRetryCount(3);
        acquireWorkerLock();
        rejectClaim();
        assertSame(notification, service.sendNotification(NOTIFICATION_ID));
        assertEquals(3, notification.getRetryCount());
        verifyNoInteractions(mailSender);
        verifyNoCompletion();
    }

    @Test
    void shouldNotResendAlreadySentNotification() {
        notification.setStatus("SENT");
        acquireWorkerLock();
        rejectClaim();
        assertSame(notification, service.sendNotification(NOTIFICATION_ID));
        verifyNoInteractions(mailSender);
        verifyNoCompletion();
    }

    @Test
    void shouldNotSendWhenAnotherWorkerOwnsClaim() {
        notification.setStatus("PROCESSING");
        notification.setProcessingToken(UUID.randomUUID());
        acquireWorkerLock();
        rejectClaim();
        assertSame(notification, service.sendNotification(NOTIFICATION_ID));
        verifyNoInteractions(mailSender);
        verifyNoCompletion();
    }

    @Test
    void shouldSkipWithoutClaimingWhenWorkerLockIsUnavailable() {
        when(workerLock.<Notification>tryWithLock(eq(NOTIFICATION_ID), any())).thenReturn(Optional.empty());
        when(repository.findById(NOTIFICATION_ID)).thenReturn(Optional.of(notification));
        assertSame(notification, service.sendNotification(NOTIFICATION_ID));
        assertEquals(0, notification.getRetryCount());
        verify(repository, never()).claimNotificationForProcessing(anyLong(), any(LocalDateTime.class), any(UUID.class));
        verifyNoInteractions(mailSender);
        verifyNoCompletion();
    }

    @Test
    void shouldRejectUnknownNotification() {
        acquireWorkerLock();
        when(repository.claimNotificationForProcessing(eq(NOTIFICATION_ID), any(LocalDateTime.class), any(UUID.class)))
                .thenReturn(0);
        assertThrows(NoSuchElementException.class, () -> service.sendNotification(NOTIFICATION_ID));
        verifyNoInteractions(mailSender);
        verifyNoCompletion();
    }

    @Test
    void shouldRejectInvalidNotificationIdBeforeLocking() {
        assertThrows(IllegalArgumentException.class, () -> service.sendNotification(null));
        assertThrows(IllegalArgumentException.class, () -> service.sendNotification(0L));
        verifyNoInteractions(workerLock, repository, mailSender);
    }

    @Test
    void shouldNotSendWhenReloadedClaimHasDifferentToken() {
        acquireWorkerLock();
        claimSuccessfully(0);
        when(repository.findById(NOTIFICATION_ID)).thenAnswer(invocation -> {
            notification.setProcessingToken(UUID.randomUUID());
            return Optional.of(notification);
        });
        assertThrows(IllegalStateException.class, () -> service.sendNotification(NOTIFICATION_ID));
        verifyNoInteractions(mailSender);
        verifyNoCompletion();
    }

    @Test
    void shouldRejectSuccessfulCompletionWhenOwnershipIsLost() {
        acquireWorkerLock();
        claimSuccessfully(0);
        when(repository.markSentIfOwned(eq(NOTIFICATION_ID), any(UUID.class), any(LocalDateTime.class))).thenReturn(0);
        assertThrows(IllegalStateException.class, () -> service.sendNotification(NOTIFICATION_ID));
        verify(mailSender).send(any(SimpleMailMessage.class));
        verify(repository, never()).markFailedIfOwned(anyLong(), any(UUID.class), nullable(LocalDateTime.class));
        verify(repository, never()).save(any(Notification.class));
    }

    @Test
    void shouldRejectFailedCompletionWhenOwnershipIsLost() {
        acquireWorkerLock();
        claimSuccessfully(0);
        failSmtp();
        when(repository.markFailedIfOwned(eq(NOTIFICATION_ID), any(UUID.class), any(LocalDateTime.class))).thenReturn(0);
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> service.sendNotification(NOTIFICATION_ID));
        assertInstanceOf(MailSendException.class, failure.getCause());
        verify(repository, never()).markSentIfOwned(anyLong(), any(UUID.class), any(LocalDateTime.class));
        verify(repository, never()).save(any(Notification.class));
    }

    @Test
    void shouldPropagateCompletionDatabaseFailureWithoutCallingItSmtpFailure() {
        acquireWorkerLock();
        claimSuccessfully(0);
        DataIntegrityViolationException failure = new DataIntegrityViolationException("Completion failed");
        when(repository.markSentIfOwned(eq(NOTIFICATION_ID), any(UUID.class), any(LocalDateTime.class))).thenThrow(failure);
        assertSame(failure, assertThrows(DataIntegrityViolationException.class,
                () -> service.sendNotification(NOTIFICATION_ID)));
        verify(mailSender).send(any(SimpleMailMessage.class));
        verify(repository, never()).markFailedIfOwned(anyLong(), any(UUID.class), nullable(LocalDateTime.class));
    }

    @Test
    void shouldGenerateNewTokenForEachClaimAttempt() {
        acquireWorkerLock();
        claimSuccessfully(0);
        failSmtp();
        completeFailure();
        service.sendNotification(NOTIFICATION_ID);
        // Model a later retry becoming due, without waiting or connecting to a database.
        notification.setNextRetryAt(LocalDateTime.now().minusSeconds(1));
        service.sendNotification(NOTIFICATION_ID);
        ArgumentCaptor<UUID> tokens = ArgumentCaptor.forClass(UUID.class);
        verify(repository, times(2)).claimNotificationForProcessing(eq(NOTIFICATION_ID), any(LocalDateTime.class), tokens.capture());
        List<UUID> values = tokens.getAllValues();
        assertNotNull(values.get(0));
        assertNotNull(values.get(1));
        assertNotEquals(values.get(0), values.get(1));
        verify(repository).markFailedIfOwned(eq(NOTIFICATION_ID), eq(values.get(0)), any(LocalDateTime.class));
        verify(repository).markFailedIfOwned(eq(NOTIFICATION_ID), eq(values.get(1)), any(LocalDateTime.class));
        assertEquals(2, notification.getRetryCount());
    }

    @Test
    void shouldReturnPendingNotifications() {
        when(repository.findByStatus("PENDING")).thenReturn(List.of(notification));
        assertEquals(List.of(notification), service.getPendingNotifications());
        verify(repository).findByStatus("PENDING");
        verifyNoInteractions(mailSender, workerLock);
    }

    @Test
    void shouldDelegateReadyNotificationLookupWithCurrentTime() {
        when(repository.findNotificationsReadyForSending(any(LocalDateTime.class))).thenReturn(List.of(notification));
        LocalDateTime before = LocalDateTime.now();
        List<Notification> result = service.getNotificationsReadyForSending();
        LocalDateTime after = LocalDateTime.now();
        ArgumentCaptor<LocalDateTime> time = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(repository).findNotificationsReadyForSending(time.capture());
        assertEquals(List.of(notification), result);
        assertFalse(time.getValue().isBefore(before));
        assertFalse(time.getValue().isAfter(after));
    }

    private Notification createFromEvent(UUID eventId) {
        return service.createNotificationFromEvent(eventId, EMAIL, TYPE, TITLE, MESSAGE, BOOKING_ID);
    }

    private void stubKafkaInsert(UUID eventId, int insertedRows, Notification stored) {
        when(repository.insertKafkaNotificationIfAbsent(eq(eventId), eq(EMAIL), eq(TYPE), eq(TITLE),
                eq(MESSAGE), eq(BOOKING_ID), any(LocalDateTime.class))).thenReturn(insertedRows);
        when(repository.findByEventId(eventId)).thenReturn(Optional.of(stored));
    }

    private void acquireWorkerLock() {
        when(workerLock.<Notification>tryWithLock(eq(NOTIFICATION_ID), any())).thenAnswer(invocation -> {
            Supplier<Notification> action = invocation.getArgument(1);
            return Optional.of(action.get());
        });
    }

    // Simulate repository persistence; these unit tests do not execute the repository SQL.
    private void claimSuccessfully(int previousAttempts) {
        notification.setRetryCount(previousAttempts);
        when(repository.claimNotificationForProcessing(eq(NOTIFICATION_ID), any(LocalDateTime.class), any(UUID.class)))
                .thenAnswer(invocation -> {
                    notification.setStatus("PROCESSING");
                    notification.setProcessingStartedAt(invocation.getArgument(1));
                    notification.setProcessingToken(invocation.getArgument(2));
                    notification.setRetryCount(notification.getRetryCount() + 1);
                    notification.setNextRetryAt(null);
                    return 1;
                });
        when(repository.findById(NOTIFICATION_ID)).thenReturn(Optional.of(notification));
    }

    private void rejectClaim() {
        when(repository.claimNotificationForProcessing(eq(NOTIFICATION_ID), any(LocalDateTime.class), any(UUID.class)))
                .thenReturn(0);
        when(repository.findById(NOTIFICATION_ID)).thenReturn(Optional.of(notification));
    }

    private void completeSuccessfully() {
        when(repository.markSentIfOwned(eq(NOTIFICATION_ID), any(UUID.class), any(LocalDateTime.class)))
                .thenAnswer(invocation -> {
                    assertEquals(notification.getProcessingToken(), invocation.<UUID>getArgument(1));
                    assertEquals("PROCESSING", notification.getStatus());
                    notification.setStatus("SENT");
                    notification.setSentAt(invocation.getArgument(2));
                    notification.setNextRetryAt(null);
                    clearOwnership();
                    return 1;
                });
    }

    private void completeFailure() {
        when(repository.markFailedIfOwned(eq(NOTIFICATION_ID), any(UUID.class), nullable(LocalDateTime.class)))
                .thenAnswer(invocation -> {
                    assertEquals(notification.getProcessingToken(), invocation.<UUID>getArgument(1));
                    assertEquals("PROCESSING", notification.getStatus());
                    notification.setStatus("FAILED");
                    notification.setSentAt(null);
                    notification.setNextRetryAt(invocation.getArgument(2));
                    clearOwnership();
                    return 1;
                });
    }

    private void clearOwnership() {
        notification.setProcessingStartedAt(null);
        notification.setProcessingToken(null);
    }

    private void failSmtp() {
        doThrow(new MailSendException("Simulated SMTP failure"))
                .when(mailSender).send(any(SimpleMailMessage.class));
    }

    private void assertFailureBackoff(int previousAttempts, long seconds) {
        acquireWorkerLock();
        claimSuccessfully(previousAttempts);
        failSmtp();
        completeFailure();
        LocalDateTime before = LocalDateTime.now();
        Notification result = service.sendNotification(NOTIFICATION_ID);
        LocalDateTime after = LocalDateTime.now();
        assertEquals("FAILED", result.getStatus());
        assertEquals(previousAttempts + 1, result.getRetryCount());
        assertNull(result.getSentAt());
        assertNull(result.getProcessingToken());
        assertNull(result.getProcessingStartedAt());
        ArgumentCaptor<UUID> token = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<LocalDateTime> retryAt = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(repository).claimNotificationForProcessing(eq(NOTIFICATION_ID), any(LocalDateTime.class), token.capture());
        verify(repository).markFailedIfOwned(eq(NOTIFICATION_ID), eq(token.getValue()), retryAt.capture());
        assertNotNull(retryAt.getValue());
        assertFalse(retryAt.getValue().isBefore(before.plusSeconds(seconds)));
        assertFalse(retryAt.getValue().isAfter(after.plusSeconds(seconds)));
        verify(mailSender).send(any(SimpleMailMessage.class));
        verify(repository, never()).markSentIfOwned(anyLong(), any(UUID.class), any(LocalDateTime.class));
        verify(repository, never()).save(any(Notification.class));
    }

    private void verifyNoCompletion() {
        verify(repository, never()).markSentIfOwned(anyLong(), any(UUID.class), any(LocalDateTime.class));
        verify(repository, never()).markFailedIfOwned(anyLong(), any(UUID.class), nullable(LocalDateTime.class));
        verify(repository, never()).save(any(Notification.class));
    }
}
