package com.app.med_support.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private final JavaMailSender medSupportEmailSender;


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
    }
}
