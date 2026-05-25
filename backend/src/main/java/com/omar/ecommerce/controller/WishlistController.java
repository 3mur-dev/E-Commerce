package com.omar.ecommerce.controller;

import com.omar.ecommerce.dtos.ApiResponse;
import com.omar.ecommerce.dtos.WishlistResponse;
import com.omar.ecommerce.entities.User;
import com.omar.ecommerce.repositories.UserRepository;
import com.omar.ecommerce.services.WishlistService;
import com.omar.ecommerce.util.ApiResponseUtil;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;
    private final UserRepository userRepository;

    @GetMapping
    public ApiResponse<WishlistResponse> getWishlist(Authentication authentication) {
        User user = requireUser(authentication);
        return ApiResponseUtil.success("Wishlist retrieved successfully", wishlistService.getWishlist(user));
    }

    @DeleteMapping("/{wishListId}/items/{productId}")
    public ApiResponse<WishlistResponse> removeWishlistItem(
                                           Authentication authentication,
                                           @PathVariable Long wishListId,
                                           @PathVariable Long productId
    ) {
        User user = requireUser(authentication);
        return ApiResponseUtil.success("Item removed from wishlist", wishlistService.deleteWishlistItem(user, wishListId, productId));
    }

    private User requireUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required"));
    }
}
