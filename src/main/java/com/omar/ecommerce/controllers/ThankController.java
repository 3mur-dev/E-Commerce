package com.omar.ecommerce.controllers;

import com.omar.ecommerce.entities.Order;
import com.omar.ecommerce.entities.OrderItem;
import com.omar.ecommerce.entities.User;
import com.omar.ecommerce.repositories.OrderItemRepository;
import com.omar.ecommerce.repositories.OrderRepository;
import com.omar.ecommerce.repositories.UserRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@AllArgsConstructor
@Slf4j
public class ThankController {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    @GetMapping("/thank")
    public String showThankYouPage(Model model,
                                   @AuthenticationPrincipal UserDetails principal) {

        log.info("=== Accessing /thank endpoint ===");

        if (principal == null) {
            log.warn("No authenticated user - redirecting to login");
            return "redirect:/login";
        }

        String username = principal.getUsername();
        log.info("Looking for user: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("User not found: {}", username);
                    return new IllegalStateException("User not found");
                });

        // Get the LAST order created by this user
        Order order = orderRepository.findTopByUserOrderByIdDesc(user).orElse(null);

        if (order == null) {
            log.warn("No recent order found for user: {}", username);
            return "redirect:/cart?empty";
        }

        log.info("Found order {} for user {}", order.getId(), username);

        // Get order items
        List<OrderItem> items = orderItemRepository.findByOrder(order);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String formattedDate = order.getCreationTimestamp().format(formatter);

        // Pass to template
        model.addAttribute("order", order);
        model.addAttribute("formattedDate", formattedDate);
        model.addAttribute("items", items);

        log.info("Thank you page rendered successfully for order: {}", order.getId());
        return "thank";
    }
}
