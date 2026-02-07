package com.omar.ecommerce.repositories;

import com.omar.ecommerce.entities.Order;
import com.omar.ecommerce.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Integer> {
    Optional<Order> findTopByUserOrderByIdDesc(User user);

}
