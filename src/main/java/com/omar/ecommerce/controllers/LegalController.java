package com.omar.ecommerce.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class LegalController {

    @RequestMapping({"/privacy", "/privacy/", "/privacy.html"})
    public String privacyPage() {
        return "privacy";
    }

    @RequestMapping({"/terms", "/terms/", "/terms.html"})
    public String termsPage() {
        return "terms";
    }
}
