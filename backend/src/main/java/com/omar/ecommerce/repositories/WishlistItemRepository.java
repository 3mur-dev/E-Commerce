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

    void deleteByProductId(Long productId);

    @Query("""
select i from WishlistItem i
join fetch i.product
where i.wishlist.id = :wishlistId
""")
    List<WishlistItem> findByWishlistIdWithProduct(Long wishlistId);

    @Query("""
select wi from WishlistItem wi
where wi.wishlist.id = :wishlistId
and wi.product.id = :productId
and wi.wishlist.user.id = :userId
""")
    Optional<WishlistItem> findSecureItem(
            Long wishlistId,
            Long productId,
            Long userId
    );
}