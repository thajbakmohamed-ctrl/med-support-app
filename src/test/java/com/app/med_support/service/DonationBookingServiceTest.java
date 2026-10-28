
package com.app.med_support.service;

import com.app.med_support.model.BloodRequest;
import com.app.med_support.model.DonationBooking;
import com.app.med_support.model.DonationSlot;
import com.app.med_support.model.DonorProfile;
import com.app.med_support.model.Hospital;
import com.app.med_support.model.User;

import com.app.med_support.repository.BloodRequestRepository;
import com.app.med_support.repository.DonationBookingRepository;
import com.app.med_support.repository.DonationSlotRepository;
import com.app.med_support.repository.DonorProfileRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class DonationBookingServiceTest {

    @Mock
    private DonationBookingRepository donationBookingRepository;

    @Mock
    private BloodRequestRepository bloodRequestRepository;

    @Mock
    private DonorProfileRepository donorProfileRepository;

    @Mock
    private DonationSlotRepository donationSlotRepository;

    @Mock
    private BookingNotificationService bookingNotificationService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private EmailService emailService;

    @Mock
    private ExecutorService reminderExecutor;

    private DonationBookingService donationBookingService;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {

        // Initialize Mockito mocks
        mocks = MockitoAnnotations.openMocks(this);

        // Simulate transaction synchronization for unit tests
        TransactionSynchronizationManager.initSynchronization();

        donationBookingService = new DonationBookingService(
                donationBookingRepository,
                bloodRequestRepository,
                donorProfileRepository,
                bookingNotificationService,
                auditLogService,
                donationSlotRepository,
                emailService,
                reminderExecutor
        );
    }

    @AfterEach
    void tearDown() throws Exception {

        TransactionSynchronizationManager.clearSynchronization();

        mocks.close();
    }

    @Test
    void shouldRejectBookingWhenDonorProfileDoesNotExist() {

        DonationBooking booking = new DonationBooking();

        DonorProfile donorProfile = new DonorProfile();
        donorProfile.setId(999L);

        booking.setDonorProfile(donorProfile);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> donationBookingService.createDonationBooking(booking)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void shouldRejectBookingWhenBloodRequestDoesNotExist() {

        DonationBooking booking = new DonationBooking();

        DonorProfile donorProfile = new DonorProfile();
        donorProfile.setId(1L);

        booking.setDonorProfile(donorProfile);

        BloodRequest bloodRequest = new BloodRequest();
        bloodRequest.setId(999L);

        booking.setBloodRequest(bloodRequest);

        when(donorProfileRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(donorProfile));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> donationBookingService.createDonationBooking(booking)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void shouldRejectBookingWhenBloodRequestIsNotOpen() {

        DonationBooking booking = new DonationBooking();

        DonorProfile donorProfile = new DonorProfile();
        donorProfile.setId(1L);

        BloodRequest bloodRequest = new BloodRequest();
        bloodRequest.setId(1L);
        bloodRequest.setBloodRequestStatus("CLOSED");

        booking.setDonorProfile(donorProfile);
        booking.setBloodRequest(bloodRequest);

        when(donorProfileRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(donorProfile));

        when(bloodRequestRepository.findById(1L))
                .thenReturn(Optional.of(bloodRequest));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> donationBookingService.createDonationBooking(booking)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void shouldRejectBookingWhenDateIsInThePast() {

        DonationBooking booking = new DonationBooking();

        DonorProfile donorProfile = new DonorProfile();
        donorProfile.setId(1L);

        BloodRequest bloodRequest = new BloodRequest();
        bloodRequest.setId(1L);
        bloodRequest.setBloodRequestStatus("OPEN");
        bloodRequest.setBloodNeededDate(LocalDate.now().plusDays(5));

        booking.setDonorProfile(donorProfile);
        booking.setBloodRequest(bloodRequest);

        booking.setDonationBookingDate(LocalDate.now().minusDays(1));
        booking.setDonationBookingTime(LocalTime.of(10, 0));

        when(donorProfileRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(donorProfile));

        when(bloodRequestRepository.findById(1L))
                .thenReturn(Optional.of(bloodRequest));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> donationBookingService.createDonationBooking(booking)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void shouldRejectBookingWhenDateIsAfterBloodNeededDate() {

        DonationBooking booking = new DonationBooking();

        DonorProfile donorProfile = new DonorProfile();
        donorProfile.setId(1L);

        BloodRequest bloodRequest = new BloodRequest();
        bloodRequest.setId(1L);
        bloodRequest.setBloodRequestStatus("OPEN");
        bloodRequest.setBloodNeededDate(LocalDate.now().plusDays(3));

        booking.setDonorProfile(donorProfile);
        booking.setBloodRequest(bloodRequest);

        booking.setDonationBookingDate(LocalDate.now().plusDays(5));
        booking.setDonationBookingTime(LocalTime.of(10, 0));

        when(donorProfileRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(donorProfile));

        when(bloodRequestRepository.findById(1L))
                .thenReturn(Optional.of(bloodRequest));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> donationBookingService.createDonationBooking(booking)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void shouldRejectDuplicateBookingAtSameDateAndTime() {

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

        when(donorProfileRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(donorProfile));

        when(bloodRequestRepository.findById(1L))
                .thenReturn(Optional.of(bloodRequest));

        when(donationBookingRepository
                .existsByDonorProfileIdAndDonationBookingDateAndDonationBookingTimeAndDonationBookingStatusNot(
                        1L,
                        booking.getDonationBookingDate(),
                        booking.getDonationBookingTime(),
                        "CANCELLED"
                )).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> donationBookingService.createDonationBooking(booking)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void shouldCreateBookingSuccessfullyWhenDataIsValid() {

        DonationBooking booking = new DonationBooking();

        DonorProfile donorProfile = new DonorProfile();
        donorProfile.setId(1L);

        User user = new User();
        user.setEmail("donor@test.com");

        donorProfile.setUser(user);

        Hospital hospital = new Hospital();
        hospital.setId(1L);

        BloodRequest bloodRequest = new BloodRequest();
        bloodRequest.setId(1L);
        bloodRequest.setBloodRequestStatus("OPEN");
        bloodRequest.setBloodNeededDate(LocalDate.now().plusDays(5));
        bloodRequest.setHospital(hospital);

        DonationSlot slot = new DonationSlot();

        slot.setId(10L);
        slot.setHospital(hospital);
        slot.setSlotDate(LocalDate.now().plusDays(2));
        slot.setStartTime(LocalTime.of(9, 0));
        slot.setEndTime(LocalTime.of(11, 0));
        slot.setCapacity(2);
        slot.setReservedCount(0);
        slot.setStatus("OPEN");

        booking.setDonorProfile(donorProfile);
        booking.setBloodRequest(bloodRequest);
        booking.setDonationSlot(slot);

        booking.setDonationBookingDate(slot.getSlotDate());
        booking.setDonationBookingTime(LocalTime.of(10, 0));

        when(donorProfileRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(donorProfile));

        when(bloodRequestRepository.findById(1L))
                .thenReturn(Optional.of(bloodRequest));

        when(donationBookingRepository
                .existsByDonorProfileIdAndDonationBookingDateAndDonationBookingTimeAndDonationBookingStatusNot(
                        1L,
                        booking.getDonationBookingDate(),
                        booking.getDonationBookingTime(),
                        "CANCELLED"
                )).thenReturn(false);

        when(donationSlotRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(slot));

        when(donationBookingRepository.save(booking))
                .thenReturn(booking);

        DonationBooking result =
                donationBookingService.createDonationBooking(booking);

        assertNotNull(result);
        assertEquals("PENDING", result.getDonationBookingStatus());
        assertEquals(1, slot.getReservedCount());
        assertEquals(10L, result.getDonationSlot().getId());
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

        // Legacy booking without a donation slot
        when(donationBookingRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(booking));

        when(donationBookingRepository.save(booking))
                .thenReturn(booking);

        DonationBooking result =
                donationBookingService.cancelDonationBooking(1L, 1L);

        assertNotNull(result);
        assertEquals("CANCELLED", result.getDonationBookingStatus());

        verify(donationBookingRepository).save(booking);
    }

    @Test
    void shouldRejectBookingWhenSlotIsFull() {

        DonorProfile donorProfile = new DonorProfile();
        donorProfile.setId(1L);

        BloodRequest bloodRequest = new BloodRequest();
        bloodRequest.setId(1L);
        bloodRequest.setBloodRequestStatus("OPEN");
        bloodRequest.setBloodNeededDate(LocalDate.now().plusDays(5));

        Hospital hospital = new Hospital();
        hospital.setId(1L);

        bloodRequest.setHospital(hospital);

        DonationSlot slot = new DonationSlot();

        slot.setId(10L);
        slot.setHospital(hospital);
        slot.setSlotDate(LocalDate.now().plusDays(2));
        slot.setStartTime(LocalTime.of(9, 0));
        slot.setEndTime(LocalTime.of(11, 0));
        slot.setCapacity(2);
        slot.setReservedCount(2);
        slot.setStatus("OPEN");

        DonationBooking booking = new DonationBooking();

        booking.setDonorProfile(donorProfile);
        booking.setBloodRequest(bloodRequest);
        booking.setDonationSlot(slot);

        booking.setDonationBookingDate(slot.getSlotDate());
        booking.setDonationBookingTime(LocalTime.of(10, 0));

        when(donorProfileRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(donorProfile));

        when(bloodRequestRepository.findById(1L))
                .thenReturn(Optional.of(bloodRequest));

        when(donationBookingRepository
                .existsByDonorProfileIdAndDonationBookingDateAndDonationBookingTimeAndDonationBookingStatusNot(
                        1L,
                        booking.getDonationBookingDate(),
                        booking.getDonationBookingTime(),
                        "CANCELLED"
                )).thenReturn(false);

        when(donationSlotRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(slot));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> donationBookingService.createDonationBooking(booking)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(2, slot.getReservedCount());

        verify(donationBookingRepository, never())
                .save(any(DonationBooking.class));
    }

    @Test
    void shouldReleaseSlotWhenBookingIsCancelled() {

        DonorProfile donorProfile = new DonorProfile();
        donorProfile.setId(1L);

        User user = new User();
        user.setEmail("donor@test.com");

        donorProfile.setUser(user);

        DonationBooking booking = new DonationBooking();

        booking.setId(1L);
        booking.setDonorProfile(donorProfile);
        booking.setDonationBookingStatus("PENDING");

        DonationSlot slot = new DonationSlot();

        slot.setId(10L);
        slot.setCapacity(2);
        slot.setReservedCount(2);
        slot.setStatus("OPEN");

        booking.setDonationSlot(slot);

        when(donationBookingRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(booking));

        when(donationSlotRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(slot));

        when(donationBookingRepository.save(booking))
                .thenReturn(booking);

        DonationBooking result =
                donationBookingService.cancelDonationBooking(1L, 1L);

        assertNotNull(result);
        assertEquals("CANCELLED", result.getDonationBookingStatus());
        assertEquals(1, slot.getReservedCount());

        verify(donationBookingRepository).save(booking);
    }

    @Test
    void shouldNotReleaseSlotTwiceWhenBookingAlreadyCancelled() {

        DonorProfile donorProfile = new DonorProfile();
        donorProfile.setId(1L);

        User user = new User();
        user.setEmail("donor@test.com");

        donorProfile.setUser(user);

        DonationSlot slot = new DonationSlot();

        slot.setId(10L);
        slot.setCapacity(2);
        slot.setReservedCount(2);
        slot.setStatus("OPEN");

        DonationBooking booking = new DonationBooking();

        booking.setId(1L);
        booking.setDonorProfile(donorProfile);
        booking.setDonationSlot(slot);
        booking.setDonationBookingStatus("PENDING");

        when(donationBookingRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(booking));

        when(donationSlotRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(slot));

        when(donationBookingRepository.save(booking))
                .thenReturn(booking);

        DonationBooking firstResult =
                donationBookingService.cancelDonationBooking(1L, 1L);

        assertNotNull(firstResult);
        assertEquals("CANCELLED", firstResult.getDonationBookingStatus());
        assertEquals(1, slot.getReservedCount());


        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> donationBookingService.cancelDonationBooking(1L, 1L)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(1, slot.getReservedCount());

        verify(donationBookingRepository, times(1)).save(booking);
        verify(donationSlotRepository, times(1)).findByIdForUpdate(10L);
    }

    @Test
    void shouldRejectBookingWhenSlotIsClosed() {

        DonorProfile donor = new DonorProfile();
        donor.setId(1L);

        Hospital hospital = new Hospital();
        hospital.setId(3L);

        BloodRequest bloodRequest = new BloodRequest();

        bloodRequest.setId(9L);
        bloodRequest.setHospital(hospital);
        bloodRequest.setBloodRequestStatus("OPEN");
        bloodRequest.setBloodNeededDate(LocalDate.now().plusDays(7));

        DonationSlot slot = new DonationSlot();

        slot.setId(2L);
        slot.setHospital(hospital);
        slot.setSlotDate(LocalDate.now().plusDays(3));
        slot.setStartTime(LocalTime.of(13, 0));
        slot.setEndTime(LocalTime.of(15, 0));
        slot.setCapacity(3);
        slot.setReservedCount(0);
        slot.setStatus("CLOSED");

        DonationBooking booking = new DonationBooking();

        booking.setDonorProfile(donor);
        booking.setBloodRequest(bloodRequest);
        booking.setDonationSlot(slot);

        booking.setDonationBookingDate(slot.getSlotDate());
        booking.setDonationBookingTime(slot.getStartTime());

        when(donorProfileRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(donor));

        when(bloodRequestRepository.findById(9L))
                .thenReturn(Optional.of(bloodRequest));

        when(donationSlotRepository.findByIdForUpdate(2L))
                .thenReturn(Optional.of(slot));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> donationBookingService.createDonationBooking(booking)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals(0, slot.getReservedCount());

        verify(donationBookingRepository, never())
                .save(any(DonationBooking.class));
    }
}
