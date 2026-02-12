package com.omar.ecommerce.controllers;

import com.omar.ecommerce.entities.User;
import com.omar.ecommerce.repositories.UserRepository;
import com.omar.ecommerce.services.EmailService;
import com.omar.ecommerce.services.EmailVerificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;
    private final EmailService emailService;
    private final UserRepository userRepository;

    @Value("${app.verify.base-url:}")
    private String verifyBaseUrl;

    @GetMapping("/verify-email")
    public String verifyEmail(@RequestParam(required = false) String token, Model model) {
        if (emailVerificationService.verifyToken(token)) {
            model.addAttribute("success", "Email verified. You can now log in.");
            return "verify-email";
        }
        model.addAttribute("error", "Verification link is invalid or expired.");
        return "verify-email";
    }

    @GetMapping("/resend-verification")
    public String showResendVerification() {
        return "resend-verification";
    }

    @PostMapping("/resend-verification")
    public String resendVerification(@RequestParam String email,
                                     HttpServletRequest request,
                                     Model model) {
        if (email == null || email.isBlank()) {
            model.addAttribute("error", "Email is required.");
            return "resend-verification";
        }
        Optional<User> userOpt = userRepository.findByEmail(email.trim());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (Boolean.TRUE.equals(user.getEmailVerified())) {
                model.addAttribute("success", "That email is already verified.");
                return "resend-verification";
            }
            Optional<String> tokenOpt = emailVerificationService.createToken(user);
            if (tokenOpt.isPresent()) {
                String verifyLink = buildVerifyLink(request, tokenOpt.get());
                try {
                    emailService.sendVerificationEmail(email, verifyLink);
                } catch (Exception ex) {
                    model.addAttribute("error", "We could not send the verification email. Try again later.");
                    return "resend-verification";
                }
            }
        }

        model.addAttribute("success",
                "If that email exists, we sent a verification link. Check your inbox.");
        return "resend-verification";
    }

    private String buildVerifyLink(HttpServletRequest request, String token) {
        String baseUrl = resolveBaseUrl(request);
        return baseUrl + "/verify-email?token=" + token;
    }

    private String resolveBaseUrl(HttpServletRequest request) {
        if (verifyBaseUrl != null && !verifyBaseUrl.isBlank()) {
            return verifyBaseUrl.trim();
        }
        String scheme = request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();
        String portPart = (port == 80 || port == 443) ? "" : ":" + port;
        return scheme + "://" + host + portPart;
    }
}
