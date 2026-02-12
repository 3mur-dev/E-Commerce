package com.omar.ecommerce.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:no-reply@shoppio.local}")
    private String fromAddress;

    public void sendPasswordResetEmail(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setFrom(fromAddress);
        message.setSubject("Your Shoppio reset code");
        message.setText(buildBody(code));
        mailSender.send(message);
    }

    public void sendVerificationEmail(String to, String verifyLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setFrom(fromAddress);
        message.setSubject("Verify your Shoppio account");
        message.setText(buildVerifyBody(verifyLink));
        mailSender.send(message);
    }

    private String buildBody(String code) {
        return "We received a request to reset your password.\n\n"
                + "Your verification code (valid for 60 minutes):\n"
                + code + "\n\n"
                + "If you did not request this, you can safely ignore this email.";
    }

    private String buildVerifyBody(String verifyLink) {
        return "Welcome to Shoppio!\n\n"
                + "Please verify your email to activate your account:\n"
                + verifyLink + "\n\n"
                + "If you did not create this account, you can ignore this email.";
    }
}
