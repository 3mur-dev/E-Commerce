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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
                                 @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails){

        List<ProductResponse> products;
        int currentPage = page;
        int totalPages = 1;
        long totalItems = 0;

        if (keyword != null && !keyword.isBlank()) {
            // Search with sort + pagination
            products = productService.searchByName(keyword, sort, page);
        } else {
            // All products with sort + pagination
            products = productService.findAll(sort, page);
        }

        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalItems", totalItems);

        if (userDetails != null) {
            String username = userDetails.getUsername();
            List<Long> userFavorites = favoriteService.findUserFavorites(username);

            System.out.println(" USER: " + username + " FAVORITES: " + userFavorites.size());

            for (ProductResponse product : products) {
                boolean isFavorited = userFavorites.contains(product.getId());
                product.setFavorited(isFavorited);
            }
        }

        int cartCount = 0;
        List<CartItem> items = List.of();
        if (userDetails != null) {
            User user = userRepository.findByUsername(userDetails.getUsername()).get();
            Cart cart = cartRepository.findByUser(user).orElseGet(() -> {
                Cart newCart = new Cart();
                newCart.setUser(user);
                return cartRepository.save(newCart);
            });
            items = cartItemRepository.findByCart(cart);
            cartCount = items.stream().mapToInt(CartItem::getQuantity).sum();
        }

        model.addAttribute("products", products);
        model.addAttribute("cartCount", cartCount);
        model.addAttribute("items", items);
        model.addAttribute("keyword", keyword);
        model.addAttribute("sort", sort);
        model.addAttribute("productRequest", new ProductRequest());

        products.forEach(p -> System.out.println("Product " + p.getName() + " imageUrl: '" + p.getImageUrl() + "'"));

        return "products";
    }
}