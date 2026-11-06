package com.app.med_support.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private final JavaMailSender medSupportEmailSender;

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);


    public EmailService(JavaMailSender medSupportEmailSender) {
        this.medSupportEmailSender = medSupportEmailSender;
    }
    public void sendVerificationEmail(String userEmail, String verificationLink) {
        // empty
        SimpleMailMessage emailMessageToVerifyEmail = new SimpleMailMessage();
        // take the message that is called emailMessageToVerifyEmail and send it to userEmail
        emailMessageToVerifyEmail.setTo(userEmail);
        emailMessageToVerifyEmail.setSubject("Verify Your Email to Activate Your Med Support Account");
        emailMessageToVerifyEmail.setText(
        "Welcome to Med Support!\n\n" + "Thank you for creating your account.\n\n" +
        "Please click the link below to verify your email and activate your account:\n\n" +
        verificationLink + "\n\nThis verification link is valid for 24 hours.");
        medSupportEmailSender.send(emailMessageToVerifyEmail);

        logger.info("Verification email submitted successfully");
    }
    public void sendPasswordResetEmail(String userEmail, String resetLink) {
        SimpleMailMessage passwordResetEmail = new SimpleMailMessage();
        passwordResetEmail.setTo(userEmail);
        passwordResetEmail.setSubject("Reset Your Med Support Account Password");
        passwordResetEmail.setText("Hello,\n\n" +
        "We received a request to reset your Med Support account password.\n\n" +
        "Please click the link below to reset your password:\n\n" + resetLink +
        "\n\nThis password reset link is valid for 1 hour.");
        medSupportEmailSender.send(passwordResetEmail);

        logger.info("Password reset email submitted successfully");
    }

    public void sendBookingConfirmationEmail(String userEmail, Long bookingId) {
        SimpleMailMessage email = new SimpleMailMessage();
        email.setTo(userEmail);
        email.setSubject("Med Support - Booking Confirmed");
        email.setText("Hello,\n\n" +
                        "Your blood donation booking has been confirmed.\n\n" +
                        "Booking ID: " + bookingId + "\n\n" +
                        "Thank you for supporting blood donation!\n\n" +
                        "Med Support Team");

        medSupportEmailSender.send(email);

        logger.info("Booking confirmation email submitted successfully");
    }

    public void sendBookingCancellationEmail(String userEmail, Long bookingId) {
        SimpleMailMessage email = new SimpleMailMessage();
        email.setTo(userEmail);
        email.setSubject("Med Support - Booking Cancelled");
        email.setText("Hello,\n\n" +
                        "Your blood donation booking has been cancelled.\n\n" +
                        "Booking ID: " + bookingId + "\n\n" +
                        "If you would like to donate blood, you can make a new booking.\n\n" +
                        "Med Support Team");
        medSupportEmailSender.send(email);

        logger.info("Booking cancellation email submitted successfully");
    }


    public void sendBookingReminderEmail(String userEmail, Long bookingId) {
        SimpleMailMessage email = new SimpleMailMessage();
        email.setTo(userEmail);
        email.setSubject("Med Support - Donation Appointment Reminder");
        email.setText("Hello,\n\n" +
                        "This is a reminder about your upcoming blood donation appointment.\n\n" +
                        "Booking ID: " + bookingId + "\n\n" +
                        "Please make sure to arrive on time.\n\n" +
                        "Thank you for helping save lives!\n\n" +
                        "Med Support Team");

        medSupportEmailSender.send(email);

        logger.info("Booking reminder email submitted successfully");
    }
}
