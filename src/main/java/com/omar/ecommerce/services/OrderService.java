package com.omar.ecommerce.services;

import com.omar.ecommerce.entities.*;
import com.omar.ecommerce.repositories.*;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class OrderService {

    private final CartService cartService;
    private final CartItemRepository cartItemRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CartRepository cartRepository;

    @Transactional
    public Order checkOut(User user) {

        // Get user's cart
        Cart cart = cartService.getOrCreateCart(user);
        List<CartItem> items = cartItemRepository.findByCart(cart);

        System.out.println("=== CHECKOUT DEBUG ===");
        System.out.println("Cart ID: " + cart.getId());
        System.out.println("Cart Items Count: " + items.size());
        items.forEach(item ->
                System.out.println("Item: " + item.getProduct().getName() + " Qty: " + item.getQuantity())
        );

        if (items.isEmpty()) {
            throw new IllegalStateException("Cart is empty");
        }

        // Create order
        Order order = new Order();
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING);
        order.setCreationTimestamp(LocalDateTime.now());
        order.setItems(new ArrayList<>());
        order.setTotal(BigDecimal.ZERO);

        BigDecimal total = BigDecimal.ZERO;

        // Process each cart item
        for (CartItem ci : items) {
            Product product = productRepository.findById(ci.getProduct().getId())
                    .orElseThrow(() -> new IllegalStateException("Product not found: " + ci.getProduct().getId()));

            // Check stock
            if (ci.getQuantity() > product.getStock()) {
                throw new RuntimeException("Not enough stock for product: " + product.getName()
                );
            }

            // Deduct stock
            product.setStock(product.getStock() - ci.getQuantity());
            productRepository.save(product); // part of transaction

            // Create OrderItem
            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setProduct(product);
            oi.setQuantity(ci.getQuantity());
            oi.setPrice(product.getPrice());

            // Add to order
            order.getItems().add(oi);

            // Update total
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(ci.getQuantity())));
        }

        // Set total
        order.setTotal(total);

        // Save order (cascades to order items)
        Order savedOrder = orderRepository.save(order);

        // Clear cart items from DB
        cartItemRepository.deleteAll(items);

        return savedOrder;
    }
}