package com.omar.ecommerce.repositories;

import com.omar.ecommerce.entities.Order;
import com.omar.ecommerce.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Integer> {
    Optional<Order> findTopByUserOrderByIdDesc(User user);

}
