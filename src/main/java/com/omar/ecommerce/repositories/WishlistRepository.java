package com.omar.ecommerce.repositories;

import com.omar.ecommerce.entities.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    List<Wishlist> findByUserIdOrderByIdAsc(Long userId);

    Optional<Wishlist> findByUserIdAndId(Long userId, Long id);

    Optional<Wishlist> findByUserIdAndDefaultListTrue(Long userId);

    Optional<Wishlist> findByShareToken(String shareToken);
}
