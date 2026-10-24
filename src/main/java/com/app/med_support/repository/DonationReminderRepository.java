package com.app.med_support.repository;

import com.app.med_support.model.DonationReminder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface DonationReminderRepository
        extends JpaRepository<DonationReminder, Long> {
    List<DonationReminder> findByStatusAndReminderTimeLessThanEqual(
            String status, LocalDateTime reminderTime);

}
