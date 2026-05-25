package com.omar.ecommerce.services;

import com.omar.ecommerce.dtos.OrderItemResponse;
import com.omar.ecommerce.dtos.OrderResponse;
import com.omar.ecommerce.dtos.OrderUserResponse;
import com.omar.ecommerce.entities.Order;
import com.omar.ecommerce.entities.OrderItem;
import com.omar.ecommerce.entities.OrderStatus;
import com.omar.ecommerce.entities.PaymentStatus;
import com.omar.ecommerce.events.events.OrderStatusChangedEvent;
import com.omar.ecommerce.repositories.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminOrderService {

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final EmailService emailService;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional(readOnly = true)
    public List<OrderResponse> findAllOrders() {
        return orderRepository.findAllWithUserAndItems().stream()
                .sorted(Comparator.comparing(Order::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .reversed()
                        .thenComparing(Order::getId, Comparator.reverseOrder()))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    @CacheEvict(value = "orders_list", allEntries = true)
    public OrderResponse updateStatus(Long orderId,
                                      OrderStatus status,
                                      String ip,
                                      String userAgent) {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String performedBy = authentication.getName();

        if (orderId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order id is required");
        }
        if (status == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status is required");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (status == OrderStatus.CANCELLED || status == OrderStatus.REFUNDED) {
            orderService.releaseReservedStock(orderId);
            order.setStatus(status);
            if (status == OrderStatus.CANCELLED) {
                if (order.getPaymentMethod() == com.omar.ecommerce.entities.PaymentMethod.CARD
                        && order.getPaymentStatus() != PaymentStatus.PAID) {
                    order.setPaymentStatus(PaymentStatus.FAILED);
                }
                if (order.getPaymentMethod() == com.omar.ecommerce.entities.PaymentMethod.CASH_ON_DELIVERY) {
                    order.setPaymentStatus(PaymentStatus.NOT_REQUIRED);
                }
            }
            if (status == OrderStatus.REFUNDED) {
                order.setPaymentStatus(PaymentStatus.REFUNDED);
            }
        } else {
            order.setStatus(status);
        }

        orderRepository.save(order);

        sendStatusNotification(order, status);

        applicationEventPublisher.publishEvent(
                new OrderStatusChangedEvent(
                        orderId,
                        status,
                        performedBy,
                        ip,
                        userAgent
                )
        );

        return toResponse(orderRepository.findByIdWithUserAndItems(orderId).orElse(order));
    }

    private OrderResponse toResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setOrderNumber(order.getOrderNumber());
        response.setStatus(order.getStatus());
        response.setPaymentMethod(order.getPaymentMethod());
        response.setPaymentStatus(order.getPaymentStatus());
        response.setCreationTimestamp(order.getCreatedAt());
        response.setTotal(order.getTotal());
        response.setUser(toUserResponse(order));
        response.setCustomerName(order.getCustomerName());
        response.setCustomerEmail(order.getCustomerEmail());
        response.setPhone(order.getPhone());
        response.setAddressLine1(order.getAddressLine1());
        response.setAddressLine2(order.getAddressLine2());
        response.setCity(order.getCity());
        response.setState(order.getState());
        response.setPostalCode(order.getPostalCode());
        response.setCountry(order.getCountry());
        response.setNote(order.getNote());
        response.setItems(order.getItems().stream().map(this::toItemResponse).toList());
        return response;
    }

    private OrderUserResponse toUserResponse(Order order) {
        if (order.getUser() == null) {
            return null;
        }

        OrderUserResponse userResponse = new OrderUserResponse();
        userResponse.setId(order.getUser().getId());
        userResponse.setName(order.getUser().getUsername());
        userResponse.setEmail(order.getUser().getEmail());
        return userResponse;
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        OrderItemResponse response = new OrderItemResponse();
        response.setId(item.getId());
        response.setProductId(item.getProduct().getId());
        response.setProductName(item.getProduct().getName());
        response.setName(item.getProduct().getName());
        response.setImageUrl(item.getProduct().getImageUrl());
        response.setQuantity(item.getQuantity());
        response.setPrice(item.getPrice());
        response.setSubtotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        return response;
    }

    private void sendStatusNotification(Order order, OrderStatus status) {
        String email = resolveNotificationEmail(order);
        if (email == null || email.isBlank()) {
            return;
        }

        try {
            if (status == OrderStatus.SHIPPED) {
                emailService.sendOrderShippedEmail(email, order.getId());
            }
            if (status == OrderStatus.DELIVERED) {
                emailService.sendOrderDeliveredEmail(email, order.getId());
            }
        } catch (Exception e) {
            log.warn("Order status email failed for order {} and email {}", order.getId(), email, e);
        }
    }

    private String resolveNotificationEmail(Order order) {
        if (order.getCustomerEmail() != null && !order.getCustomerEmail().isBlank()) {
            return order.getCustomerEmail();
        }
        if (order.getUser() != null) {
            return order.getUser().getEmail();
        }
        return null;
    }
}
