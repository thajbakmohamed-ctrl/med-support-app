package com.app.med_support.repository;

import com.app.med_support.model.DonationBooking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DonationBookingRepository extends JpaRepository<DonationBooking, Long> {
    List<DonationBooking> findByDonorProfileId(Long donorProfileId);
    List<DonationBooking> findByBloodRequestId(Long bloodRequestId);
    List<DonationBooking> findByDonationBookingStatus(String donationBookingStatus);
}
