package com.omar.ecommerce.controller;

import com.omar.ecommerce.dtos.*;
import com.omar.ecommerce.entities.User;
import com.omar.ecommerce.repositories.UserRepository;
import com.omar.ecommerce.services.CartService;
import com.omar.ecommerce.services.OrderService;
import com.omar.ecommerce.util.ApiResponseUtil;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;
    private final OrderService orderService;
    private final UserRepository userRepository;

    @GetMapping
    public ApiResponse<CartResponse> getCart(Authentication authentication) {
        User user = requireUser(authentication);
        return ApiResponseUtil.success("Cart retrieved successfully", cartService.getCart(user));
    }

    @PostMapping("/add")
    public ApiResponse<CartResponse> addToCart(Authentication authentication, @Valid @RequestBody CartItemDTO request) {
        User user = requireUser(authentication);
        CartResponse updatedCart = cartService.addItem(user, request);
        return ApiResponseUtil.success("Item added to cart", updatedCart);
    }

    @PutMapping("/items/{cartItemId}")
    public ApiResponse<CartResponse> updateQuantity(
            Authentication authentication,
            @PathVariable Long cartItemId,
            @Valid @RequestBody QuantityRequest request
    ) {
        User user = requireUser(authentication);
        return ApiResponseUtil.success("Cart quantity updated", cartService.updateQuantity(user, cartItemId, request));
    }

    @DeleteMapping("/items/{cartItemId}")
    public ApiResponse<CartResponse> removeFromCart(
            Authentication authentication,
            @PathVariable Long cartItemId
    ) {
        User user = requireUser(authentication);
        return ApiResponseUtil.success("Item removed from cart", cartService.removeItem(user, cartItemId));
    }

    @PostMapping("/checkout")
    public ApiResponse<OrderResponse> checkout(Authentication authentication, @Valid @RequestBody CheckoutRequest request) {
        User user = requireUser(authentication);
        return ApiResponseUtil.success("Checkout completed", orderService.checkoutResponse(user, request));
    }

    private User requireUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required"));
    }
}
