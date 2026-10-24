package com.app.med_support.repository;

import com.app.med_support.model.DonationBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface DonationBookingRepository extends JpaRepository<DonationBooking, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM DonationBooking b WHERE b.id = :bookingId")
    Optional<DonationBooking> findByIdForUpdate(@Param("bookingId") Long bookingId);
    List<DonationBooking> findByDonorProfileId(Long donorProfileId);
    List<DonationBooking> findByBloodRequestId(Long bloodRequestId);
    List<DonationBooking> findByDonationBookingStatus(String donationBookingStatus);
    boolean existsByDonorProfileIdAndDonationBookingDateAndDonationBookingTimeAndDonationBookingStatusNot(
    Long donorProfileId, LocalDate donationBookingDate, LocalTime donationBookingTime, String donationBookingStatus);


}
