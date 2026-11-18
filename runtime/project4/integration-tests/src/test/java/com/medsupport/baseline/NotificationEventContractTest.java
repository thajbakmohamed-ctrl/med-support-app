package com.medsupport.baseline;

import com.medsupport.booking_service.client.BloodRequestClient;
import com.medsupport.booking_service.client.IdentityClient;
import com.medsupport.booking_service.controller.DonationBookingController;
import com.medsupport.booking_service.event.BookingNotificationEvent;
import com.medsupport.booking_service.exception.GlobalExceptionHandler;
import com.medsupport.booking_service.service.DonationBookingService;
import com.medsupport.notification_service.consumer.BookingEventConsumer;
import com.medsupport.notification_service.service.NotificationService;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import org.junit.platform.suite.api.*;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/core.feature")
@ConfigurationParameter(key="cucumber.glue", value="com.medsupport.baseline")
@ConfigurationParameter(key="cucumber.plugin", value="pretty")
public class NotificationEventContractTest {
    static final UUID EVENT=UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    static BookingNotificationEvent fixture() {
        return new BookingNotificationEvent(EVENT,7L,"donor@example.invalid","BOOKING_CREATED",
                "Donation Booking Created","Synthetic test message");
    }
    private final NotificationService persistence=mock(NotificationService.class);
    private BookingNotificationEvent event;
    private RuntimeException failure;
    private int httpStatus;

    @Given("a valid booking notification")
    public void validEvent() { event=fixture(); }

    @Given("an invalid booking notification")
    public void invalidEvent() { event=null; }

    @When("Notification consumes the event")
    public void consume() {
        try(var serializer=new JacksonJsonSerializer<BookingNotificationEvent>();
            var deserializer=new JacksonJsonDeserializer<>(com.medsupport.notification_service.event.BookingNotificationEvent.class,false)) {
            com.medsupport.notification_service.event.BookingNotificationEvent received=null;
            if(event!=null) {
                byte[] bytes=serializer.serialize("booking-events",event);
                assertEquals(Set.of("eventId","bookingId","recipientEmail","notificationType","title","message"),
                        new HashSet<>(JsonMapper.builder().build().readTree(bytes).propertyNames()));
                received=deserializer.deserialize("booking-events",bytes);
            }
            try { new BookingEventConsumer(persistence).consumeBookingEvent(received); }
            catch(RuntimeException e) { failure=e; }
        }
    }

    @Then("the notification is submitted once for persistence")
    public void accepted() {
        assertNull(failure);
        verify(persistence).createNotificationFromEvent(EVENT,"donor@example.invalid","BOOKING_CREATED",
                "Donation Booking Created","Synthetic test message",7L);
        verifyNoMoreInteractions(persistence);
    }

    @Then("the event is rejected without persistence")
    public void rejected() {
        assertInstanceOf(IllegalArgumentException.class,failure);
        verifyNoInteractions(persistence);
    }

    @When("a visitor requests their bookings without a bearer token")
    public void unauthenticatedBookingRequest() throws Exception {
        var service=mock(DonationBookingService.class);
        var mvc=MockMvcBuilders.standaloneSetup(new DonationBookingController(service,
                mock(IdentityClient.class),mock(BloodRequestClient.class)))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        httpStatus=mvc.perform(get("/api/donation-bookings/my")).andReturn().getResponse().getStatus();
        verifyNoInteractions(service);
    }

    @Then("the response is unauthorized")
    public void unauthorized() { assertEquals(401,httpStatus); }
}
