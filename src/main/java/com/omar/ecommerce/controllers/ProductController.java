package com.omar.ecommerce.controllers;

import com.omar.ecommerce.dtos.ProductRequest;
import com.omar.ecommerce.dtos.ProductResponse;
import com.omar.ecommerce.entities.*;
import com.omar.ecommerce.repositories.*;
import com.omar.ecommerce.services.FavoriteService;
import com.omar.ecommerce.services.ProductService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final FavoriteService favoriteService;

    @GetMapping
    public String getAllProducts(Model model,
                                 @RequestParam(required = false) String keyword,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "") String sort,
                                 @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {

        Page<ProductResponse> productPage;

        if (keyword != null && !keyword.isBlank()) {
            productPage = productService.searchByName(keyword, sort, page);
        } else {
            productPage = productService.findAll(sort, page);
        }
        List<ProductResponse> products = productPage.getContent();

        if (userDetails != null) {
            String username = userDetails.getUsername();
            List<Long> userFavorites = favoriteService.findUserFavorites(username);
            System.out.println(" USER: " + username + " FAVORITES: " + userFavorites.size());

            for (ProductResponse product : products) {
                product.setFavorited(userFavorites.contains(product.getId()));
            }
        }


        int cartCount = 0;
        List<CartItem> items = List.of();
        if (userDetails != null) {
            String username = userDetails.getUsername();


            User user = userRepository.findByUsername(username).orElseThrow();


            Cart cart = cartRepository.findByUser(user).orElseGet(() -> {
                Cart newCart = new Cart();
                newCart.setUser(user);
                return cartRepository.save(newCart);
            });

            items = cartItemRepository.findByCart(cart);
            cartCount = items.stream().mapToInt(CartItem::getQuantity).sum();
        }

        model.addAttribute("products", products);
        model.addAttribute("currentPage", productPage.getNumber());
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("cartCount", cartCount);
        model.addAttribute("items", items);
        model.addAttribute("keyword", keyword);
        model.addAttribute("sort", sort);
        model.addAttribute("productRequest", new ProductRequest());

        return "products";
    }
}
