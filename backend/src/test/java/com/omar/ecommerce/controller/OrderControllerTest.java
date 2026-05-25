package com.omar.ecommerce.controller;

import com.omar.ecommerce.entities.Cart;
import com.omar.ecommerce.entities.CartItem;
import com.omar.ecommerce.entities.Order;
import com.omar.ecommerce.entities.Product;
import com.omar.ecommerce.entities.User;
import com.omar.ecommerce.repositories.CartItemRepository;
import com.omar.ecommerce.repositories.CartRepository;
import com.omar.ecommerce.repositories.OrderRepository;
import com.omar.ecommerce.repositories.ProductRepository;
import com.omar.ecommerce.repositories.UserRepository;
import com.omar.ecommerce.security.CustomUserDetails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private CartRepository cartRepository;

    @MockBean
    private CartItemRepository cartItemRepository;

    @MockBean
    private OrderRepository orderRepository;

    @MockBean
    private ProductRepository productRepository;

    @Test
    void checkout_returnsOrderResponse() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("omar");
        user.setPassword("encoded-password");
        user.setRole(com.omar.ecommerce.entities.Role.USER);

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

        when(userRepository.findByUsername("omar")).thenReturn(Optional.of(user));
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCart(cart)).thenReturn(List.of(cartItem));
        when(orderRepository.findByCheckoutIdempotencyKey("checkout-123")).thenReturn(Optional.empty());
        when(productRepository.findWithCategoryById(99L)).thenReturn(Optional.of(product));
        when(productRepository.reserveStock(99L, 2)).thenReturn(1);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order value = invocation.getArgument(0);
            value.setId(55L);
            value.setOrderNumber("ORD-123");
            return value;
        });

        mockMvc.perform(post("/api/orders/checkout")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                new CustomUserDetails(user),
                                null,
                                new CustomUserDetails(user).getAuthorities()
                        )))
                        .contentType("application/json")
                        .content("""
                                {
                                  "customerName": "Omar",
                                  "customerEmail": "omar@example.com",
                                  "phone": "1234567890",
                                  "addressLine1": "Main Street",
                                  "addressLine2": "",
                                  "city": "Kuwait City",
                                  "state": "",
                                  "postalCode": "12345",
                                  "country": "Kuwait",
                                  "paymentMethod": "CASH_ON_DELIVERY",
                                  "note": "",
                                  "idempotencyKey": "checkout-123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderNumber").value("ORD-123"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.paymentStatus").value("NOT_REQUIRED"));
    }
}
