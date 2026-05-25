package com.omar.ecommerce.controller;

import com.omar.ecommerce.dtos.ApiResponse;
import com.omar.ecommerce.dtos.CheckoutRequest;
import com.omar.ecommerce.dtos.OrderResponse;
import com.omar.ecommerce.entities.User;
import com.omar.ecommerce.repositories.UserRepository;
import com.omar.ecommerce.services.OrderService;
import com.omar.ecommerce.util.ApiResponseUtil;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final UserRepository userRepository;

    @PostMapping("/checkout")
    public ApiResponse<OrderResponse> checkout(Authentication authentication, @Valid @RequestBody CheckoutRequest request) {
        User user = requireUser(authentication);
        return ApiResponseUtil.success("Checkout completed", orderService.checkoutResponse(user, request));
    }

    @GetMapping("/me")
    public ApiResponse<List<OrderResponse>> myOrders(Authentication authentication) {
        User user = requireUser(authentication);
        return ApiResponseUtil.success("Orders retrieved successfully", orderService.getMyOrdersResponse(user));
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderResponse> getOrder(Authentication authentication, @PathVariable Long id) {
        User user = requireUser(authentication);
        return ApiResponseUtil.success("Order retrieved successfully", orderService.getOrderResponse(user, id));
    }

    private User requireUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required"));
    }
}
