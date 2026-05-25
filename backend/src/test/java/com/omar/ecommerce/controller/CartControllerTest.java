package com.omar.ecommerce.controller;

import com.omar.ecommerce.entities.Cart;
import com.omar.ecommerce.entities.CartItem;
import com.omar.ecommerce.entities.Product;
import com.omar.ecommerce.entities.User;
import com.omar.ecommerce.repositories.CartItemRepository;
import com.omar.ecommerce.repositories.CartRepository;
import com.omar.ecommerce.repositories.OrderRepository;
import com.omar.ecommerce.repositories.ProductRepository;
import com.omar.ecommerce.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private ProductRepository productRepository;

    @MockBean
    private CartRepository cartRepository;

    @MockBean
    private CartItemRepository cartItemRepository;

    @MockBean
    private OrderRepository orderRepository;

    @Test
    @WithMockUser(username = "omar")
    void addToCart_returnsUpdatedCart() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("omar");

        Product product = new Product();
        product.setId(7L);
        product.setName("Keyboard");
        product.setPrice(new BigDecimal("24.99"));
        product.setStock(10);

        Cart cart = new Cart();
        cart.setId(10L);
        cart.setUser(user);

        AtomicReference<CartItem> savedItem = new AtomicReference<>();

        when(userRepository.findByUsername("omar")).thenReturn(Optional.of(user));
        when(productRepository.findById(7L)).thenReturn(Optional.of(product));
        when(cartRepository.findByUser(user)).thenReturn(Optional.empty(), Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> {
            Cart value = invocation.getArgument(0);
            value.setId(10L);
            value.setUser(user);
            return value;
        });
        when(cartItemRepository.findByCartAndProduct(cart, product)).thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> {
            CartItem value = invocation.getArgument(0);
            value.setId(1L);
            savedItem.set(value);
            return value;
        });
        when(cartItemRepository.findByCart(cart)).thenAnswer(invocation -> savedItem.get() == null
                ? List.of()
                : List.of(savedItem.get()));

        mockMvc.perform(post("/api/cart/add")
                        .contentType("application/json")
                        .content("""
                                {
                                  "productId": 7,
                                  "quantity": 2
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(49.98))
                .andExpect(jsonPath("$.data.items[0].productName").value("Keyboard"))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2));
    }
}
