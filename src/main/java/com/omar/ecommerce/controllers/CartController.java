package com.omar.ecommerce.controllers;

import com.omar.ecommerce.entities.*;
import com.omar.ecommerce.repositories.CartItemRepository;
import com.omar.ecommerce.repositories.CartRepository;
import com.omar.ecommerce.repositories.ProductRepository;
import com.omar.ecommerce.repositories.UserRepository;
import com.omar.ecommerce.services.CartService;
import com.omar.ecommerce.services.OrderService;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

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
    @Transactional
    public String showCart(Authentication auth, Model model) {
        if (auth == null || !auth.isAuthenticated()) return "redirect:/login";

        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        String username = userDetails.getUsername();
        User user = userRepository.findByUsername(username).get();


        Cart cart = cartService.getOrCreateCart(user);
        BigDecimal cartTotal = cartService.calculateTotalPrice(cart);

        List<CartItem> items = cartItemRepository.findByCart(cart);

        boolean hasOutOfStock = items.stream()
                .anyMatch(item -> item.getQuantity() > item.getProduct().getStock());

        model.addAttribute("hasOutOfStock", hasOutOfStock);
        model.addAttribute("cartTotal", cartTotal);
        model.addAttribute("cart", cart);
        model.addAttribute("items", items);

        return "cart";
    }

    @PostMapping("/add")
    public String addCart(Authentication auth, @RequestParam("productId") long productId) {
        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        String username = userDetails.getUsername();
        User user = userRepository.findByUsername(username).get();
        Product product = productRepository.findById(productId).get();

        CartItem cartItem = cartService.addToCart(product, user);
        return "redirect:/cart";
    }

    @PostMapping("/decrease")
    public String decrease(Authentication auth, @RequestParam("productId") long productId) {
        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        String username = userDetails.getUsername();
        User user = userRepository.findByUsername(username).get();
        Product product = productRepository.findById(productId).get();

        cartService.decreaseQuantity(user, product);
        return "redirect:/cart";
    }

    @PostMapping("/checkout")
    public String processCheckout(@AuthenticationPrincipal UserDetails principal) {
        if (principal == null) {
            return "redirect:/login";
        }

        User user = userRepository.findByUsername(principal.getUsername()).get();
        Order order = orderService.checkOut(user);  // ✅ Uses OrderService

        return "redirect:/thank";  // ✅ Redirects to ThankController
    }
}