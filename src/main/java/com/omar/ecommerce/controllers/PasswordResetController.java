package com.omar.ecommerce.controllers;

import com.omar.ecommerce.services.EmailService;
import com.omar.ecommerce.services.PasswordResetService;
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
public class PasswordResetController {

    private final PasswordResetService passwordResetService;
    private final EmailService emailService;

    @Value("${app.reset.show-link:false}")
    private boolean showResetLink;

    @GetMapping("/forgot-password")
    public String showForgotPassword() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String handleForgotPassword(@RequestParam String email,
                                       Model model) {
        String normalizedEmail = email == null ? null : email.trim();
        Optional<String> tokenOpt = passwordResetService.createResetToken(normalizedEmail);
        boolean mailFailed = false;

        if (tokenOpt.isPresent()) {
            String token = tokenOpt.get();
            try {
                emailService.sendPasswordResetEmail(normalizedEmail, token);
                if (showResetLink) {
                    model.addAttribute("resetCode", token);
                }
            } catch (Exception ex) {
                model.addAttribute("error", "We could not send the reset email. Please try again later.");
                mailFailed = true;
            }
        }

        if (!mailFailed) {
            model.addAttribute("success",
                    "If that email exists, we sent a verification code. Check your inbox.");
        }
        return "forgot-password";
    }

    @GetMapping("/reset-password")
    public String showResetPassword() {
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String handleResetPassword(@RequestParam String email,
                                      @RequestParam String code,
                                      @RequestParam String password,
                                      @RequestParam String confirm,
                                      Model model) {
        if (!password.equals(confirm)) {
            model.addAttribute("error", "Passwords do not match.");
            return "reset-password";
        }

        boolean success = passwordResetService.resetPasswordWithCode(email, code, password);
        if (!success) {
            model.addAttribute("error", "Reset code is invalid or expired.");
            return "reset-password";
        }

        model.addAttribute("success", "Password updated. You can now log in.");
        return "reset-password";
    }
}
