
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
    @Query("SELECT b.donationBookingStatus, COUNT(b) FROM DonationBooking b "
            + "WHERE b.donationSlot.hospitalId=:hospitalId GROUP BY b.donationBookingStatus")
    List<Object[]> countHospitalBookingsByStatus(@Param("hospitalId") Long hospitalId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM DonationBooking b WHERE b.id = :bookingId")
    Optional<DonationBooking> findByIdForUpdate(
            @Param("bookingId") Long bookingId);

    boolean existsByDonorProfileIdAndDonationBookingDateAndDonationBookingTimeAndDonationBookingStatusNot(
            Long donorProfileId, LocalDate donationBookingDate,
            LocalTime donationBookingTime, String excludedStatus);


    List<DonationBooking> findByDonorProfileId(Long donorProfileId);
    List<DonationBooking> findByDonationSlot_HospitalId(Long hospitalId);

    boolean existsByDonorProfileIdAndDonationBookingDateAndDonationBookingTimeAndDonationBookingStatusNotAndIdNot(
            Long donorProfileId, LocalDate date, LocalTime time, String excludedStatus, Long bookingId);

    List<DonationBooking> findByBloodRequestId(Long bloodRequestId);


    long countByBloodRequestIdAndDonationBookingStatus(Long bloodRequestId,
            String donationBookingStatus);
}
