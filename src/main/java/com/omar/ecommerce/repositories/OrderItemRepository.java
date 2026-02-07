package com.omar.ecommerce.repositories;

import com.omar.ecommerce.entities.Order;
import com.omar.ecommerce.entities.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrder(Order order);

    @Query("SELECT oi FROM OrderItem oi JOIN FETCH oi.product p WHERE oi.order = :order")
    List<OrderItem> findByOrderWithProduct(@Param("order") Order order);
}
