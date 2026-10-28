package com.app.med_support.repository;

import com.app.med_support.model.DonationReminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface DonationReminderRepository
        extends JpaRepository<DonationReminder, Long> {
    List<DonationReminder> findByStatusAndReminderTimeLessThanEqual(
            String status, LocalDateTime reminderTime);

    @Modifying
    @Query("""
    UPDATE DonationReminder r
    SET r.status = 'PROCESSING'
    WHERE r.id = :reminderId
    AND r.status = 'PENDING'
    """)
    int claimReminder(@Param("reminderId") Long reminderId);

}
