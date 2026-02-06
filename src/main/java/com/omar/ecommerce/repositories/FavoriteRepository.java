package com.omar.ecommerce.repositories;

import com.omar.ecommerce.entities.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long>{

    Optional<Favorite> findByUserIdAndProductId(Long userId, Long productId);
    List<Favorite> findByUserId(Long userId);

    @Query("SELECT f.product.id FROM Favorite f WHERE f.user.username = :username")
    List<Long> findProductIdsByUsername(@Param("username") String username);

    void deleteByProductId(long id);
}