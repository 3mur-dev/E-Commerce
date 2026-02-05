package com.omar.ecommerce.services;

import com.omar.ecommerce.entities.*;
import com.omar.ecommerce.repositories.*;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class OrderService {

    private final CartService cartService;
    private final CartItemRepository cartItemRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public Order checkOut(User user) {

        Cart cart = cartService.getOrCreateCart(user);

        List<CartItem> items = cartItemRepository.findByCart(cart);

        if (items.isEmpty()) return null;

        Order order = new Order();
        order.setUser(user);
        order.setCreationTimestamp(LocalDateTime.now());
        order.setStatus("pending");
        order.setTotal(0.0);
        orderRepository.save(order);

        double total = 0;
        for (CartItem item : items) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(item.getProduct());
            orderItem.setQuantity(item.getQuantity());
            orderItem.setPrice(item.getProduct().getPrice());
            orderItemRepository.save(orderItem);

            total += item.getProduct().getPrice() * item.getQuantity();
        }

        order.setTotal(total);
        orderRepository.save(order);

        // Properly clear cart items without affecting Cart
        cart.getItems().clear(); // works if orphanRemoval=true
        // OR: cartItemRepository.deleteByCart(cart);

        return order;
    }
}