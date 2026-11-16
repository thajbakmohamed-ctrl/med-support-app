
package com.medsupport.booking_service.repository;

import com.medsupport.booking_service.model.DonationSlot;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface DonationSlotRepository
        extends JpaRepository<DonationSlot, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM DonationSlot s WHERE s.id = :slotId")
    Optional<DonationSlot> findByIdForUpdate(
            @Param("slotId") Long slotId);

    List<DonationSlot> findByHospitalId(Long hospitalId);

    @Query("""
            SELECT s
            FROM DonationSlot s
            WHERE s.hospitalId = :hospitalId
              AND s.status = 'OPEN'
              AND s.reservedCount < s.capacity
              AND s.slotDate <= :neededDate
              AND (
                    s.slotDate > :today
                    OR (
                        s.slotDate = :today
                        AND s.startTime > :currentTime
                    )
                  )
            ORDER BY s.slotDate ASC, s.startTime ASC, s.id ASC
            """)
    List<DonationSlot> findAvailableSlotsForBloodRequest(
            @Param("hospitalId") Long hospitalId,
            @Param("neededDate") LocalDate neededDate,
            @Param("today") LocalDate today,
            @Param("currentTime") LocalTime currentTime);
}
