
package com.medsupport.notification_service.consumer;

import com.medsupport.notification_service.event.BookingNotificationEvent;
import com.medsupport.notification_service.service.NotificationService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingEventConsumerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private BookingEventConsumer bookingEventConsumer;

    // TEST 1: Valid Kafka event creates a notification.
    @Test
    void shouldCreateNotificationWhenBookingEventReceived() {

        UUID eventId = UUID.randomUUID();

        BookingNotificationEvent event =
                new BookingNotificationEvent(
                        eventId,
                        10L,
                        "donor@example.com",
                        "BOOKING_CONFIRMED",
                        "Booking Confirmed",
                        "Your donation booking has been confirmed."
                );

        bookingEventConsumer.consumeBookingEvent(event);

        verify(notificationService).createNotificationFromEvent(
                eventId,
                "donor@example.com",
                "BOOKING_CONFIRMED",
                "Booking Confirmed",
                "Your donation booking has been confirmed.",
                10L
        );
    }

    // TEST 2: Reject null event.
    @Test
    void shouldRejectNullEvent() {

        assertThrows(
                IllegalArgumentException.class,
                () -> bookingEventConsumer.consumeBookingEvent(null)
        );

        verifyNoInteractions(notificationService);
    }

    // TEST 3: Reject missing booking ID.
    @Test
    void shouldRejectEventWithoutBookingId() {

        BookingNotificationEvent event =
                new BookingNotificationEvent(
                        UUID.randomUUID(),
                        null,
                        "donor@example.com",
                        "BOOKING_CREATED",
                        "Booking Created",
                        "Your booking has been created."
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> bookingEventConsumer.consumeBookingEvent(event)
        );

        verifyNoInteractions(notificationService);
    }

    // TEST 4: Reject missing email.
    @Test
    void shouldRejectEventWithoutRecipientEmail() {

        BookingNotificationEvent event =
                new BookingNotificationEvent(
                        UUID.randomUUID(),
                        10L,
                        "",
                        "BOOKING_CREATED",
                        "Booking Created",
                        "Your booking has been created."
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> bookingEventConsumer.consumeBookingEvent(event)
        );

        verifyNoInteractions(notificationService);
    }

    // TEST 5: Reject missing notification type.
    @Test
    void shouldRejectEventWithoutNotificationType() {

        BookingNotificationEvent event =
                new BookingNotificationEvent(
                        UUID.randomUUID(),
                        10L,
                        "donor@example.com",
                        null,
                        "Booking Created",
                        "Your booking has been created."
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> bookingEventConsumer.consumeBookingEvent(event)
        );

        verifyNoInteractions(notificationService);
    }

    // TEST 6: Database failure propagates.
    @Test
    void shouldPropagateNotificationCreationFailure() {

        UUID eventId = UUID.randomUUID();

        BookingNotificationEvent event =
                new BookingNotificationEvent(
                        eventId,
                        10L,
                        "donor@example.com",
                        "BOOKING_CONFIRMED",
                        "Booking Confirmed",
                        "Your booking has been confirmed."
                );

        doThrow(new IllegalStateException("Database error"))
                .when(notificationService)
                .createNotificationFromEvent(
                        eventId,
                        "donor@example.com",
                        "BOOKING_CONFIRMED",
                        "Booking Confirmed",
                        "Your booking has been confirmed.",
                        10L
                );

        assertThrows(
                IllegalStateException.class,
                () -> bookingEventConsumer.consumeBookingEvent(event)
        );

        verify(notificationService).createNotificationFromEvent(
                eventId,
                "donor@example.com",
                "BOOKING_CONFIRMED",
                "Booking Confirmed",
                "Your booking has been confirmed.",
                10L
        );
    }

    // TEST 7: Reject event without eventId.
    @Test
    void shouldRejectEventWithoutEventId() {

        BookingNotificationEvent event =
                new BookingNotificationEvent(
                        null,
                        10L,
                        "donor@example.com",
                        "BOOKING_CONFIRMED",
                        "Booking Confirmed",
                        "Your booking has been confirmed."
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> bookingEventConsumer.consumeBookingEvent(event)
        );

        verifyNoInteractions(notificationService);
    }

    // TEST 8: Reject event without title.
    @Test
    void shouldRejectEventWithoutTitle() {

        BookingNotificationEvent event =
                new BookingNotificationEvent(
                        UUID.randomUUID(),
                        10L,
                        "donor@example.com",
                        "BOOKING_CREATED",
                        "",
                        "Your booking has been created."
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> bookingEventConsumer.consumeBookingEvent(event)
        );

        verifyNoInteractions(notificationService);
    }

    // TEST 9: Reject event without message.
    @Test
    void shouldRejectEventWithoutMessage() {

        BookingNotificationEvent event =
                new BookingNotificationEvent(
                        UUID.randomUUID(),
                        10L,
                        "donor@example.com",
                        "BOOKING_CREATED",
                        "Booking Created",
                        " "
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> bookingEventConsumer.consumeBookingEvent(event)
        );

        verifyNoInteractions(notificationService);
    }
}
