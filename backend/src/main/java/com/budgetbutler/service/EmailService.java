package com.budgetbutler.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Sending real email requires an SMTP server (Gmail, SendGrid, etc.) configured in
 * application.properties. Since this project is meant to run with a $0 budget and many
 * beginners won't have SMTP set up locally, this service falls back to simply LOGGING the
 * email content (including the verification/reset link) to the console instead of crashing -
 * you can still copy the link from the terminal and test the full flow without any email setup.
 *
 * To send real emails: set spring.mail.* properties (see application.properties) - a free
 * option is a Gmail account with an "App Password" (Google Account -> Security -> App Passwords).
 */
@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    @Value("${app.mail-enabled:false}")
    private boolean mailEnabled;

    public void sendVerificationEmail(String toEmail, String verificationLink) {
        String subject = "Verify your Budget Butler account";
        String body = "Welcome to Budget Butler! Click the link below to verify your email:\n\n"
                + verificationLink + "\n\nThis link expires in 24 hours.";
        send(toEmail, subject, body);
    }

    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        String subject = "Reset your Budget Butler password";
        String body = "We received a request to reset your password. Click the link below:\n\n"
                + resetLink + "\n\nThis link expires in 1 hour. If you didn't request this, you can ignore this email.";
        send(toEmail, subject, body);
    }

    private void send(String toEmail, String subject, String body) {
        if (!mailEnabled) {
            // Console fallback - lets you test the full flow with zero email setup.
            System.out.println("=== EMAIL (mail sending disabled, printing instead) ===");
            System.out.println("To: " + toEmail);
            System.out.println("Subject: " + subject);
            System.out.println(body);
            System.out.println("========================================================");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            // Don't let a broken SMTP config crash registration/reset flows - just log it.
            System.err.println("Failed to send email to " + toEmail + ": " + e.getMessage());
        }
    }
}
