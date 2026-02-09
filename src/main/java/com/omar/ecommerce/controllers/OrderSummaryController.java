package com.omar.ecommerce.controllers;

import com.omar.ecommerce.entities.User;
import com.omar.ecommerce.repositories.OrderRepository;
import com.omar.ecommerce.repositories.UserRepository;
import com.omar.ecommerce.services.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@RequiredArgsConstructor
@Controller
public class OrderSummaryController {

private final OrderService orderService;
private final UserRepository userRepository;

    @GetMapping("/order-summary")
    public String orderSummary(Model model, Authentication auth) {

        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        String username = userDetails.getUsername();
        User user = userRepository.findByUsername(username).get();

        model.addAttribute("orders", orderService.findByUser(user));
        return "order-summary";
    }


}
