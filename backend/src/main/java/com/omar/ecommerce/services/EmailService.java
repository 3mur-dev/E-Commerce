package com.omar.ecommerce.services;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${app.mail.from:}")
    private String fromEmail;

    public EmailService(@Nullable JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationEmail(String email, String token) {

        String verificationLink =
                resolveFrontendUrl() + "/verify-email?token=" + token;

        String html = buildVerificationEmailHtml(verificationLink);

        sendHtmlEmail(
                email,
                "Verify your email",
                html
        );
    }

    public void sendOrderReceivedEmail(String email) {

        String html = """
            <div style="font-family: Arial; padding: 20px;">
                <h2>Order Received ✅</h2>

                <p>Thank you for your order.</p>

                <p>
                    Your order has been received successfully
                    and is now being processed.
                </p>

                <p>
                    We’ll notify you once the status changes.
                </p>
            </div>
            """;

        sendHtmlEmail(
                email,
                "We received your order",
                html
        );
    }

    public void sendOrderShippedEmail(String email, Long orderId) {

        String html = """
        <div style="
            font-family: Arial, sans-serif;
            background-color: #f5f5f5;
            padding: 40px 20px;
        ">

            <div style="
                max-width: 600px;
                margin: 0 auto;
                background: white;
                border-radius: 12px;
                padding: 40px;
                box-shadow: 0 4px 12px rgba(0,0,0,0.08);
            ">

                <h1 style="
                    margin: 0 0 20px;
                    color: #111827;
                    font-size: 28px;
                ">
                    Your Order Has Shipped 🚚
                </h1>

                <p style="
                    color: #4B5563;
                    font-size: 16px;
                    line-height: 1.7;
                    margin-bottom: 20px;
                ">
                    Good news — your order is now on the way.
                </p>

                <div style="
                    background: #F9FAFB;
                    border: 1px solid #E5E7EB;
                    border-radius: 10px;
                    padding: 20px;
                    margin: 30px 0;
                ">

                    <p style="
                        margin: 0;
                        color: #6B7280;
                        font-size: 14px;
                    ">
                        ORDER ID
                    </p>

                    <p style="
                        margin: 8px 0 0;
                        font-size: 22px;
                        font-weight: bold;
                        color: #111827;
                    ">
                        #%s
                    </p>

                </div>

                <p style="
                    color: #4B5563;
                    font-size: 15px;
                    line-height: 1.7;
                ">
                    We’ll notify you again once your order is delivered.
                </p>

                <hr style="
                    border: none;
                    border-top: 1px solid #E5E7EB;
                    margin: 35px 0;
                ">

                <p style="
                    color: #9CA3AF;
                    font-size: 13px;
                    text-align: center;
                    margin: 0;
                ">
                    Thank you for shopping with Shoppio.
                </p>

            </div>
        </div>
        """.formatted(orderId);

        sendHtmlEmail(
                email,
                "Your order has shipped",
                html
        );
    }

    public void sendOrderDeliveredEmail(String email, Long orderId) {
        String html = """
            <div style="
            font-family: Arial, sans-serif;
            background-color: #f5f5f5;
            padding: 40px 20px;
        ">

            <div style="
                max-width: 600px;
                margin: 0 auto;
                background: white;
                border-radius: 12px;
                padding: 40px;
                box-shadow: 0 4px 12px rgba(0,0,0,0.08);
            ">

                <h1 style="
                    margin: 0 0 20px;
                    color: #111827;
                    font-size: 28px;
                ">
                    Your Order Has Arrived 🚚
                </h1>

                <p style="
                    color: #4B5563;
                    font-size: 16px;
                    line-height: 1.7;
                    margin-bottom: 20px;
                ">
                    Good news — your order has been delivered.
                </p>

                <div style="
                    background: #F9FAFB;
                    border: 1px solid #E5E7EB;
                    border-radius: 10px;
                    padding: 20px;
                    margin: 30px 0;
                ">

                    <p style="
                        margin: 0;
                        color: #6B7280;
                        font-size: 14px;
                    ">
                        ORDER ID
                    </p>

                    <p style="
                        margin: 8px 0 0;
                        font-size: 22px;
                        font-weight: bold;
                        color: #111827;
                    ">
                        #%s
                    </p>

                </div>

                <p style="
                    color: #4B5563;
                    font-size: 15px;
                    line-height: 1.7;
                ">
                    Feel free to reach out if you have any questions.
                </p>

                <hr style="
                    border: none;
                    border-top: 1px solid #E5E7EB;
                    margin: 35px 0;
                ">

                <p style="
                    color: #9CA3AF;
                    font-size: 13px;
                    text-align: center;
                    margin: 0;
                ">
                    Thank you for shopping with Shoppio.
                </p>

            </div>
        </div>
    """.formatted(orderId);

        sendHtmlEmail(
                email,
                "Your order has been delivered",
                html
        );
    }

    public void sendHtmlEmail(
            String to,
            String subject,
            String html
    ) {

        try {

            if (mailSender == null) {
                log.info("Skipping email to {} because no JavaMailSender is configured", to);
                return;
            }

            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true);

            helper.setFrom(resolveFromEmail());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);

            mailSender.send(message);

            log.info("Email sent to {}", to);

        } catch (MessagingException | MailException e) {

            log.error("Failed to send email to {}", to, e);

            throw new RuntimeException(
                    "Email service error: " + e.getMessage(),
                    e
            );
        }
    }

    private String buildVerificationEmailHtml(String link) {

        return """
            <div style="font-family: Arial; padding: 20px;">
                <h2>Verify your email</h2>

                <p>
                    Click the button below to verify your account:
                </p>

                <a href="%s"
                   style="
                       display:inline-block;
                       padding:10px 20px;
                       background:#4F46E5;
                       color:white;
                       text-decoration:none;
                       border-radius:6px;
                   ">
                   Verify Email
                </a>

                <p style="margin-top:20px;color:gray;">
                   This link expires in 24 hours.
                </p>
            </div>
            """.formatted(link);
    }

    private String resolveFrontendUrl() {
        if (frontendUrl == null || frontendUrl.isBlank()) {
            return "http://localhost:5173";
        }

        return frontendUrl.endsWith("/")
                ? frontendUrl.substring(0, frontendUrl.length() - 1)
                : frontendUrl;
    }

    private String resolveFromEmail() {
        if (fromEmail == null || fromEmail.isBlank()) {
            throw new IllegalStateException("Mail sender address is not configured");
        }

        return fromEmail;
    }
}
