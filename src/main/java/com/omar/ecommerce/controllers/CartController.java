package com.omar.ecommerce.controllers;

import com.omar.ecommerce.entities.*;
import com.omar.ecommerce.repositories.CartItemRepository;
import com.omar.ecommerce.repositories.CartRepository;
import com.omar.ecommerce.repositories.ProductRepository;
import com.omar.ecommerce.repositories.UserRepository;
import com.omar.ecommerce.services.CartService;
import com.omar.ecommerce.services.OrderService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/cart")
@AllArgsConstructor
public class CartController {

    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final CartService cartService;
    private final ProductRepository productRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderService orderService;

    @GetMapping
    public String showCart(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails == null) return "redirect:/login"; // prevent null user

        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Cart cart = cartService.getOrCreateCart(user);
        cart.getUser().getId(); // force lazy load

        List<CartItem> items = cartItemRepository.findByCart(cart);

        BigDecimal cartTotal = cartService.calculateTotalPrice(cart);

        boolean hasOutOfStock = items.stream()
                .anyMatch(item -> item.getQuantity() > item.getProduct().getStock());

        model.addAttribute("hasOutOfStock", hasOutOfStock);
        model.addAttribute("cartTotal", cartTotal);
        model.addAttribute("cart", cart);
        model.addAttribute("items", items);

        return "cart";
    }

    @PostMapping("/add")
    public String AddCart(@AuthenticationPrincipal UserDetails userDetails,
                          @RequestParam("productId") long productId) {

        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));


        Product product = productRepository.findById(productId).orElseThrow();

        CartItem cartItem = cartService.addToCart(product, user);

        return "redirect:/cart";
    }

    @PostMapping("/decrease")
    public String Decrease(Model model, @AuthenticationPrincipal UserDetails userDetails,
                           @RequestParam("productId") long productId) {

        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Product product = productRepository.findById(productId).orElseThrow();
        cartService.decreaseQuantity(user, product);
        return "redirect:/cart";
    }

    @PostMapping("/checkout")
    public String checkOut(@AuthenticationPrincipal UserDetails userDetails, RedirectAttributes ra) {
        // Just redirect - ThankController handles everything
        return "redirect:/thank";
    }
}