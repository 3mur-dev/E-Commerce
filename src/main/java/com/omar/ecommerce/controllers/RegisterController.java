package com.omar.ecommerce.controllers;

import com.omar.ecommerce.entities.Role;
import com.omar.ecommerce.entities.User;
import com.omar.ecommerce.repositories.UserRepository;
import com.omar.ecommerce.services.EmailService;
import com.omar.ecommerce.services.EmailVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.BindingResult;

@Controller
@RequiredArgsConstructor
public class RegisterController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;
    private final EmailService emailService;

    @Value("${app.verify.base-url:}")
    private String verifyBaseUrl;

    @GetMapping("/register")
    public String showRegisterForm(Model model,
                                   @RequestParam(value = "error", required = false) String error,
                                   @RequestParam(value = "success", required = false) String success) {
        model.addAttribute("user", new User());
        model.addAttribute("error", error);
        model.addAttribute("success", success);
        return "register";
    }


    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("user") User user,
                               BindingResult result,
                               HttpServletRequest request) {
        if (result.hasErrors()) {
            return "redirect:/register?error=Invalid+registration+details";
        }

        String normalizedUsername = user.getUsername() == null ? null : user.getUsername().trim();
        String normalizedEmail = user.getEmail() == null ? null : user.getEmail().trim();

        user.setUsername(normalizedUsername);
        user.setEmail(normalizedEmail);

        // Check if username or email already exists (including soft-deleted)
        if (normalizedUsername == null || normalizedUsername.isBlank()) {
            return "redirect:/register?error=Username+is+required";
        }
        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            return "redirect:/register?error=Email+is+required";
        }

        if (userRepository.existsByUsernameAny(normalizedUsername)) {
            return "redirect:/register?error=Username+already+exists";
        }
        if (userRepository.existsByEmailAny(normalizedEmail)) {
            return "redirect:/register?error=Email+already+exists";
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        user.setRole(Role.USER);
        user.setEmailVerified(false);

        userRepository.save(user);

        String verifyLink = buildVerifyLink(request, user);
        if (verifyLink == null) {
            return "redirect:/login?verify=failed";
        }

        try {
            emailService.sendVerificationEmail(user.getEmail(), verifyLink);
        } catch (Exception ex) {
            return "redirect:/login?verify=failed";
        }

        return "redirect:/login?verify=sent";
    }

    private String buildVerifyLink(HttpServletRequest request, User user) {
        return emailVerificationService.createToken(user)
                .map(token -> {
                    String baseUrl = resolveBaseUrl(request);
                    return baseUrl + "/verify-email?token=" + token;
                })
                .orElse(null);
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
