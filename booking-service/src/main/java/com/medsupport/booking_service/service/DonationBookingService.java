
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
import java.util.List;

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

    @Transactional
    public DonationBookingResponse cancelBooking(
            Long bookingId,
            Long verifiedDonorProfileId) {

        if (bookingId == null || verifiedDonorProfileId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Booking ID and donor profile are required");
        }


        DonationBooking existingBooking = bookingRepository
                .findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Booking not found"));

        if (!existingBooking.getDonorProfileId()
                .equals(verifiedDonorProfileId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You cannot cancel another donor's booking");
        }


        DonationSlot slot = slotRepository
                .findByIdForUpdate(
                        existingBooking.getDonationSlot().getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Donation slot not found"));


        DonationBooking booking = bookingRepository
                .findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Booking not found"));

        if (!booking.getDonorProfileId()
                .equals(verifiedDonorProfileId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You cannot cancel another donor's booking");
        }

        if ("CANCELLED".equals(booking.getDonationBookingStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Booking is already cancelled");
        }

        if ("COMPLETED".equals(booking.getDonationBookingStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Completed bookings cannot be cancelled");
        }

        if (slot.getReservedCount() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Invalid slot reservation count");
        }

        booking.setDonationBookingStatus("CANCELLED");
        slot.setReservedCount(slot.getReservedCount() - 1);

        slotRepository.save(slot);
        DonationBooking savedBooking =
                bookingRepository.saveAndFlush(booking);

        return DonationBookingResponse.from(savedBooking);
    }

    @Transactional
    public DonationBookingResponse updateBookingStatus(
            Long bookingId,
            String newStatus,
            Long verifiedHospitalId) {

        if (bookingId == null
                || verifiedHospitalId == null
                || newStatus == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Required information is missing");
        }

        // Read the booking to identify its slot.
        DonationBooking existingBooking = bookingRepository
                .findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Booking not found"));

        // Lock slot first, matching cancellation lock order.
        DonationSlot slot = slotRepository
                .findByIdForUpdate(
                        existingBooking.getDonationSlot().getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Donation slot not found"));

        // Lock booking before updating status.
        DonationBooking booking = bookingRepository
                .findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Booking not found"));

        if (!slot.getId().equals(booking.getDonationSlot().getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Booking slot changed during processing");
        }

        if (!slot.getHospitalId().equals(verifiedHospitalId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You cannot update another hospital's booking");
        }

        String currentStatus = booking.getDonationBookingStatus();

        boolean validTransition = switch (currentStatus) {
            case "PENDING" ->
                    newStatus.equals("CONFIRMED")
                            || newStatus.equals("CANCELLED");

            case "CONFIRMED" ->
                    newStatus.equals("COMPLETED")
                            || newStatus.equals("CANCELLED");

            default -> false;
        };

        if (!validTransition) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Invalid booking status transition");
        }


        if ("CANCELLED".equals(newStatus)) {
            if (slot.getReservedCount() <= 0) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Invalid slot reservation count");
            }

            slot.setReservedCount(slot.getReservedCount() - 1);
            slotRepository.save(slot);
        }

        booking.setDonationBookingStatus(newStatus);

        DonationBooking savedBooking =
                bookingRepository.saveAndFlush(booking);

        return DonationBookingResponse.from(savedBooking);
    }

    @Transactional(readOnly = true)
    public List<DonationBookingResponse> getBookingsByDonor(
            Long verifiedDonorProfileId) {

        return bookingRepository.findByDonorProfileId(
                        verifiedDonorProfileId
                ).stream()
                .map(DonationBookingResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DonationBookingResponse> getBookingsByBloodRequest(
            Long bloodRequestId) {

        return bookingRepository.findByBloodRequestId(
                        bloodRequestId
                ).stream()
                .map(DonationBookingResponse::from)
                .toList();
    }



}
