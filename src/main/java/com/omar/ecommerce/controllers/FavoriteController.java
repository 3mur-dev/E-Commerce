package com.omar.ecommerce.controllers;

import com.omar.ecommerce.entities.User;
import com.omar.ecommerce.repositories.UserRepository;
import com.omar.ecommerce.services.FavoriteService;
import com.omar.ecommerce.services.WishlistService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;

    @RestController
    @RequestMapping("/products/favorites")
    @AllArgsConstructor
    public class FavoriteController {

    private final UserRepository userRepository;
    private final FavoriteService favoriteService;
    private final WishlistService wishlistService;

    @PostMapping("/toggle")
    public ResponseEntity<String> toggleFavorite(
            @RequestParam Long productId,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(401).body("Not logged in");
        }

        String username = userDetails.getUsername();
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body("User not found");
        }

        User user = userOpt.get();
        boolean isNowFavorite = favoriteService.toggleFavorite(user.getId(), productId);
        try {
            if (isNowFavorite) {
                wishlistService.addToDefaultList(user, productId);
            } else {
                wishlistService.removeFromDefaultList(user, productId);
            }
        } catch (Exception ignored) {
        }

        return ResponseEntity.ok(isNowFavorite ? "favorited" : "unfavorited");
    }
}
