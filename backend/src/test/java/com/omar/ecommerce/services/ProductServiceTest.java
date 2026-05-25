package com.omar.ecommerce.services;

import com.omar.ecommerce.mapper.ProductMapper;
import com.omar.ecommerce.repositories.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Test
    void findById_throws404WhenProductMissing() {
        ProductService productService = new ProductService(productRepository, null, null);
        when(productRepository.findWithCategoryById(99L)).thenReturn(java.util.Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> productService.getResponseById(99L));

        assertTrue(ex.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND));
    }
}
