
package com.medsupport.booking_service.repository;

import com.medsupport.booking_service.model.DonationSlot;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DonationSlotRepository
        extends JpaRepository<DonationSlot, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM DonationSlot s WHERE s.id = :slotId")
    Optional<DonationSlot> findByIdForUpdate(
            @Param("slotId") Long slotId
    );
}
