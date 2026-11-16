
package com.medsupport.notification_service.scheduler;

import com.medsupport.notification_service.model.Notification;
import com.medsupport.notification_service.service.NotificationService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailNotificationSchedulerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private EmailNotificationScheduler scheduler;

    // TEST 1: Process pending notifications.
    @Test
    void shouldSendPendingNotifications() {

        Notification first = new Notification();
        first.setId(1L);
        first.setStatus("PENDING");

        Notification second = new Notification();
        second.setId(2L);
        second.setStatus("PENDING");

        when(notificationService.getNotificationsReadyForSending())
                .thenReturn(List.of(first, second));

        when(notificationService.sendNotification(1L))
                .thenReturn(first);

        when(notificationService.sendNotification(2L))
                .thenReturn(second);

        scheduler.sendPendingEmails();

        verify(notificationService).sendNotification(1L);
        verify(notificationService).sendNotification(2L);
    }

    // TEST 2: Do nothing when the queue is empty.
    @Test
    void shouldDoNothingWhenNoNotificationsAreReady() {

        when(notificationService.getNotificationsReadyForSending())
                .thenReturn(List.of());

        scheduler.sendPendingEmails();

        verify(notificationService)
                .getNotificationsReadyForSending();

        verify(notificationService, never())
                .sendNotification(anyLong());
    }

    // TEST 3: Continue processing after one failure.
    @Test
    void shouldContinueWhenOneNotificationFails() {

        Notification first = new Notification();
        first.setId(1L);
        first.setStatus("PENDING");

        Notification second = new Notification();
        second.setId(2L);
        second.setStatus("PENDING");

        when(notificationService.getNotificationsReadyForSending())
                .thenReturn(List.of(first, second));

        when(notificationService.sendNotification(1L))
                .thenThrow(new IllegalStateException("Email failure"));

        when(notificationService.sendNotification(2L))
                .thenReturn(second);

        scheduler.sendPendingEmails();

        verify(notificationService).sendNotification(1L);
        verify(notificationService).sendNotification(2L);
    }

    // TEST 4: Process a failed notification that is due for retry.
    @Test
    void shouldRetryFailedNotificationWhenDue() {

        Notification failed = new Notification();
        failed.setId(3L);
        failed.setStatus("FAILED");
        failed.setRetryCount(1);

        when(notificationService.getNotificationsReadyForSending())
                .thenReturn(List.of(failed));

        when(notificationService.sendNotification(3L))
                .thenReturn(failed);

        scheduler.sendPendingEmails();

        verify(notificationService).sendNotification(3L);
    }

    // TEST 5: Skip notifications without IDs.
    @Test
    void shouldSkipNotificationWithoutId() {

        Notification invalid = new Notification();
        invalid.setStatus("PENDING");

        when(notificationService.getNotificationsReadyForSending())
                .thenReturn(List.of(invalid));

        scheduler.sendPendingEmails();

        verify(notificationService, never())
                .sendNotification(anyLong());
    }
}
