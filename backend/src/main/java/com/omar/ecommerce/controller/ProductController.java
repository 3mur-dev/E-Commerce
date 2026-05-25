package com.omar.ecommerce.controller;

import com.omar.ecommerce.dtos.ApiResponse;
import com.omar.ecommerce.dtos.ProductResponse;
import com.omar.ecommerce.entities.User;
import com.omar.ecommerce.repositories.UserRepository;
import com.omar.ecommerce.services.ProductService;
import com.omar.ecommerce.util.ApiResponseUtil;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final UserRepository userRepository;

    @GetMapping
    public ApiResponse<List<ProductResponse>> getAllProducts() {
        return ApiResponseUtil.success( "Products fetched successfully", productService.findAll());
    }

    @GetMapping("/{productId}")
    public ApiResponse<ProductResponse> getProduct(@PathVariable Long productId) {
        return ApiResponseUtil.success("Product retrieved successfully", productService.getResponseById(productId));
    }

    @PostMapping("/{productId}/favorite")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<ProductResponse> toggleFavorite(
            @PathVariable Long productId,
            Authentication authentication
    ) {
        User user = requireUser(authentication);
        return ApiResponseUtil.success("Favorite status updated", productService.toggleFavorite(user, productId));
    }

    private User requireUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required"));
    }
}
