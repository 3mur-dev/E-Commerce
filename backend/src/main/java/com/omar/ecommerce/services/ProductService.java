package com.omar.ecommerce.services;

import com.omar.ecommerce.dtos.ProductResponse;
import com.omar.ecommerce.entities.Product;
import com.omar.ecommerce.entities.User;
import com.omar.ecommerce.mapper.ProductMapper;
import com.omar.ecommerce.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    private final ProductMapper productMapper;
    private final WishlistService wishlistService;

    public ProductResponse mapToResponse(Product product) {
        return productMapper.toResponse(product);
    }

    @Cacheable(value = "product_list")
    public List<ProductResponse> findAll() {
        return productRepository.findAllActiveProducts()
                .stream()
                .map(this::mapToResponse)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    public ProductResponse toggleFavorite(User user, Long productId) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        if (productId == null || productId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product id is required");
        }

        ProductResponse response = getResponseById(productId).copy();
        boolean favorited = wishlistService.toggleWishlistItem(user, productId);
        response.setFavorited(favorited);
        return response;
    }

    @Cacheable(value = "products", key = "#id")
    public ProductResponse getResponseById(long id) {
        Product product = productRepository.findWithCategoryById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Product with ID " + id + " not found"));

        return mapToResponse(product);
    }
}
