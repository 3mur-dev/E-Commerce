package com.omar.ecommerce.controllers;

import com.omar.ecommerce.dtos.ProductRequest;
import com.omar.ecommerce.entities.Cart;
import com.omar.ecommerce.entities.CartItem;
import com.omar.ecommerce.entities.Category;
import com.omar.ecommerce.entities.User;
import com.omar.ecommerce.repositories.*;
import com.omar.ecommerce.services.ProductService;
import lombok.AllArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@AllArgsConstructor
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final CartRepository cartRepository;



    @GetMapping
    public String getAllProducts(Model model,
                                 @RequestParam(required = false) String keyword,
                                 @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {

        // Products listing
        if (keyword != null && !keyword.isBlank()) {
            model.addAttribute("products", productService.searchByName(keyword));
        } else {
            model.addAttribute("products", productService.findAll());
        }

        // Cart badge logic
        int cartCount = 0;
        List<CartItem> items = List.of(); // Default empty list
        if (userDetails != null) {
            User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
            Cart cart = cartRepository.findByUser(user).orElseGet(() -> {
                Cart newCart = new Cart();
                newCart.setUser(user);
                return cartRepository.save(newCart);
            });
            items = cartItemRepository.findByCart(cart);
            cartCount = items.stream().mapToInt(CartItem::getQuantity).sum();
        }

        // Add these attributes for the template
        model.addAttribute("cartCount", cartCount);
        model.addAttribute("items", items); // For potential mini-cart
        model.addAttribute("keyword", keyword);
        model.addAttribute("productRequest", new ProductRequest());

        return "products";
    }

}