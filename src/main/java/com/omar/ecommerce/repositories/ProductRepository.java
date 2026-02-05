package com.omar.ecommerce.repositories;

import com.omar.ecommerce.entities.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    // Must return List<Product>, NOT Object
    List<Product> findByNameContainingIgnoreCase(String name);

    boolean existsByName(String name);
}
