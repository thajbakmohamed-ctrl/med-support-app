
package com.medsupport.booking_service.repository;

import com.medsupport.booking_service.model.DonationBooking;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface DonationBookingRepository
        extends JpaRepository<DonationBooking, Long> {

    // Lock booking during status changes or cancellation
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM DonationBooking b WHERE b.id = :bookingId")
    Optional<DonationBooking> findByIdForUpdate(
            @Param("bookingId") Long bookingId
    );

    boolean existsByDonorProfileIdAndDonationBookingDateAndDonationBookingTimeAndDonationBookingStatusNot(
            Long donorProfileId,
            LocalDate donationBookingDate,
            LocalTime donationBookingTime,
            String excludedStatus
    );


    List<DonationBooking> findByDonorProfileId(
            Long donorProfileId
    );

    List<DonationBooking> findByBloodRequestId(
            Long bloodRequestId
    );


    long countByBloodRequestIdAndDonationBookingStatus(
            Long bloodRequestId,
            String donationBookingStatus
    );
}
