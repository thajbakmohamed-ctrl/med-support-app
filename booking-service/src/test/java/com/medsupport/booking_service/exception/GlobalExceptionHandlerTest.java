package com.medsupport.booking_service.exception;

import com.medsupport.booking_service.client.BloodRequestClient;
import com.medsupport.booking_service.client.IdentityClient;
import com.medsupport.booking_service.controller.DonationBookingController;
import com.medsupport.booking_service.controller.DonationSlotController;
import com.medsupport.booking_service.request.DonationBookingRequest;
import com.medsupport.booking_service.service.DonationBookingService;
import com.medsupport.booking_service.service.DonationSlotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GlobalExceptionHandlerTest {
    private final DonationBookingService bookingService = mock(DonationBookingService.class);
    private final DonationSlotService slotService = mock(DonationSlotService.class);
    private final IdentityClient identityClient = mock(IdentityClient.class);
    private final BloodRequestClient bloodRequestClient = mock(BloodRequestClient.class);
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        // Standalone MVC only: no Spring Boot context, sockets or external clients.
        mvc = MockMvcBuilders.standaloneSetup(
                        new DonationBookingController(bookingService, identityClient, bloodRequestClient),
                        new DonationSlotController(slotService, identityClient, bloodRequestClient))
                .setControllerAdvice(handler).build();
    }

    @Test
    void missingBookingFieldsReturnSafe400BeforeAnyServiceCall() throws Exception {
        mvc.perform(post("/api/donation-bookings")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.message").value(containsString("required")))
                .andExpect(content().string(not(containsString("com.medsupport"))))
                .andExpect(content().string(not(containsString("stackTrace"))));
        verifyNoInteractions(bookingService, slotService, identityClient, bloodRequestClient);
    }

    @Test
    void invalidBookingIdReturnsPublicValidationMessage() throws Exception {
        mvc.perform(post("/api/donation-bookings")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"donorProfileId":0,"bloodRequestId":11,"donationSlotId":9,
                                 "donationBookingDate":"2099-04-01","donationBookingTime":"10:00:00"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(
                        "donorProfileId: Donor profile ID must be positive"));
        verifyNoInteractions(bookingService, slotService, identityClient, bloodRequestClient);
    }

    @Test
    void pastBookingDateReturns400() throws Exception {
        mvc.perform(post("/api/donation-bookings")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"donorProfileId":107,"bloodRequestId":11,"donationSlotId":9,
                                 "donationBookingDate":"2000-01-01","donationBookingTime":"10:00:00"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "donationBookingDate: Booking date cannot be in the past"));
        verifyNoInteractions(bookingService, slotService, identityClient, bloodRequestClient);
    }

    @Test
    void invalidSlotCapacityReturns400BeforeAnyServiceCall() throws Exception {
        mvc.perform(post("/api/donation-slots")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"slotDate":"2099-04-01","startTime":"10:00:00",
                                 "endTime":"11:00:00","capacity":0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("capacity: Capacity must be at least 1"));
        verifyNoInteractions(bookingService, slotService, identityClient, bloodRequestClient);
    }

    @Test
    void missingSlotStartTimeReturns400() throws Exception {
        mvc.perform(post("/api/donation-slots")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"slotDate":"2099-04-01","endTime":"11:00:00","capacity":2}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("startTime: Start time is required"));
        verifyNoInteractions(bookingService, slotService, identityClient, bloodRequestClient);
    }

    @Test
    void globalValidationErrorUsesGenericFallback() throws Exception {
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(
                new DonationBookingRequest(), "request");
        binding.addError(new ObjectError("request", "test-only internal detail"));
        MethodParameter parameter = new MethodParameter(
                DonationBookingController.class.getMethod("createBooking",
                        String.class, DonationBookingRequest.class), 1);
        var response = handler.handleValidationException(
                new MethodArgumentNotValidException(parameter, binding));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().get("status"));
        assertEquals("Invalid request", response.getBody().get("message"));
    }

    @Test
    void preservesBusinessRuleConflictStatusAndMessage() {
        var response = handler.handleResponseStatusException(
                new ResponseStatusException(HttpStatus.CONFLICT, "Donation slot is full"));
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().get("status"));
        assertEquals("Donation slot is full", response.getBody().get("message"));
    }

    @Test
    void unexpectedFailuresDoNotExposeExceptionDetails() {
        var response = handler.handleGeneralException(
                new IllegalStateException("test-only internal database detail"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, response.getBody().get("status"));
        assertEquals("An unexpected error occurred", response.getBody().get("message"));
        assertFalse(response.getBody().toString().contains("database detail"));
    }
}
