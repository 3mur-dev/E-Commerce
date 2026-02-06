package com.omar.ecommerce.controllers;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Date;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String login(HttpServletRequest request, Model model) {
        model.addAttribute("isAdminLogin", request.getParameter("admin") != null);
        model.addAttribute("usernamePlaceholder",
                request.getParameter("admin") != null ? "admin@shop.com" : "john@example.com");
        return "login";
    }
}
