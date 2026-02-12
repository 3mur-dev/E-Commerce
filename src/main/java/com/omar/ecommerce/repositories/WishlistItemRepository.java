package com.omar.ecommerce.repositories;

import com.omar.ecommerce.entities.WishlistItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {

    @EntityGraph(attributePaths = {"product"})
    List<WishlistItem> findByWishlistIdOrderByAddedAtDesc(Long wishlistId);

    Optional<WishlistItem> findByWishlistIdAndProductId(Long wishlistId, Long productId);

    @Query("SELECT wi.product.id FROM WishlistItem wi WHERE wi.wishlist.id = :wishlistId")
    List<Long> findProductIdsByWishlistId(@Param("wishlistId") Long wishlistId);

    void deleteByProductId(Long productId);
}
