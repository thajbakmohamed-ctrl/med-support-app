
package com.medsupport.booking_service.service;

import com.medsupport.booking_service.model.DonationBooking;
import com.medsupport.booking_service.model.DonationSlot;
import com.medsupport.booking_service.repository.DonationBookingRepository;
import com.medsupport.booking_service.repository.DonationSlotRepository;
import com.medsupport.booking_service.request.DonationBookingRequest;
import com.medsupport.booking_service.response.DonationBookingResponse;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;

@Service
public class DonationBookingService {

    private final DonationBookingRepository bookingRepository;
    private final DonationSlotRepository slotRepository;

    public DonationBookingService(
            DonationBookingRepository bookingRepository,
            DonationSlotRepository slotRepository) {

        this.bookingRepository = bookingRepository;
        this.slotRepository = slotRepository;
    }

    // Internal method: call only after checking donor ownership
    // and blood request details with their owning services.
    @Transactional
    public DonationBookingResponse createBooking(
            DonationBookingRequest request,
            Long verifiedDonorProfileId,
            Long verifiedHospitalId,
            LocalDate verifiedBloodNeededDate,
            String verifiedBloodRequestStatus) {

        if (request == null
                || request.getDonationSlotId() == null
                || request.getBloodRequestId() == null
                || verifiedDonorProfileId == null
                || verifiedHospitalId == null
                || verifiedBloodNeededDate == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Required booking information is missing");
        }

        if (!"OPEN".equals(verifiedBloodRequestStatus)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Blood request is not open");
        }

        if (request.getDonorProfileId() == null
                || !request.getDonorProfileId()
                .equals(verifiedDonorProfileId)) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You cannot book for another donor");
        }

        // Lock the slot before checking capacity.
        DonationSlot slot = slotRepository
                .findByIdForUpdate(request.getDonationSlotId())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Donation slot not found"));

        LocalDate bookingDate = request.getDonationBookingDate();
        LocalTime bookingTime = request.getDonationBookingTime();

        if (bookingDate == null || bookingTime == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Booking date and time are required");
        }

        if (bookingDate.isBefore(LocalDate.now())
                || (bookingDate.equals(LocalDate.now())
                && bookingTime.isBefore(LocalTime.now()))) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Booking date and time cannot be in the past");
        }

        if (bookingDate.isAfter(verifiedBloodNeededDate)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Booking date cannot be after blood needed date");
        }

        if (!verifiedHospitalId.equals(slot.getHospitalId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Slot and blood request must belong to the same hospital");
        }

        if (!bookingDate.equals(slot.getSlotDate())
                || bookingTime.isBefore(slot.getStartTime())
                || !bookingTime.isBefore(slot.getEndTime())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Booking must match the slot date and time");
        }

        if (!"OPEN".equals(slot.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Donation slot is not open");
        }

        boolean alreadyBooked = bookingRepository
                .existsByDonorProfileIdAndDonationBookingDateAndDonationBookingTimeAndDonationBookingStatusNot(
                        verifiedDonorProfileId,
                        bookingDate,
                        bookingTime,
                        "CANCELLED");

        if (alreadyBooked) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Donor already has a booking at this date and time");
        }

        if (slot.getReservedCount() >= slot.getCapacity()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Donation slot is full");
        }

        DonationBooking booking = new DonationBooking();

        booking.setDonorProfileId(verifiedDonorProfileId);
        booking.setBloodRequestId(request.getBloodRequestId());
        booking.setDonationSlot(slot);
        booking.setDonationBookingDate(bookingDate);
        booking.setDonationBookingTime(bookingTime);
        booking.setDonationBookingStatus("PENDING");

        slot.setReservedCount(slot.getReservedCount() + 1);

        slotRepository.save(slot);

        DonationBooking savedBooking =
                bookingRepository.saveAndFlush(booking);

        return DonationBookingResponse.from(savedBooking);
    }
}
