package com.omar.ecommerce.repositories;

import com.omar.ecommerce.entities.Cart;
import com.omar.ecommerce.entities.CartItem;
import com.omar.ecommerce.entities.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByCart(Cart cart);
    Optional<CartItem> findByProduct(Product product);
    Optional<CartItem> findByCartAndProduct(Cart cart, Product product);

    @Transactional
    void deleteByCart(Cart cart);

}
