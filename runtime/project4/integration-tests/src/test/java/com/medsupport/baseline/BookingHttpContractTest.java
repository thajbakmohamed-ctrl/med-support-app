package com.medsupport.baseline;

import com.medsupport.booking_service.client.*;
import com.medsupport.booking_service.controller.*;
import com.medsupport.booking_service.exception.GlobalExceptionHandler;
import com.medsupport.booking_service.model.DonationSlot;
import com.medsupport.booking_service.response.DonationBookingResponse;
import com.medsupport.booking_service.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BookingHttpContractTest {
    final DonationBookingService bookings=mock(DonationBookingService.class);
    final DonationSlotService slots=mock(DonationSlotService.class);
    final IdentityClient identity=mock(IdentityClient.class);
    final BloodRequestClient blood=mock(BloodRequestClient.class);
    final LocalDate date=LocalDate.of(2099,4,1);
    final DonationBookingResponse response=new DonationBookingResponse(7L,2L,3L,4L,date,LocalTime.of(10,0),"PENDING",null,null);
    final String bearer="Bearer synthetic-contract-token";
    MockMvc mvc;

    @BeforeEach void setup() {
        mvc=MockMvcBuilders.standaloneSetup(new DonationBookingController(bookings,identity,blood),new DonationSlotController(slots,identity,blood))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        lenient().when(identity.getMyDonorProfile(bearer)).thenReturn(new IdentityClient.DonorProfileInfo(2L));
        lenient().when(identity.getMyHospitalStaffInfo(bearer)).thenReturn(new IdentityClient.HospitalStaffInfo(5L));
        lenient().when(identity.getAuthenticatedEmail(bearer)).thenReturn("synthetic@example.invalid");
        var info=new BloodRequestClient.BloodRequestInfo(3L,5L,date,"OPEN");
        lenient().when(blood.getBloodRequest(3L,bearer)).thenReturn(info);
        lenient().when(blood.getBloodRequestForStaff(3L,bearer)).thenReturn(info);
    }
    @Test void createAcceptsExistingPayloadAndReturns201WithExactPublicFields() throws Exception {
        when(bookings.createBooking(any(),eq(2L),eq(5L),eq(date),eq("OPEN"),eq("synthetic@example.invalid"))).thenReturn(response);
        String body=mvc.perform(post("/api/donation-bookings").header("Authorization",bearer).contentType(MediaType.APPLICATION_JSON).content("""
                {"donorProfileId":2,"bloodRequestId":3,"donationSlotId":4,
                 "donationBookingDate":"2099-04-01","donationBookingTime":"10:00:00"}
                """)).andExpect(status().isCreated()).andExpect(jsonPath("$.donationBookingStatus").value("PENDING"))
                .andExpect(jsonPath("$.donationBookingDate").value("2099-04-01")).andReturn().getResponse().getContentAsString();
        Set<String> fields=new HashSet<>(JsonMapper.builder().build().readTree(body).propertyNames());
        assertEquals(Set.of("id","donorProfileId","bloodRequestId","donationSlotId","donationBookingDate","donationBookingTime","donationBookingStatus","createdAt","updatedAt"),fields);
        verify(bookings).createBooking(argThat(r->r.getDonorProfileId()==2L && r.getBloodRequestId()==3L && r.getDonationSlotId()==4L && r.getDonationBookingTime().equals(LocalTime.of(10,0))),eq(2L),eq(5L),eq(date),eq("OPEN"),eq("synthetic@example.invalid"));
    }
    @Test void donorListIsArrayAtMyRoute() throws Exception {
        when(bookings.getBookingsByDonor(2L)).thenReturn(List.of(response));
        mvc.perform(get("/api/donation-bookings/my").header("Authorization",bearer)).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(7));
    }
    @Test void cancellationUsesDeleteAndReturns200WithBody() throws Exception {
        when(bookings.cancelBooking(7L,2L,"synthetic@example.invalid")).thenReturn(response);
        mvc.perform(delete("/api/donation-bookings/7").header("Authorization",bearer)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(7));
        verify(bookings).cancelBooking(7L,2L,"synthetic@example.invalid");
    }
    @Test void staffStatusUsesStatusParameterAndDonorContactLookup() throws Exception {
        when(bookings.getDonorProfileIdForStaffNotification(7L,5L)).thenReturn(2L);
        when(identity.getDonorEmail(2L)).thenReturn("donor@example.invalid");
        when(bookings.updateBookingStatus(7L,"CONFIRMED",5L,"synthetic@example.invalid",2L,"donor@example.invalid")).thenReturn(response);
        mvc.perform(put("/api/donation-bookings/7/status").param("status","CONFIRMED").header("Authorization",bearer)).andExpect(status().isOk());
        verify(bookings).updateBookingStatus(7L,"CONFIRMED",5L,"synthetic@example.invalid",2L,"donor@example.invalid");
    }
    @Test void hospitalReviewUsesRequestRouteAndRejectsOtherHospital() throws Exception {
        when(bookings.getBookingsByBloodRequest(3L)).thenReturn(List.of(response));
        mvc.perform(get("/api/donation-bookings/blood-request/3").header("Authorization",bearer)).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(7));
        when(blood.getBloodRequestForStaff(3L,bearer)).thenReturn(new BloodRequestClient.BloodRequestInfo(3L,99L,date,"OPEN"));
        mvc.perform(get("/api/donation-bookings/blood-request/3").header("Authorization",bearer)).andExpect(status().isForbidden());
    }
    @Test void missingBearerAndInvalidPayloadDoNotReachBookingService() throws Exception {
        mvc.perform(get("/api/donation-bookings/my")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/donation-bookings").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest());
        verifyNoInteractions(bookings);
    }
    @Test void slotRoutesKeepExistingShapeAndCapacityParameter() throws Exception {
        DonationSlot slot=new DonationSlot(); slot.setId(4L); slot.setHospitalId(5L); slot.setSlotDate(date);
        slot.setStartTime(LocalTime.of(9,0)); slot.setEndTime(LocalTime.of(12,0)); slot.setCapacity(3); slot.setReservedCount(1); slot.setStatus("OPEN");
        when(slots.getAvailableSlotsForBloodRequest(5L,date)).thenReturn(List.of(slot));
        when(slots.getSlotsByHospital(5L)).thenReturn(List.of(slot));
        when(slots.updateCapacity(4L,3,5L)).thenReturn(slot);
        when(slots.createSlot(eq(5L),eq(date),any(),any(),eq(3))).thenReturn(slot);
        mvc.perform(get("/api/donation-slots/available").param("bloodRequestId","3").header("Authorization",bearer)).andExpect(status().isOk()).andExpect(jsonPath("$[0].reservedCount").value(1));
        mvc.perform(get("/api/donation-slots/my-hospital").header("Authorization",bearer)).andExpect(status().isOk()).andExpect(jsonPath("$[0].hospitalId").value(5));
        mvc.perform(put("/api/donation-slots/4/capacity").param("capacity","3").header("Authorization",bearer)).andExpect(status().isOk());
        mvc.perform(post("/api/donation-slots").header("Authorization",bearer).contentType(MediaType.APPLICATION_JSON).content("""
                {"slotDate":"2099-04-01","startTime":"09:00:00","endTime":"12:00:00","capacity":3}
                """)).andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(4));
    }
}
