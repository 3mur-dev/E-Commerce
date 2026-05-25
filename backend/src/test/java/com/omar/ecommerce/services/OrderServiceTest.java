package com.omar.ecommerce.services;

import com.omar.ecommerce.dtos.CheckoutRequest;
import com.omar.ecommerce.dtos.CheckoutResult;
import com.omar.ecommerce.entities.*;
import com.omar.ecommerce.repositories.CartRepository;
import com.omar.ecommerce.repositories.CartItemRepository;
import com.omar.ecommerce.repositories.OrderRepository;
import com.omar.ecommerce.repositories.ProductRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StripeCheckoutGateway stripeCheckoutService;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private PlatformTransactionManager transactionManager;

    private void stubTransactionManager() {
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
    }

    @Test
    void checkout_createsOrderAndClearsCart() {
        stubTransactionManager();
        CartService cartService = new CartService(cartRepository, cartItemRepository, productRepository);
        EmailService emailService = new EmailService(null);
        OrderService orderService = new OrderService(cartService, cartItemRepository, orderRepository, productRepository, emailService, stripeCheckoutService, applicationEventPublisher, transactionManager);

        User user = new User();
        user.setId(1L);
        user.setUsername("omar");

        Cart cart = new Cart();
        cart.setId(10L);
        cart.setUser(user);

        Product product = new Product();
        product.setId(99L);
        product.setName("Mouse");
        product.setPrice(new BigDecimal("25.00"));
        product.setStock(5);

        CartItem cartItem = new CartItem();
        cartItem.setId(7L);
        cartItem.setCart(cart);
        cartItem.setProduct(product);
        cartItem.setQuantity(2);

        CheckoutRequest request = new CheckoutRequest();
        request.setCustomerName("Omar");
        request.setCustomerEmail("omar@example.com");
        request.setPhone("1234567890");
        request.setAddressLine1("Main Street");
        request.setAddressLine2("");
        request.setCity("Kuwait City");
        request.setState("");
        request.setPostalCode("12345");
        request.setCountry("Kuwait");
        request.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        request.setNote("");
        request.setIdempotencyKey("checkout-123");

        when(orderRepository.findByCheckoutIdempotencyKey("checkout-123")).thenReturn(Optional.empty());
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCart(cart)).thenReturn(List.of(cartItem));
        when(productRepository.findWithCategoryById(99L)).thenReturn(Optional.of(product));
        when(productRepository.reserveStock(99L, 2)).thenReturn(1);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(50L);
            return order;
        });

        CheckoutResult result = orderService.checkout(user, request);
        Order order = result.getOrder();

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        verify(cartItemRepository).deleteAll(List.of(cartItem));
        verify(productRepository).reserveStock(99L, 2);

        assertEquals(50L, order.getId());
        assertEquals(new BigDecimal("50.00"), order.getTotal());
        assertEquals(5, product.getStock());
        assertNotNull(order.getItems());
        assertEquals(1, order.getItems().size());
        assertEquals(PaymentStatus.NOT_REQUIRED, order.getPaymentStatus());
        assertEquals("checkout-123", captor.getValue().getCheckoutIdempotencyKey());
        assertNull(result.getStripeCheckoutUrl());
    }

    @Test
    void checkout_createsStripeSessionForCardPayments() {
        stubTransactionManager();
        CartService cartService = new CartService(cartRepository, cartItemRepository, productRepository);
        EmailService emailService = new EmailService(null);
        OrderService orderService = new OrderService(cartService, cartItemRepository, orderRepository, productRepository, emailService, stripeCheckoutService, applicationEventPublisher, transactionManager);

        User user = new User();
        user.setId(1L);
        user.setUsername("omar");
        user.setEmail("omar@example.com");

        Cart cart = new Cart();
        cart.setId(10L);
        cart.setUser(user);

        Product product = new Product();
        product.setId(99L);
        product.setName("Mouse");
        product.setPrice(new BigDecimal("25.00"));
        product.setStock(5);

        CartItem cartItem = new CartItem();
        cartItem.setId(7L);
        cartItem.setCart(cart);
        cartItem.setProduct(product);
        cartItem.setQuantity(2);

        CheckoutRequest request = new CheckoutRequest();
        request.setCustomerName("Omar");
        request.setCustomerEmail("omar@example.com");
        request.setPhone("1234567890");
        request.setAddressLine1("Main Street");
        request.setAddressLine2("");
        request.setCity("Kuwait City");
        request.setState("");
        request.setPostalCode("12345");
        request.setCountry("Kuwait");
        request.setPaymentMethod(PaymentMethod.CARD);
        request.setNote("");
        request.setIdempotencyKey("checkout-card-123");

        when(orderRepository.findByCheckoutIdempotencyKey("checkout-card-123")).thenReturn(Optional.empty());
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCart(cart)).thenReturn(List.of(cartItem));
        when(productRepository.findWithCategoryById(99L)).thenReturn(Optional.of(product));
        when(productRepository.reserveStock(99L, 2)).thenReturn(1);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(51L);
            return order;
        });
        when(stripeCheckoutService.createCheckoutSession(any(Order.class))).thenReturn("https://checkout.stripe.test/session");

        CheckoutResult result = orderService.checkout(user, request);

        assertNotNull(result.getOrder());
        assertEquals("https://checkout.stripe.test/session", result.getStripeCheckoutUrl());
        assertEquals(PaymentStatus.PENDING, result.getOrder().getPaymentStatus());
        verify(productRepository).reserveStock(99L, 2);
        verify(stripeCheckoutService).createCheckoutSession(any(Order.class));
    }

    @Test
    void markPaymentPaid_marksOrderPaidWithoutTouchingStock() {
        CartService cartService = new CartService(cartRepository, cartItemRepository, productRepository);
        EmailService emailService = new EmailService(null);
        OrderService orderService = new OrderService(cartService, cartItemRepository, orderRepository, productRepository, emailService, stripeCheckoutService, applicationEventPublisher, null);

        User user = new User();
        user.setId(1L);
        user.setUsername("omar");

        Product product = new Product();
        product.setId(99L);
        product.setName("Mouse");
        product.setPrice(new BigDecimal("25.00"));
        product.setStock(5);

        OrderItem orderItem = new OrderItem();
        orderItem.setId(7L);
        orderItem.setQuantity(2);
        orderItem.setPrice(new BigDecimal("25.00"));

        Order order = new Order();
        order.setId(50L);
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setItems(List.of(orderItem));

        orderItem.setOrder(order);
        orderItem.setProduct(product);

        when(orderRepository.findByIdWithUserAndItems(50L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.markPaymentPaid(50L, "pi_123");

        assertEquals(5, product.getStock());
        assertEquals(OrderStatus.PROCESSING, order.getStatus());
        assertEquals(PaymentStatus.PAID, order.getPaymentStatus());
        assertEquals("pi_123", order.getStripePaymentIntentId());
    }

    @Test
    void markPaymentFailed_releasesReservedStockAndCancelsOrder() {
        CartService cartService = new CartService(cartRepository, cartItemRepository, productRepository);
        EmailService emailService = new EmailService(null);
        OrderService orderService = new OrderService(cartService, cartItemRepository, orderRepository, productRepository, emailService, stripeCheckoutService, applicationEventPublisher, null);

        User user = new User();
        user.setId(1L);
        user.setUsername("omar");

        Product product = new Product();
        product.setId(99L);
        product.setName("Mouse");
        product.setPrice(new BigDecimal("25.00"));
        product.setStock(3);

        OrderItem orderItem = new OrderItem();
        orderItem.setId(7L);
        orderItem.setQuantity(2);
        orderItem.setPrice(new BigDecimal("25.00"));

        Order order = new Order();
        order.setId(50L);
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setItems(List.of(orderItem));

        orderItem.setOrder(order);
        orderItem.setProduct(product);

        when(orderRepository.findByIdWithUserAndItems(50L)).thenReturn(Optional.of(order));
        when(productRepository.releaseStock(99L, 2)).thenReturn(1);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.markPaymentFailed(50L, "Stripe payment failed");

        verify(productRepository).releaseStock(99L, 2);
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals(PaymentStatus.FAILED, order.getPaymentStatus());
    }
}
