package com.omar.ecommerce.repositories;

import com.omar.ecommerce.entities.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;

public interface ProductRepository extends JpaRepository<Product, Long>,
        JpaSpecificationExecutor<Product> {

    // Search active products by name (list)
    @EntityGraph(attributePaths = "category")
    @Query("SELECT p FROM Product p WHERE COALESCE(p.deleted, false) = false AND LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Product> findByNameContainingIgnoreCase(String name);

    // Search active products by name (paged)
    @EntityGraph(attributePaths = "category")
    @Query("SELECT p FROM Product p WHERE COALESCE(p.deleted, false) = false AND LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

    // Fetch all active products (paged)
    @EntityGraph(attributePaths = "category")
    @Query("SELECT p FROM Product p WHERE COALESCE(p.deleted, false) = false")
    Page<Product> findAllActive(Pageable pageable);

    @EntityGraph(attributePaths = "category")
    @Lock(PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);

    @Modifying
    @Query("""
            UPDATE Product p
            SET p.stock = p.stock - :quantity
            WHERE p.id = :productId
              AND COALESCE(p.deleted, false) = false
              AND p.stock >= :quantity
            """)
    int reserveStock(@Param("productId") Long productId, @Param("quantity") int quantity);

    @Modifying
    @Query("""
            UPDATE Product p
            SET p.stock = p.stock + :quantity
            WHERE p.id = :productId
              AND COALESCE(p.deleted, false) = false
            """)
    int releaseStock(@Param("productId") Long productId, @Param("quantity") int quantity);

    // Check if active product exists by name
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Product p WHERE p.name = :name AND COALESCE(p.deleted, false) = false")
    boolean existsByName(String name);

    // Optional: fetch all active products
    @EntityGraph(attributePaths = "category")
    @Query("SELECT p FROM Product p WHERE COALESCE(p.deleted, false) = false")
    List<Product> findAllActiveProducts();

    @EntityGraph(attributePaths = {"category"})
    Optional<Product> findWithCategoryById(Long id);
}
