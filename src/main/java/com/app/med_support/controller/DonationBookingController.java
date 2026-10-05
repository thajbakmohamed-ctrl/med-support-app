package com.app.med_support.controller;

import com.app.med_support.model.DonationBooking;
import com.app.med_support.service.DonationBookingService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/donation-bookings")
public class DonationBookingController {

    private final DonationBookingService donationBookingService;

    public DonationBookingController(DonationBookingService donationBookingService) {
        this.donationBookingService = donationBookingService;
    }
    @PostMapping
    public DonationBooking createDonationBooking(@RequestBody DonationBooking donationBooking) {
        return donationBookingService.createDonationBooking(donationBooking);
    }


    @GetMapping("/donor/{donorProfileId}")
    public List<DonationBooking> getBookingsByDonor(@PathVariable Long donorProfileId) {
        return donationBookingService.getBookingsByDonor(donorProfileId);
    }


    @GetMapping("/blood-request/{bloodRequestId}")
    public List<DonationBooking> getBookingsByBloodRequest(@PathVariable Long bloodRequestId) {
        return donationBookingService.getBookingsByBloodRequest(bloodRequestId);
    }


    @PutMapping("/{bookingId}/status")
    public DonationBooking updateDonationBookingStatus(@PathVariable Long bookingId, @RequestParam String newStatus) {
        return donationBookingService.updateDonationBookingStatus(bookingId, newStatus);
    }

    
    @PutMapping("/{bookingId}/cancel")
    public DonationBooking cancelDonationBooking(@PathVariable Long bookingId, @RequestParam Long donorProfileId) {
        return donationBookingService.cancelDonationBooking(bookingId, donorProfileId);
    }
}
