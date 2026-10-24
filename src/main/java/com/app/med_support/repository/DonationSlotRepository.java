package com.app.med_support.repository;

import com.app.med_support.model.DonationSlot;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DonationSlotRepository
        extends JpaRepository<DonationSlot, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM DonationSlot s WHERE s.id = :slotId")
    Optional<DonationSlot> findByIdForUpdate(@Param("slotId") Long slotId);
    @Query("""
       SELECT s FROM DonationSlot s
       WHERE s.hospital.id = :hospitalId
       AND s.status = 'OPEN'
       AND s.reservedCount < s.capacity
       ORDER BY s.slotDate, s.startTime
       """)
    List<DonationSlot> findAvailableSlotsByHospitalId(
            @Param("hospitalId") Long hospitalId);


}