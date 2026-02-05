package com.omar.ecommerce.controllers;

import com.omar.ecommerce.dtos.MessageRequest;
import com.omar.ecommerce.services.MessageService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@AllArgsConstructor
public class ContactController {

    private final MessageService messageService;

    @RequestMapping("/contact")
    public String contactPage() {
        return "contact";
    }

    @PostMapping("/contact/send")
    public String handleContactForm(@Valid @ModelAttribute MessageRequest request, RedirectAttributes redirectAttributes) {
        try {
            // Attempt to save the message
            messageService.save(request);

            // If successful, only add the success flash attribute
            redirectAttributes.addFlashAttribute("success", "Message sent successfully!");
        } catch (Exception e) {
            // If an error occurs, add the fail flash attribute instead
            redirectAttributes.addFlashAttribute("fail", "Message hasn't been sent!");
        }

        return "redirect:/contact";
    }
}