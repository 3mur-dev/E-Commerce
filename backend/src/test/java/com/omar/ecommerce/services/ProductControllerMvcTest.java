package com.omar.ecommerce.services;

import com.omar.ecommerce.controller.ProductController;
import com.omar.ecommerce.dtos.ProductResponse;
import com.omar.ecommerce.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductControllerMvcTest {

    @Test
    void getProduct_returnsProductDetails() throws Exception {
        ProductResponse response = new ProductResponse();
        response.setId(7L);
        response.setName("Keyboard");
        response.setPrice(new BigDecimal("24.99"));
        response.setStock(10);

        ProductService productService = new ProductService(null, null, null) {
            @Override
            public ProductResponse getResponseById(long id) {
                return response;
            }
        };

        ProductController controller = new ProductController(productService, mock(UserRepository.class));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(get("/api/products/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Product retrieved successfully"))
                .andExpect(jsonPath("$.data.id").value(7))
                .andExpect(jsonPath("$.data.name").value("Keyboard"))
                .andExpect(jsonPath("$.data.price").value(24.99))
                .andExpect(jsonPath("$.data.stock").value(10));
    }

    @Test
    void getProduct_returns404WhenMissing() throws Exception {
        ProductService productService = new ProductService(null, null, null) {
            @Override
            public ProductResponse getResponseById(long id) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product with ID " + id + " not found");
            }
        };

        ProductController controller = new ProductController(productService, mock(UserRepository.class));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(get("/api/products/999"))
                .andExpect(status().isNotFound());
    }
}
