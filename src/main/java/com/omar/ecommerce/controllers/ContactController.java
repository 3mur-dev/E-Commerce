package com.omar.ecommerce.controllers;

import com.omar.ecommerce.dtos.MessageRequest;
import com.omar.ecommerce.services.MessageService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@AllArgsConstructor
public class ContactController {
    private final MessageService messageService;

    @GetMapping("/contact")
    public String contactPage(Model model) {
        model.addAttribute("messageRequest", new MessageRequest());
        return "contact";
    }

    @PostMapping("/contact/send")
    public String handleContactForm(@Valid @ModelAttribute MessageRequest request,
                                    BindingResult bindingResult,
                                    RedirectAttributes redirectAttributes,
                                    Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("messageRequest", request);
            return "contact";  // Return to form with errors
        }

        try {
            messageService.save(request);
            redirectAttributes.addFlashAttribute("success", "Message sent successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("fail", "Message hasn't been sent!");
        }
        return "redirect:/contact";
    }
}
