package com.app.med_support.service;

import com.app.med_support.model.DonationReminder;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

@Service
public class DonationReminderTaskService {
    private static final Logger logger = LoggerFactory.getLogger(DonationReminderTaskService.class);

    private final ExecutorService reminderExecutor;
    private final ReminderProcessingService processingService;
    private final EmailService emailService;



    public DonationReminderTaskService(ExecutorService reminderExecutor,
            ReminderProcessingService processingService, EmailService emailService) {

        this.reminderExecutor = reminderExecutor;
        this.processingService = processingService;
        this.emailService = emailService;
    }


    public void submitReminder(DonationReminder reminder) {

        Long reminderId = reminder.getId();

        try {

            reminderExecutor.submit(() -> {

                logger.info("Thread {} started reminder task. Reminder ID: {}",
                        Thread.currentThread().getName(), reminderId);

                boolean claimed = false;
                try {

                    claimed = processingService.claimReminder(reminderId);

                    if (!claimed) {
                        return;
                    }



                    processingService.sendReminderNotification(reminderId);


                    ReminderProcessingService.ReminderEmailDetails details = processingService.getReminderEmailDetails(reminderId);


                    emailService.sendBookingReminderEmail(details.donorEmail(),
                            details.bookingId());

                    processingService.completeReminder(reminderId);


                    logger.info("Reminder processed successfully. Reminder ID: {}",
                            reminderId);

                } catch (Exception e) {

                    logger.error("Failed to process reminder ID: {}",
                            reminderId, e);

                    if (claimed) {
                        try {

                            processingService.failReminder(reminderId);

                        } catch (Exception updateError) {

                            logger.error("Failed to mark reminder ID {} as FAILED",
                                    reminderId, updateError);
                        }
                    }
                }
            });

        } catch (RejectedExecutionException e) {

            logger.error("Executor rejected reminder task. Reminder ID: {}",
                    reminderId, e);
        }
    }
}