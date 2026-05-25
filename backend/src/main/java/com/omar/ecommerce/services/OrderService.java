package com.omar.ecommerce.services;

import com.omar.ecommerce.dtos.CheckoutRequest;
import com.omar.ecommerce.dtos.CheckoutResult;
import com.omar.ecommerce.dtos.OrderItemResponse;
import com.omar.ecommerce.dtos.OrderResponse;
import com.omar.ecommerce.dtos.OrderUserResponse;
import com.omar.ecommerce.entities.*;
import com.omar.ecommerce.events.events.OrderPaymentStatusChangedEvent;
import com.omar.ecommerce.repositories.CartItemRepository;
import com.omar.ecommerce.repositories.OrderRepository;
import com.omar.ecommerce.repositories.ProductRepository;
import com.omar.ecommerce.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final CartService cartService;
    private final CartItemRepository cartItemRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final EmailService emailService;
    private final StripeCheckoutGateway stripeCheckoutService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final PlatformTransactionManager transactionManager;

    @CacheEvict(value = "orders_list", allEntries = true)
    public CheckoutResult checkout(User user, CheckoutRequest request) {
        Order order = new TransactionTemplate(transactionManager).execute(status -> createOrder(user, request));
        if (order == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Checkout could not be completed");
        }

        String stripeCheckoutUrl = null;

        try {
            if (order.getPaymentMethod() == PaymentMethod.CARD) {
                stripeCheckoutUrl = stripeCheckoutService.createCheckoutSession(order);
                if (stripeCheckoutUrl == null || stripeCheckoutUrl.isBlank()) {
                    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Stripe checkout session URL was not created");
                }
            }

            sendOrderReceivedEmail(order, user);

            return new CheckoutResult(order, stripeCheckoutUrl);
        } catch (ResponseStatusException e) {
            if (order.getPaymentMethod() == PaymentMethod.CARD) {
                cancelCheckoutAfterFailure(order.getId(), e.getReason() == null ? "Stripe checkout failed" : e.getReason());
            }
            throw e;
        } catch (Exception e) {
            log.error("Checkout failed for user {}: {}", user.getUsername(), e.getMessage(), e);
            if (order.getPaymentMethod() == PaymentMethod.CARD) {
                cancelCheckoutAfterFailure(order.getId(), "Checkout failed");
            }
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Checkout failed: " + e.getMessage(),
                    e
            );
        }
    }

    public OrderResponse checkoutResponse(User user, CheckoutRequest request) {
        CheckoutResult checkoutResult = checkout(user, request);
        return toResponse(checkoutResult.getOrder(), checkoutResult.getStripeCheckoutUrl());
    }

    @Cacheable(value = "orders_list", key = "#user.id")
    public List<OrderResponse> getMyOrdersResponse(User user) {

        List<Order> orders = orderRepository.findFullOrdersByUserId(user.getId());

        return orders.stream()
                .map(this::toResponse)
                .toList();
    }

    public Order getOrder(Long id, User user) {
        return orderRepository.findFullOrderByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }

    public OrderResponse getOrderResponse(User user, Long id) {
        return toResponse(getOrder(id, user));
    }

    private Order createOrder(User user, CheckoutRequest request) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Checkout request is required");
        }
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            Order existing = orderRepository.findByCheckoutIdempotencyKey(request.getIdempotencyKey()).orElse(null);
            if (existing != null) {
                if (!existing.getUser().getId().equals(user.getId())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Checkout key already used");
                }
                return existing;
            }
        }

        Cart cart = cartService.getOrCreateCart(user);
        List<CartItem> items = cartItemRepository.findByCart(cart);
        if (items.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart is empty");
        }

        Order order = new Order();
        order.setOrderNumber("ORD-" + System.currentTimeMillis());
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentMethod(request.getPaymentMethod());
        order.setPaymentStatus(request.getPaymentMethod() == PaymentMethod.CASH_ON_DELIVERY
                ? PaymentStatus.NOT_REQUIRED
                : PaymentStatus.PENDING);
        order.setCheckoutIdempotencyKey(request.getIdempotencyKey());
        order.setCustomerName(request.getCustomerName());
        order.setCustomerEmail(request.getCustomerEmail());
        order.setPhone(request.getPhone());
        order.setAddressLine1(request.getAddressLine1());
        order.setAddressLine2(request.getAddressLine2());
        order.setCity(request.getCity());
        order.setState(request.getState());
        order.setPostalCode(request.getPostalCode());
        order.setCountry(request.getCountry());
        order.setNote(request.getNote());
        order.setItems(new ArrayList<>());
        order.setTotal(BigDecimal.ZERO);

        BigDecimal total = BigDecimal.ZERO;
        for (CartItem ci : items) {
            Product product = productRepository.findWithCategoryById(ci.getProduct().getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

            int reserved = productRepository.reserveStock(product.getId(), ci.getQuantity());
            if (reserved == 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Not enough stock for product: " + product.getName());
            }

            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setProduct(product);
            oi.setQuantity(ci.getQuantity());
            oi.setPrice(product.getPrice());
            order.getItems().add(oi);

            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(ci.getQuantity())));
        }

        order.setTotal(total);
        Order savedOrder = orderRepository.save(order);
        cartItemRepository.deleteAll(items);
        return savedOrder;
    }

    @Transactional
    @CacheEvict(value = "orders_list", allEntries = true)
    public void markPaymentPaid(Long orderId, String paymentIntentId) {
        Order order = orderRepository.findByIdWithUserAndItems(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (order.getPaymentStatus() == PaymentStatus.PAID || order.getStatus() == OrderStatus.CANCELLED) {
            return; // prevent duplicate webhook processing
        }

        order.setStatus(OrderStatus.PROCESSING);
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setStripePaymentIntentId(paymentIntentId);

        orderRepository.save(order);

        Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String performedBy = authentication == null ? "system" : authentication.getName();

        applicationEventPublisher.publishEvent(
                new OrderPaymentStatusChangedEvent(orderId, PaymentStatus.PAID.name(), performedBy, paymentIntentId)
        );
    }

    @Transactional
    @CacheEvict(value = "orders_list", allEntries = true)
    public void markPaymentFailed(Long orderId, String reason) {
        Order order = orderRepository.findByIdWithUserAndItems(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (order.getPaymentStatus() == PaymentStatus.PAID || order.getPaymentStatus() == PaymentStatus.FAILED
                || order.getStatus() == OrderStatus.CANCELLED) {
            return;
        }

        releaseReservedStock(order);
        order.setStatus(OrderStatus.CANCELLED);
        order.setPaymentStatus(PaymentStatus.FAILED);
        orderRepository.save(order);

        log.warn("Marked order {} as payment failed: {}", orderId, reason);
    }

    @Transactional
    @CacheEvict(value = "orders_list", allEntries = true)
    public void releaseReservedStock(Long orderId) {
        Order order = orderRepository.findByIdWithUserAndItems(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.REFUNDED
                || order.getPaymentStatus() == PaymentStatus.FAILED
                || order.getPaymentStatus() == PaymentStatus.REFUNDED) {
            return;
        }
        releaseReservedStock(order);
    }

    private void releaseReservedStock(Order order) {
        for (OrderItem item : order.getItems()) {
            int released = productRepository.releaseStock(item.getProduct().getId(), item.getQuantity());
            if (released == 0) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
            }
        }
    }

    private void cancelCheckoutAfterFailure(Long orderId, String reason) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            Order order = orderRepository.findByIdWithUserAndItems(orderId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

            if (order.getPaymentStatus() == PaymentStatus.PAID || order.getPaymentStatus() == PaymentStatus.FAILED
                    || order.getStatus() == OrderStatus.CANCELLED) {
                return;
            }

            releaseReservedStock(order);
            order.setStatus(OrderStatus.CANCELLED);
            order.setPaymentStatus(PaymentStatus.FAILED);
            orderRepository.save(order);

            log.warn("Cancelled checkout for order {}: {}", orderId, reason);
        });
    }

    private void sendOrderReceivedEmail(Order order, User user) {
        String email = resolveNotificationEmail(order, user);
        if (email == null || email.isBlank()) {
            return;
        }

        try {
            emailService.sendOrderReceivedEmail(email);
        } catch (Exception e) {
            log.warn("Order confirmation email failed for order {} and email {}", order.getId(), email, e);
        }
    }

    private String resolveNotificationEmail(Order order, User user) {
        if (order != null && order.getCustomerEmail() != null && !order.getCustomerEmail().isBlank()) {
            return order.getCustomerEmail();
        }
        if (user != null) {
            return user.getEmail();
        }
        return null;
    }

    private OrderResponse toResponse(Order order) {
        return toResponse(order, null);
    }

    private OrderResponse toResponse(Order order, String stripeCheckoutUrl) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setOrderNumber(order.getOrderNumber());
        response.setStatus(order.getStatus());
        response.setPaymentMethod(order.getPaymentMethod());
        response.setPaymentStatus(order.getPaymentStatus());
        response.setCreationTimestamp(order.getCreatedAt());
        response.setTotal(order.getTotal());
        response.setSessionUrl(stripeCheckoutUrl);
        response.setStripeCheckoutUrl(stripeCheckoutUrl);
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

    private OrderItemResponse toItemResponse(OrderItem item) {
        OrderItemResponse response = new OrderItemResponse();

        Product product = item.getProduct();

        response.setId(item.getId());

        response.setProductId(product != null ? product.getId() : null);
        response.setProductName(product != null ? product.getName() : null);
        response.setName(product != null ? product.getName() : null);
        response.setImageUrl(product != null ? product.getImageUrl() : null);

        response.setQuantity(item.getQuantity());
        response.setPrice(item.getPrice());
        response.setSubtotal(
                item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
        );

        return response;
    }

    private OrderUserResponse toUserResponse(Order order) {
        User user = order.getUser();

        if (user == null) return null;

        OrderUserResponse response = new OrderUserResponse();
        response.setId(user.getId());
        response.setName(user.getUsername());
        response.setEmail(user.getEmail());
        return response;
    }
}
