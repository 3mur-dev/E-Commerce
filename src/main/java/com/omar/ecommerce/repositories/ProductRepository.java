package com.omar.ecommerce.repositories;

import com.omar.ecommerce.entities.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // Search active products by name (list)
    @Query("SELECT p FROM Product p WHERE COALESCE(p.deleted, false) = false AND LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Product> findByNameContainingIgnoreCase(String name);

    // Search active products by name (paged)
    @Query("SELECT p FROM Product p WHERE COALESCE(p.deleted, false) = false AND LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

    // Fetch all active products (paged)
    @Query("SELECT p FROM Product p WHERE COALESCE(p.deleted, false) = false")
    Page<Product> findAllActive(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);

    // Check if active product exists by name
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Product p WHERE p.name = :name AND COALESCE(p.deleted, false) = false")
    boolean existsByName(String name);

    // Optional: fetch all active products
    @Query("SELECT p FROM Product p WHERE COALESCE(p.deleted, false) = false")
    List<Product> findAllActiveProducts();
}
