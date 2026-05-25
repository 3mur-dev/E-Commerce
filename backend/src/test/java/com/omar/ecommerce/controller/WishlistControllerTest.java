package com.omar.ecommerce.controller;

import com.omar.ecommerce.entities.User;
import com.omar.ecommerce.entities.Wishlist;
import com.omar.ecommerce.entities.WishlistVisibility;
import com.omar.ecommerce.repositories.ProductRepository;
import com.omar.ecommerce.repositories.UserRepository;
import com.omar.ecommerce.repositories.WishlistItemRepository;
import com.omar.ecommerce.repositories.WishlistRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WishlistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private WishlistRepository wishlistRepository;

    @MockBean
    private WishlistItemRepository wishlistItemRepository;

    @MockBean
    private ProductRepository productRepository;

    @Test
    void getWishlist_returnsWishlistResponse() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("omar");
        user.setPassword("password123");
        user.setRole(com.omar.ecommerce.entities.Role.USER);

        Wishlist wishlist = new Wishlist();
        wishlist.setId(9L);
        wishlist.setVisibility(WishlistVisibility.PRIVATE);
        wishlist.setUser(user);

        when(userRepository.findByUsername("omar")).thenReturn(Optional.of(user));
        when(wishlistRepository.findByUserId(1L)).thenReturn(Optional.of(wishlist));
        when(wishlistRepository.findByUserIdOrderByIdAsc(1L)).thenReturn(List.of(wishlist));
        when(wishlistItemRepository.findByWishlistIdOrderByAddedAtDesc(9L)).thenReturn(List.of());

        mockMvc.perform(get("/api/wishlist")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                new com.omar.ecommerce.security.CustomUserDetails(user),
                                null,
                                new com.omar.ecommerce.security.CustomUserDetails(user).getAuthorities()
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Wishlist retrieved successfully"))
                .andExpect(jsonPath("$.data.name").value("Wishlist"))
                .andExpect(jsonPath("$.data.defaultList").value(true))
                .andExpect(jsonPath("$.data.username").value("omar"));
    }
}
