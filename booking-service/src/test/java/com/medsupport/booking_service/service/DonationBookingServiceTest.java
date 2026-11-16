
package com.medsupport.booking_service.service;

import com.medsupport.booking_service.model.DonationBooking;
import com.medsupport.booking_service.model.DonationSlot;
import com.medsupport.booking_service.repository.DonationBookingRepository;
import com.medsupport.booking_service.repository.DonationSlotRepository;
import com.medsupport.booking_service.request.DonationBookingRequest;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DonationBookingServiceTest {

    private static final Long DONOR_ID = 5L;
    private static final Long HOSPITAL_ID = 2L;
    private static final Long BLOOD_REQUEST_ID = 10L;
    private static final Long SLOT_ID = 1L;

    private static final String EMAIL = "donor@example.com";
    private static final String STAFF_EMAIL = "staff@example.com";

    @Mock
    private DonationBookingRepository bookingRepository;

    @Mock
    private DonationSlotRepository slotRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private BookingOutboxService bookingOutboxService;

    @Mock
    private EntityManager entityManager;

    @Mock
    private Query advisoryLockQuery;

    @Mock
    private Query bloodRequestLockQuery;

    @Mock
    private Query completionUpdateQuery;

    @InjectMocks
    private DonationBookingService bookingService;

    private DonationBookingRequest request;

    @BeforeEach
    void setUp() {

        ReflectionTestUtils.setField(
                bookingService,
                "entityManager",
                entityManager
        );

        request = mock(DonationBookingRequest.class);

        lenient().when(request.getDonationSlotId())
                .thenReturn(SLOT_ID);

        lenient().when(request.getBloodRequestId())
                .thenReturn(BLOOD_REQUEST_ID);

        lenient().when(request.getDonorProfileId())
                .thenReturn(DONOR_ID);

        // Mock PostgreSQL advisory lock.
        lenient().doReturn(advisoryLockQuery)
                .when(entityManager)
                .createNativeQuery(
                        "SELECT pg_advisory_xact_lock(:donorId)"
                );

        lenient().when(
                advisoryLockQuery.setParameter(
                        eq("donorId"),
                        any()
                )
        ).thenReturn(advisoryLockQuery);

        // Mock blood request SELECT ... FOR UPDATE.
        lenient().doReturn(bloodRequestLockQuery)
                .when(entityManager)
                .createNativeQuery(argThat(sql ->
                        sql != null
                                && sql.contains("FROM blood_request")
                                && sql.contains("FOR UPDATE")
                ));

        lenient().when(
                bloodRequestLockQuery.setParameter(
                        eq("requestId"),
                        any()
                )
        ).thenReturn(bloodRequestLockQuery);

        mockBloodRequestForCreation(
                LocalDate.now().plusDays(3),
                "OPEN"
        );
    }

    // Helper: Mock blood request for booking creation.
    private void mockBloodRequestForCreation(
            LocalDate neededDate,
            String status
    ) {

        lenient().when(bloodRequestLockQuery.getResultList())
                .thenReturn(Collections.singletonList(
                        new Object[]{
                                HOSPITAL_ID,
                                Date.valueOf(neededDate),
                                status
                        }
                ));
    }

    // Helper: Mock blood request for cancellation.
    private void mockBloodRequestForCancellation() {

        lenient().when(bloodRequestLockQuery.getResultList())
                .thenReturn(Collections.singletonList(
                        BLOOD_REQUEST_ID
                ));
    }

    // Helper: Prepare a valid donation slot.
    private DonationSlot mockValidSlot(
            LocalDate date,
            LocalTime time
    ) {

        DonationSlot slot = mock(DonationSlot.class);

        when(slotRepository.findByIdForUpdate(SLOT_ID))
                .thenReturn(Optional.of(slot));

        lenient().when(slot.getHospitalId())
                .thenReturn(HOSPITAL_ID);

        lenient().when(slot.getSlotDate())
                .thenReturn(date);

        lenient().when(slot.getStartTime())
                .thenReturn(time.minusHours(1));

        lenient().when(slot.getEndTime())
                .thenReturn(time.plusHours(2));

        lenient().when(slot.getStatus())
                .thenReturn("OPEN");

        return slot;
    }

    // Helper: Prepare an existing booking for cancellation.
    private DonationBooking mockExistingBooking(
            Long bookingId,
            String status,
            int reservedCount
    ) {

        mockBloodRequestForCancellation();

        DonationBooking booking = mock(DonationBooking.class);
        DonationSlot slot = mock(DonationSlot.class);

        when(bookingRepository.findById(bookingId))
                .thenReturn(Optional.of(booking));

        lenient().when(booking.getDonorProfileId())
                .thenReturn(DONOR_ID);

        lenient().when(booking.getBloodRequestId())
                .thenReturn(BLOOD_REQUEST_ID);

        lenient().when(booking.getDonationSlot())
                .thenReturn(slot);

        lenient().when(slot.getId())
                .thenReturn(SLOT_ID);

        when(slotRepository.findByIdForUpdate(SLOT_ID))
                .thenReturn(Optional.of(slot));

        when(bookingRepository.findByIdForUpdate(bookingId))
                .thenReturn(Optional.of(booking));

        when(booking.getDonationBookingStatus())
                .thenReturn(status);

        lenient().when(slot.getReservedCount())
                .thenReturn(reservedCount);

        return booking;
    }

    // Helper: Prepare booking for status updates.
    private DonationBooking mockStatusUpdateBooking(
            Long bookingId,
            String currentStatus,
            int requiredUnits,
            String requestStatus
    ) {

        DonationBooking booking = mock(DonationBooking.class);
        DonationSlot slot = mock(DonationSlot.class);

        when(bookingRepository.findById(bookingId))
                .thenReturn(Optional.of(booking));

        lenient().when(booking.getBloodRequestId())
                .thenReturn(BLOOD_REQUEST_ID);

        lenient().when(booking.getDonationSlot())
                .thenReturn(slot);

        lenient().when(slot.getId())
                .thenReturn(SLOT_ID);

        lenient().when(bloodRequestLockQuery.getResultList())
                .thenReturn(Collections.singletonList(
                        new Object[]{
                                HOSPITAL_ID,
                                requiredUnits,
                                requestStatus
                        }
                ));

        lenient().when(slotRepository.findByIdForUpdate(SLOT_ID))
                .thenReturn(Optional.of(slot));

        lenient().when(bookingRepository.findByIdForUpdate(bookingId))
                .thenReturn(Optional.of(booking));

        lenient().when(slot.getHospitalId())
                .thenReturn(HOSPITAL_ID);

        lenient().when(booking.getDonationBookingStatus())
                .thenReturn(currentStatus);

        lenient().when(bookingRepository.saveAndFlush(booking))
                .thenReturn(booking);

        lenient().when(booking.getDonorProfileId())
                .thenReturn(DONOR_ID);

        lenient().when(booking.getId())
                .thenReturn(bookingId);

        return booking;
    }

    // TEST 1: Reject null booking requests.
    @Test
    void shouldRejectBookingWhenRequestIsNull() {

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.createBooking(
                        null,
                        DONOR_ID,
                        HOSPITAL_ID,
                        LocalDate.now().plusDays(5),
                        "OPEN",
                        EMAIL
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());

        verifyNoInteractions(
                auditLogService,
                bookingOutboxService
        );

        verifyNoInteractions(
                bookingRepository,
                slotRepository
        );
    }

    // TEST 2: Reject a closed blood request.
    @Test
    void shouldRejectBookingWhenBloodRequestIsClosed() {

        mockBloodRequestForCreation(
                LocalDate.now().plusDays(5),
                "CLOSED"
        );

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.createBooking(
                        request,
                        DONOR_ID,
                        HOSPITAL_ID,
                        LocalDate.now().plusDays(5),
                        "CLOSED",
                        EMAIL
                )
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());

        verify(bookingRepository, never())
                .saveAndFlush(any());

        verifyNoInteractions(
                auditLogService,
                bookingOutboxService
        );
    }

    // TEST 3: Reject booking for another donor.
    @Test
    void shouldRejectBookingForAnotherDonor() {

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.createBooking(
                        request,
                        99L,
                        HOSPITAL_ID,
                        LocalDate.now().plusDays(3),
                        "OPEN",
                        EMAIL
                )
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());

        verify(bookingRepository, never())
                .saveAndFlush(any());

        verifyNoInteractions(
                auditLogService,
                bookingOutboxService
        );
    }

    // TEST 4: Prevent duplicate bookings.
    @Test
    void shouldRejectDuplicateBooking() {

        LocalDate date = LocalDate.now().plusDays(2);
        LocalTime time = LocalTime.of(10, 0);

        when(request.getDonationBookingDate())
                .thenReturn(date);

        when(request.getDonationBookingTime())
                .thenReturn(time);

        DonationSlot slot = mockValidSlot(date, time);

        when(bookingRepository
                .existsByDonorProfileIdAndDonationBookingDateAndDonationBookingTimeAndDonationBookingStatusNot(
                        DONOR_ID,
                        date,
                        time,
                        "CANCELLED"
                ))
                .thenReturn(true);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.createBooking(
                        request,
                        DONOR_ID,
                        HOSPITAL_ID,
                        date.plusDays(1),
                        "OPEN",
                        EMAIL
                )
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());

        verify(bookingRepository, never())
                .saveAndFlush(any());

        verify(slot, never())
                .setReservedCount(anyInt());

        verifyNoInteractions(
                auditLogService,
                bookingOutboxService
        );
    }

    // TEST 5: Prevent booking when slot is full.
    @Test
    void shouldRejectBookingWhenSlotIsFull() {

        LocalDate date = LocalDate.now().plusDays(2);
        LocalTime time = LocalTime.of(10, 0);

        when(request.getDonationBookingDate())
                .thenReturn(date);

        when(request.getDonationBookingTime())
                .thenReturn(time);

        DonationSlot slot = mockValidSlot(date, time);

        when(slot.getReservedCount())
                .thenReturn(2);

        when(slot.getCapacity())
                .thenReturn(2);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.createBooking(
                        request,
                        DONOR_ID,
                        HOSPITAL_ID,
                        date.plusDays(1),
                        "OPEN",
                        EMAIL
                )
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());

        verify(bookingRepository, never())
                .saveAndFlush(any());

        verify(slot, never())
                .setReservedCount(anyInt());

        verifyNoInteractions(
                auditLogService,
                bookingOutboxService
        );
    }

    // TEST 6: Create booking, write audit log and outbox event.
    @Test
    void shouldCreateBookingSuccessfullyAndWriteAuditLog() {

        LocalDate date = LocalDate.now().plusDays(2);
        LocalTime time = LocalTime.of(10, 0);

        when(request.getDonationBookingDate())
                .thenReturn(date);

        when(request.getDonationBookingTime())
                .thenReturn(time);

        DonationSlot slot = mockValidSlot(date, time);

        when(slot.getReservedCount())
                .thenReturn(1);

        when(slot.getCapacity())
                .thenReturn(3);

        when(bookingRepository.saveAndFlush(
                any(DonationBooking.class)
        )).thenAnswer(invocation -> {

            DonationBooking savedBooking =
                    invocation.getArgument(0);

            ReflectionTestUtils.setField(
                    savedBooking,
                    "id",
                    100L
            );

            return savedBooking;
        });

        bookingService.createBooking(
                request,
                DONOR_ID,
                HOSPITAL_ID,
                date.plusDays(1),
                "OPEN",
                EMAIL
        );

        verify(slot).setReservedCount(2);

        verify(slotRepository).save(slot);

        verify(bookingRepository).saveAndFlush(
                argThat(booking ->
                        DONOR_ID.equals(booking.getDonorProfileId())
                                && BLOOD_REQUEST_ID.equals(
                                booking.getBloodRequestId()
                        )
                                && "PENDING".equals(
                                booking.getDonationBookingStatus()
                        )
                                && booking.getDonationSlot() == slot
                )
        );

        verify(auditLogService).saveAuditLog(
                eq("DONATION_BOOKING_CREATED"),
                eq(EMAIL),
                contains("created for blood request")
        );

        verify(bookingOutboxService).saveEvent(
                eq(100L),
                eq(EMAIL),
                eq("BOOKING_CREATED"),
                eq("Donation Booking Created"),
                eq("Your blood donation booking has been created successfully.")
        );
    }

    // TEST 7: Cancel booking and release slot capacity.
    @Test
    void shouldCancelBookingAndReleaseSlotCapacityAndWriteAuditLog() {

        Long bookingId = 100L;

        DonationBooking booking = mockExistingBooking(
                bookingId,
                "PENDING",
                2
        );

        when(booking.getId()).thenReturn(bookingId);

        DonationSlot slot = booking.getDonationSlot();

        when(bookingRepository.saveAndFlush(booking))
                .thenReturn(booking);

        bookingService.cancelBooking(
                bookingId,
                DONOR_ID,
                EMAIL
        );

        verify(booking)
                .setDonationBookingStatus("CANCELLED");

        verify(slot).setReservedCount(1);

        verify(slotRepository).save(slot);

        verify(bookingRepository)
                .saveAndFlush(booking);

        verify(auditLogService).saveAuditLog(
                eq("DONATION_BOOKING_CANCELLED"),
                eq(EMAIL),
                contains("cancelled by donor profile")
        );

        verify(bookingOutboxService).saveEvent(
                eq(bookingId),
                eq(EMAIL),
                eq("BOOKING_CANCELLED"),
                eq("Donation Booking Cancelled"),
                eq("Your blood donation booking has been cancelled.")
        );
    }

    // TEST 8: Reject cancellation of completed booking.
    @Test
    void shouldRejectCancellationOfCompletedBookingWithoutAudit() {

        Long bookingId = 100L;

        DonationBooking booking = mockExistingBooking(
                bookingId,
                "COMPLETED",
                2
        );

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.cancelBooking(
                        bookingId,
                        DONOR_ID,
                        EMAIL
                )
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());

        verify(booking, never())
                .setDonationBookingStatus(anyString());

        verify(slotRepository, never())
                .save(any());

        verify(bookingRepository, never())
                .saveAndFlush(any());

        verifyNoInteractions(
                auditLogService,
                bookingOutboxService
        );
    }

    // TEST 9: Reject cancellation of cancelled booking.
    @Test
    void shouldRejectCancellationOfAlreadyCancelledBookingWithoutAudit() {

        Long bookingId = 101L;

        DonationBooking booking = mockExistingBooking(
                bookingId,
                "CANCELLED",
                1
        );

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.cancelBooking(
                        bookingId,
                        DONOR_ID,
                        EMAIL
                )
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());

        verify(booking, never())
                .setDonationBookingStatus(anyString());

        verify(slotRepository, never())
                .save(any());

        verify(bookingRepository, never())
                .saveAndFlush(any());

        verifyNoInteractions(
                auditLogService,
                bookingOutboxService
        );
    }

    // TEST 10: Automatically complete blood request.
    @Test
    void shouldAutomaticallyCompleteBloodRequestAndWriteBothAuditLogs() {

        Long bookingId = 102L;

        DonationBooking booking = mockStatusUpdateBooking(
                bookingId,
                "CONFIRMED",
                3,
                "OPEN"
        );

        when(bookingRepository
                .countByBloodRequestIdAndDonationBookingStatus(
                        BLOOD_REQUEST_ID,
                        "COMPLETED"
                ))
                .thenReturn(3L);

        lenient().doReturn(completionUpdateQuery)
                .when(entityManager)
                .createNativeQuery(argThat(sql ->
                        sql != null
                                && sql.contains("UPDATE blood_request")
                ));

        when(completionUpdateQuery.setParameter(
                "requestId",
                BLOOD_REQUEST_ID
        )).thenReturn(completionUpdateQuery);

        when(completionUpdateQuery.executeUpdate())
                .thenReturn(1);

        bookingService.updateBookingStatus(
                bookingId,
                "COMPLETED",
                HOSPITAL_ID,
                STAFF_EMAIL,
                DONOR_ID,
                EMAIL
        );

        verify(booking)
                .setDonationBookingStatus("COMPLETED");

        verify(bookingRepository)
                .saveAndFlush(booking);

        verify(bookingRepository)
                .countByBloodRequestIdAndDonationBookingStatus(
                        BLOOD_REQUEST_ID,
                        "COMPLETED"
                );

        verify(completionUpdateQuery).executeUpdate();

        verify(auditLogService).saveAuditLog(
                eq("BLOOD_REQUEST_COMPLETED"),
                eq(STAFF_EMAIL),
                contains("automatically completed")
        );

        verify(auditLogService).saveAuditLog(
                eq("DONATION_BOOKING_COMPLETED"),
                eq(STAFF_EMAIL),
                contains("changed from CONFIRMED to COMPLETED")
        );

        verify(bookingOutboxService).saveEvent(
                bookingId,
                EMAIL,
                "BOOKING_COMPLETED",
                "Blood Donation Completed",
                "Thank you! Your blood donation has been completed."
        );
    }

    // TEST 11: Confirm booking and write audit log.
    @Test
    void shouldConfirmBookingAndWriteAuditLog() {

        Long bookingId = 103L;

        DonationBooking booking = mockStatusUpdateBooking(
                bookingId,
                "PENDING",
                3,
                "OPEN"
        );

        bookingService.updateBookingStatus(
                bookingId,
                "CONFIRMED",
                HOSPITAL_ID,
                STAFF_EMAIL,
                DONOR_ID,
                EMAIL
        );

        verify(booking)
                .setDonationBookingStatus("CONFIRMED");

        verify(bookingRepository)
                .saveAndFlush(booking);

        verify(auditLogService).saveAuditLog(
                eq("DONATION_BOOKING_CONFIRMED"),
                eq(STAFF_EMAIL),
                contains("changed from PENDING to CONFIRMED")
        );

        verify(bookingOutboxService).saveEvent(
                bookingId,
                EMAIL,
                "BOOKING_CONFIRMED",
                "Donation Booking Confirmed",
                "Your blood donation booking has been confirmed."
        );

        verify(auditLogService, times(1))
                .saveAuditLog(
                        anyString(),
                        anyString(),
                        anyString()
                );
    }

    // TEST 12: Reject invalid status transition.
    @Test
    void shouldNotAuditAnInvalidStatusTransition() {

        Long bookingId = 104L;

        DonationBooking booking = mockStatusUpdateBooking(
                bookingId,
                "PENDING",
                3,
                "OPEN"
        );

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.updateBookingStatus(
                        bookingId,
                        "COMPLETED",
                        HOSPITAL_ID,
                        STAFF_EMAIL,
                        DONOR_ID,
                        EMAIL
                )
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());

        verify(booking, never())
                .setDonationBookingStatus(anyString());

        verifyNoInteractions(
                auditLogService,
                bookingOutboxService
        );
    }

    // TEST 13: Reject missing audit email.
    @Test
    void shouldRejectMissingAuditEmailBeforeSaving() {

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.createBooking(
                        request,
                        DONOR_ID,
                        HOSPITAL_ID,
                        LocalDate.now().plusDays(3),
                        "OPEN",
                        " "
                )
        );

        assertEquals(HttpStatus.BAD_GATEWAY, ex.getStatusCode());

        verify(bookingRepository, never())
                .saveAndFlush(any());

        verifyNoInteractions(
                auditLogService,
                bookingOutboxService
        );
    }

    // TEST 14: Staff cancels booking and notifies the donor.
    @Test
    void shouldNotifyDonorWhenHospitalStaffCancelsBooking() {

        Long bookingId = 105L;

        DonationBooking booking = mockStatusUpdateBooking(
                bookingId,
                "CONFIRMED",
                3,
                "OPEN"
        );

        DonationSlot slot = booking.getDonationSlot();

        when(slot.getReservedCount()).thenReturn(2);

        bookingService.updateBookingStatus(
                bookingId,
                "CANCELLED",
                HOSPITAL_ID,
                STAFF_EMAIL,
                DONOR_ID,
                EMAIL
        );

        verify(booking)
                .setDonationBookingStatus("CANCELLED");

        verify(slot).setReservedCount(1);

        verify(slotRepository).save(slot);

        verify(auditLogService).saveAuditLog(
                eq("DONATION_BOOKING_CANCELLED"),
                eq(STAFF_EMAIL),
                contains("changed from CONFIRMED to CANCELLED")
        );

        verify(bookingOutboxService).saveEvent(
                bookingId,
                EMAIL,
                "BOOKING_CANCELLED",
                "Donation Booking Cancelled",
                "Your blood donation booking has been cancelled by the hospital."
        );
    }

    // TEST 15: Reject mismatched donor ID after locks.
    @Test
    void shouldRejectStatusUpdateForUnexpectedDonor() {

        Long bookingId = 106L;

        DonationBooking booking = mockStatusUpdateBooking(
                bookingId,
                "PENDING",
                3,
                "OPEN"
        );

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.updateBookingStatus(
                        bookingId,
                        "CONFIRMED",
                        HOSPITAL_ID,
                        STAFF_EMAIL,
                        999L,
                        EMAIL
                )
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());

        verify(booking, never())
                .setDonationBookingStatus(anyString());

        verifyNoInteractions(
                auditLogService,
                bookingOutboxService
        );
    }

    // TEST 16: Reject another hospital's update.
    @Test
    void shouldRejectStatusUpdateForAnotherHospital() {

        Long bookingId = 107L;

        DonationBooking booking = mockStatusUpdateBooking(
                bookingId,
                "PENDING",
                3,
                "OPEN"
        );

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.updateBookingStatus(
                        bookingId,
                        "CONFIRMED",
                        999L,
                        STAFF_EMAIL,
                        DONOR_ID,
                        EMAIL
                )
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());

        verify(booking, never())
                .setDonationBookingStatus(anyString());

        verifyNoInteractions(
                auditLogService,
                bookingOutboxService
        );
    }

    // TEST 17: Reject missing donor email.
    @Test
    void shouldRejectStatusUpdateWhenDonorEmailMissing() {

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.updateBookingStatus(
                        108L,
                        "CONFIRMED",
                        HOSPITAL_ID,
                        STAFF_EMAIL,
                        DONOR_ID,
                        " "
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());

        verify(bookingRepository, never())
                .saveAndFlush(any());

        verifyNoInteractions(
                auditLogService,
                bookingOutboxService
        );
    }

    private DonationBooking prepareReschedule(int targetReserved) {
        DonationSlot oldSlot = new DonationSlot();
        oldSlot.setId(3L); oldSlot.setHospitalId(HOSPITAL_ID);
        oldSlot.setSlotDate(LocalDate.now().plusDays(2));
        oldSlot.setStartTime(LocalTime.of(9, 0)); oldSlot.setEndTime(LocalTime.of(11, 0));
        oldSlot.setCapacity(1); oldSlot.setReservedCount(1); oldSlot.setStatus("OPEN");
        DonationSlot target = new DonationSlot();
        target.setId(1L); target.setHospitalId(HOSPITAL_ID);
        target.setSlotDate(LocalDate.now().plusDays(3));
        target.setStartTime(LocalTime.of(9, 0)); target.setEndTime(LocalTime.of(11, 0));
        target.setCapacity(1); target.setReservedCount(targetReserved); target.setStatus("OPEN");
        DonationBooking booking = new DonationBooking();
        booking.setId(99L); booking.setDonorProfileId(DONOR_ID); booking.setBloodRequestId(BLOOD_REQUEST_ID);
        booking.setDonationSlot(oldSlot); booking.setDonationBookingDate(oldSlot.getSlotDate());
        booking.setDonationBookingTime(LocalTime.of(10, 0)); booking.setDonationBookingStatus("CONFIRMED");
        lenient().when(bookingRepository.findById(99L)).thenReturn(Optional.of(booking));
        lenient().when(bookingRepository.findByIdForUpdate(99L)).thenReturn(Optional.of(booking));
        lenient().when(slotRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(target));
        lenient().when(slotRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(oldSlot));
        lenient().when(request.getDonationSlotId()).thenReturn(1L);
        lenient().when(request.getDonationBookingDate()).thenReturn(target.getSlotDate());
        lenient().when(request.getDonationBookingTime()).thenReturn(LocalTime.of(10, 0));
        lenient().when(bloodRequestLockQuery.getResultList()).thenReturn(Collections.singletonList(
                new Object[]{HOSPITAL_ID, Date.valueOf(LocalDate.now().plusDays(5)), "OPEN", true}));
        Query reminder = mock(Query.class);
        lenient().when(entityManager.createNativeQuery("SELECT COUNT(*) FROM p4_donation_reminder WHERE booking_id=:id")).thenReturn(reminder);
        lenient().when(reminder.setParameter(eq("id"), any())).thenReturn(reminder);
        lenient().when(reminder.getSingleResult()).thenReturn(0L);
        lenient().when(bookingRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        return booking;
    }

    @Test
    void rescheduleRetainsIdentityMovesCapacityAndLocksSlotsAscending() {
        DonationBooking booking = prepareReschedule(0);
        DonationSlot original = booking.getDonationSlot();
        var response = bookingService.rescheduleBooking(99L, request, DONOR_ID, HOSPITAL_ID, LocalDate.now().plusDays(5), EMAIL);
        assertEquals(99L, response.id()); assertEquals(1L, response.donationSlotId());
        assertEquals("PENDING", response.donationBookingStatus());
        assertEquals(0, original.getReservedCount()); assertEquals(1, booking.getDonationSlot().getReservedCount());
        var locks = inOrder(slotRepository);
        locks.verify(slotRepository).findByIdForUpdate(1L);
        locks.verify(slotRepository).findByIdForUpdate(3L);
        verify(bookingOutboxService).saveEvent(eq(99L), eq(EMAIL), eq("BOOKING_RESCHEDULED"), anyString(), anyString());
    }

    @Test
    void rescheduleRejectsFullTargetWithoutReleasingOriginal() {
        DonationBooking booking = prepareReschedule(1);
        DonationSlot original = booking.getDonationSlot();
        ResponseStatusException error = assertThrows(ResponseStatusException.class, () ->
                bookingService.rescheduleBooking(99L, request, DONOR_ID, HOSPITAL_ID, LocalDate.now().plusDays(5), EMAIL));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertSame(original, booking.getDonationSlot()); assertEquals(1, original.getReservedCount());
        assertEquals("CONFIRMED", booking.getDonationBookingStatus());
        verify(slotRepository, never()).save(any()); verify(bookingRepository, never()).saveAndFlush(any());
        verifyNoInteractions(auditLogService, bookingOutboxService);
    }

    @Test
    void rescheduleRejectsAlreadyRemindedBooking() {
        DonationBooking booking = prepareReschedule(0);
        Query reminder = entityManager.createNativeQuery("SELECT COUNT(*) FROM p4_donation_reminder WHERE booking_id=:id");
        when(reminder.getSingleResult()).thenReturn(1L);
        assertThrows(ResponseStatusException.class, () ->
                bookingService.rescheduleBooking(99L, request, DONOR_ID, HOSPITAL_ID, LocalDate.now().plusDays(5), EMAIL));
        assertEquals(3L, booking.getDonationSlot().getId()); assertEquals(1, booking.getDonationSlot().getReservedCount());
        verify(slotRepository, never()).save(any()); verifyNoInteractions(auditLogService, bookingOutboxService);
    }

    @Test
    void rescheduleRejectsPastOriginalAppointment() {
        DonationBooking booking = prepareReschedule(0);
        booking.setDonationBookingDate(LocalDate.now().minusDays(1));
        assertThrows(ResponseStatusException.class, () ->
                bookingService.rescheduleBooking(99L, request, DONOR_ID, HOSPITAL_ID, LocalDate.now().plusDays(5), EMAIL));
        verify(slotRepository, never()).save(any()); verifyNoInteractions(auditLogService, bookingOutboxService);
    }

    @Test
    void rescheduleRejectsAnotherDonor() {
        prepareReschedule(0);
        ResponseStatusException error = assertThrows(ResponseStatusException.class, () ->
                bookingService.rescheduleBooking(99L, request, 999L, HOSPITAL_ID, LocalDate.now().plusDays(5), EMAIL));
        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verify(slotRepository, never()).findByIdForUpdate(anyLong()); verifyNoInteractions(auditLogService, bookingOutboxService);
    }

    @Test
    void rescheduleRejectsChangedBookingAfterWaitingForLocks() {
        DonationBooking booking = prepareReschedule(0);
        doAnswer(invocation -> {
            if (invocation.getArgument(0) == booking) booking.setDonationBookingStatus("CANCELLED");
            return null;
        }).when(entityManager).refresh(any(), eq(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE));
        assertThrows(ResponseStatusException.class, () ->
                bookingService.rescheduleBooking(99L, request, DONOR_ID, HOSPITAL_ID, LocalDate.now().plusDays(5), EMAIL));
        verify(slotRepository, never()).save(any()); verifyNoInteractions(auditLogService, bookingOutboxService);
    }
}
