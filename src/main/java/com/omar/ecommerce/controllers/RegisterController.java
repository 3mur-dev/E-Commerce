package com.omar.ecommerce.controllers;

import com.omar.ecommerce.entities.Role;
import com.omar.ecommerce.entities.User;
import com.omar.ecommerce.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
    public String registerUser(@ModelAttribute("user") User user) {

        // Check if username already exists
        if (userRepository.existsByUsername(user.getUsername())) {
            return "redirect:/register?error=Username+already+exists";
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        user.setRole(Role.ADMIN);

        userRepository.save(user);

        return "redirect:/login?success=Account+created+successfully";
    }
}