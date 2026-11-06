package com.app.med_support.service;

import com.app.med_support.model.DonationReminder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Service
public class DonationReminderScheduler {

    private final DonationReminderService reminderService;
    private final DonationReminderTaskService reminderTaskService;
    private static final Logger logger = LoggerFactory.getLogger(DonationReminderScheduler.class);

    public DonationReminderScheduler(
            DonationReminderService reminderService,
            DonationReminderTaskService reminderTaskService) {

        this.reminderService = reminderService;
        this.reminderTaskService = reminderTaskService;
    }

    // Check for due reminders every 10 seconds
    @Scheduled(fixedDelay = 10000)
    public void processPendingReminders() {

        try {

            List<DonationReminder> reminders = reminderService.getPendingReminders();

            if (!reminders.isEmpty()) {
                logger.info("Found {} pending reminders to process", reminders.size());
            }

            for (DonationReminder reminder : reminders) {

                try {
                    reminderTaskService.submitReminder(reminder);

                } catch (Exception e) {
                    logger.error("Failed to submit reminder ID: {}", reminder.getId(), e);
                }
            }

        } catch (Exception e) {

            logger.error("Failed to check pending reminders", e);
        }
    }
}