package com.omar.ecommerce.services;

import com.omar.ecommerce.entities.Favorite;
import com.omar.ecommerce.entities.Product;
import com.omar.ecommerce.entities.User;
import com.omar.ecommerce.repositories.FavoriteRepository;
import com.omar.ecommerce.repositories.ProductRepository;
import com.omar.ecommerce.repositories.UserRepository;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public List<Long> findUserFavorites(String username) {
        return favoriteRepository.findProductIdsByUsername(username);
    }

    @Transactional
    public boolean toggleFavorite(Long userId, Long productId) {

        Optional<Favorite> existing = favoriteRepository
                .findByUserIdAndProductId(userId, productId);

        if (existing.isPresent()) {
            try {
                favoriteRepository.delete(existing.get());
            } catch (Exception ignored) {
            }
            return false; // unfavorited
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Favorite favorite = new Favorite();
        favorite.setUser(user);
        favorite.setProduct(product);

        try {
            favoriteRepository.save(favorite);
            return true; // favorited
        } catch (Exception e) {
            //If duplicate insert happens, treat as already favorited
            return true;
        }
    }
}