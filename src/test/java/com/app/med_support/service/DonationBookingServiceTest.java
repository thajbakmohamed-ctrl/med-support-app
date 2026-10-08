package com.app.med_support.service;

import com.app.med_support.model.BloodRequest;
import com.app.med_support.model.DonationBooking;
import com.app.med_support.model.DonorProfile;
import com.app.med_support.model.User;
import com.app.med_support.repository.BloodRequestRepository;
import com.app.med_support.repository.DonationBookingRepository;
import com.app.med_support.repository.DonorProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;

    public class DonationBookingServiceTest {

        @Mock
        private DonationBookingRepository donationBookingRepository;

        @Mock
        private BloodRequestRepository bloodRequestRepository;

        @Mock
        private DonorProfileRepository donorProfileRepository;

        @Mock
        private BookingNotificationService bookingNotificationService;

        @Mock
        private AuditLogService auditLogService;

        private DonationBookingService donationBookingService;

        @BeforeEach
        void setUp() {MockitoAnnotations.openMocks(this);

            donationBookingService = new DonationBookingService(donationBookingRepository,
                    bloodRequestRepository, donorProfileRepository,
                    bookingNotificationService, auditLogService);
        }
        @Test
        void shouldReturnNullWhenDonorProfileDoesNotExist() {
            DonationBooking booking = new DonationBooking();
            DonorProfile donorProfile = new DonorProfile();
            donorProfile.setId(999L);
            booking.setDonorProfile(donorProfile);
            DonationBooking result = donationBookingService.createDonationBooking(booking);
            assertNull(result);
        }
        @Test
        void shouldReturnNullWhenBloodRequestDoesNotExist() {
            DonationBooking booking = new DonationBooking();
            DonorProfile donorProfile = new DonorProfile();
            donorProfile.setId(1L);
            booking.setDonorProfile(donorProfile);
            BloodRequest bloodRequest = new BloodRequest();
            bloodRequest.setId(999L);
            booking.setBloodRequest(bloodRequest);
            when(donorProfileRepository.findById(1L)).thenReturn(java.util.Optional.of(donorProfile));
            DonationBooking result = donationBookingService.createDonationBooking(booking);

            assertNull(result);
        }
        @Test
        void shouldReturnNullWhenBloodRequestIsNotOpen() {
            DonationBooking booking = new DonationBooking();

            DonorProfile donorProfile = new DonorProfile();
            donorProfile.setId(1L);

            BloodRequest bloodRequest = new BloodRequest();
            bloodRequest.setId(1L);
            bloodRequest.setBloodRequestStatus("CLOSED");

            booking.setDonorProfile(donorProfile);
            booking.setBloodRequest(bloodRequest);

            when(donorProfileRepository.findById(1L)).thenReturn(java.util.Optional.of(donorProfile));

            when(bloodRequestRepository.findById(1L)).thenReturn(java.util.Optional.of(bloodRequest));

            DonationBooking result = donationBookingService.createDonationBooking(booking);

            assertNull(result);
        }
        @Test
        void shouldReturnNullWhenBookingDateIsInThePast() {
            DonationBooking booking = new DonationBooking();
            DonorProfile donorProfile = new DonorProfile();
            donorProfile.setId(1L);
            BloodRequest bloodRequest = new BloodRequest();
            bloodRequest.setId(1L);
            bloodRequest.setBloodRequestStatus("OPEN");
            bloodRequest.setBloodNeededDate(LocalDate.now().plusDays(5));

            booking.setDonorProfile(donorProfile);
            booking.setBloodRequest(bloodRequest);

            // Booking date is in the past
            booking.setDonationBookingDate(LocalDate.now().minusDays(1));
            booking.setDonationBookingTime(LocalTime.of(10, 0));

            when(donorProfileRepository.findById(1L)).thenReturn(java.util.Optional.of(donorProfile));

            when(bloodRequestRepository.findById(1L)).thenReturn(java.util.Optional.of(bloodRequest));

            DonationBooking result = donationBookingService.createDonationBooking(booking);
            assertNull(result);
        }
        @Test
        void shouldReturnNullWhenBookingDateIsAfterBloodNeededDate() {
            DonationBooking booking = new DonationBooking();

            DonorProfile donorProfile = new DonorProfile();
            donorProfile.setId(1L);

            BloodRequest bloodRequest = new BloodRequest();
            bloodRequest.setId(1L);
            bloodRequest.setBloodRequestStatus("OPEN");

            // Blood is needed after 3 days
            bloodRequest.setBloodNeededDate(LocalDate.now().plusDays(3));

            booking.setDonorProfile(donorProfile);
            booking.setBloodRequest(bloodRequest);

            // Booking is after the blood needed date
            booking.setDonationBookingDate(LocalDate.now().plusDays(5));
            booking.setDonationBookingTime(LocalTime.of(10, 0));

            when(donorProfileRepository.findById(1L)).thenReturn(java.util.Optional.of(donorProfile));

            when(bloodRequestRepository.findById(1L)).thenReturn(java.util.Optional.of(bloodRequest));

            DonationBooking result = donationBookingService.createDonationBooking(booking);

            assertNull(result);
        }
        @Test
        void shouldReturnNullWhenDonorAlreadyHasBookingAtSameDateAndTime() {

            DonationBooking booking = new DonationBooking();

            DonorProfile donorProfile = new DonorProfile();
            donorProfile.setId(1L);

            BloodRequest bloodRequest = new BloodRequest();
            bloodRequest.setId(1L);
            bloodRequest.setBloodRequestStatus("OPEN");
            bloodRequest.setBloodNeededDate(LocalDate.now().plusDays(5));

            booking.setDonorProfile(donorProfile);
            booking.setBloodRequest(bloodRequest);
            booking.setDonationBookingDate(LocalDate.now().plusDays(2));
            booking.setDonationBookingTime(LocalTime.of(10, 0));

            when(donorProfileRepository.findById(1L)).thenReturn(java.util.Optional.of(donorProfile));

            when(bloodRequestRepository.findById(1L)).thenReturn(java.util.Optional.of(bloodRequest));

            when(donationBookingRepository.existsByDonorProfileIdAndDonationBookingDateAndDonationBookingTimeAndDonationBookingStatusNot(
           1L, booking.getDonationBookingDate(), booking.getDonationBookingTime(),
                    "CANCELLED")).thenReturn(true);

            DonationBooking result = donationBookingService.createDonationBooking(booking);
            assertNull(result);
        }
        @Test
        void shouldCreateBookingSuccessfullyWhenDataIsValid() {
            DonationBooking booking = new DonationBooking();

            DonorProfile donorProfile = new DonorProfile();
            donorProfile.setId(1L);

            BloodRequest bloodRequest = new BloodRequest();
            bloodRequest.setId(1L);
            bloodRequest.setBloodRequestStatus("OPEN");
            bloodRequest.setBloodNeededDate(LocalDate.now().plusDays(5));

            booking.setDonorProfile(donorProfile);
            booking.setBloodRequest(bloodRequest);
            booking.setDonationBookingDate(LocalDate.now().plusDays(2));
            booking.setDonationBookingTime(LocalTime.of(10, 0));

            when(donorProfileRepository.findById(1L)).thenReturn(java.util.Optional.of(donorProfile));

            when(bloodRequestRepository.findById(1L)).thenReturn(java.util.Optional.of(bloodRequest));

            when(donationBookingRepository.existsByDonorProfileIdAndDonationBookingDateAndDonationBookingTimeAndDonationBookingStatusNot(
                    1L, booking.getDonationBookingDate(), booking.getDonationBookingTime(),
                    "CANCELLED")).thenReturn(false);

            when(donationBookingRepository.save(booking)).thenReturn(booking);

            DonationBooking result = donationBookingService.createDonationBooking(booking);

            assertNotNull(result);
            assertEquals("PENDING", result.getDonationBookingStatus());
        }
        @Test
        void shouldCancelDonationBookingSuccessfully() {

            DonorProfile donorProfile = new DonorProfile();
            donorProfile.setId(1L);
            User user = new User();
            user.setEmail("donor@test.com");

            donorProfile.setUser(user);

            DonationBooking booking = new DonationBooking();
            booking.setId(1L);
            booking.setDonorProfile(donorProfile);
            booking.setDonationBookingStatus("PENDING");

            when(donationBookingRepository.findById(1L)).thenReturn(java.util.Optional.of(booking));

            when(donationBookingRepository.save(booking)).thenReturn(booking);

            DonationBooking result = donationBookingService.cancelDonationBooking(1L, 1L);

            assertNotNull(result);
            assertEquals("CANCELLED", result.getDonationBookingStatus());
        }
    }
