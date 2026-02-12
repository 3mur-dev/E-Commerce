package com.omar.ecommerce.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SupportController {

    @RequestMapping({"/support", "/support/", "/support.html"})
    public String supportPage() {
        return "support";
    }
}
