package com.omar.ecommerce.controllers;

import com.omar.ecommerce.entities.User;
import com.omar.ecommerce.repositories.UserRepository;
import com.omar.ecommerce.services.FavoriteService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;

    @RestController
    @RequestMapping("/products/favorites")
    @AllArgsConstructor
    public class FavoriteController {

    private final UserRepository userRepository;
    private final FavoriteService favoriteService;

    @PostMapping("/toggle")
    public ResponseEntity<String> toggleFavorite(
            @RequestParam Long productId,
            Authentication auth) {

        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body("Not logged in");
        }

        String username = auth.getName();
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body("User not found");
        }

        User user = userOpt.get();
        boolean isNowFavorite = favoriteService.toggleFavorite(user.getId(), productId);

        return ResponseEntity.ok(isNowFavorite ? "favorited" : "unfavorited");
    }
}