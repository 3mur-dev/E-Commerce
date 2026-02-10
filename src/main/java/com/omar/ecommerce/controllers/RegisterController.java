package com.omar.ecommerce.controllers;

import com.omar.ecommerce.entities.Role;
import com.omar.ecommerce.entities.User;
import com.omar.ecommerce.repositories.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
    public String registerUser(@Valid @ModelAttribute("user") User user, BindingResult result) {
        if (result.hasErrors()) {
            return "redirect:/register?error=Invalid+registration+details";
        }

        // Check if username or email already exists (including soft-deleted)
        if (userRepository.existsByUsernameAny(user.getUsername())) {
            return "redirect:/register?error=Username+already+exists";
        }
        if (userRepository.existsByEmailAny(user.getEmail())) {
            return "redirect:/register?error=Email+already+exists";
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        user.setRole(Role.USER);

        userRepository.save(user);

        return "redirect:/login?success=Account+created+successfully";
    }
}
