package com.omar.ecommerce.repositories;

import com.omar.ecommerce.entities.Order;
import com.omar.ecommerce.entities.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrder(Order order);
}
