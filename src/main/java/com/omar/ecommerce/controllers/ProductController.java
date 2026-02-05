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


    // LIST ALL
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
        if (userDetails != null) { // user is logged in
            // Get the User entity
            User user = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow();

            // Get or create cart
            Cart cart = cartRepository.findByUser(user)
                    .orElseGet(() -> {
                        Cart newCart = new Cart();
                        newCart.setUser(user);
                        return cartRepository.save(newCart);
                    });

            // Count total items
            List<CartItem> items = cartItemRepository.findByCart(cart);
            cartCount = items.stream().mapToInt(CartItem::getQuantity).sum();

            // Optional: pass items if you want mini cart preview
            model.addAttribute("items", items);
        }

        model.addAttribute("cartCount", cartCount); // THIS IS THE BADGE NUMBER
        model.addAttribute("productRequest", new ProductRequest());
        model.addAttribute("keyword", keyword);

        return "products";
    }

}