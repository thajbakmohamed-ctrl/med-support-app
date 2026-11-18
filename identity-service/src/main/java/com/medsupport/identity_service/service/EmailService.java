
package com.medsupport.identity_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationEmail(String userEmail, String verificationLink) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(userEmail);
        message.setSubject("Verify Your Med Support Account");

        message.setText("Welcome to Med Support!\n\n" +
                        "Please click the link below to verify your email:\n\n" +
                        verificationLink +
                        "\n\nThis link is valid for 24 hours.");

        mailSender.send(message);
        logger.info("Verification email submitted successfully");
    }

    public void sendPasswordResetEmail(String userEmail, String resetLink) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(userEmail);
        message.setSubject("Reset Your Med Support Password");

        message.setText(
                "Hello,\n\n" +
                        "Click the link below to reset your password:\n\n" +
                        resetLink +
                        "\n\nThis link is valid for 1 hour."
        );

        mailSender.send(message);
        logger.info("Password reset email submitted successfully");
    }
}
