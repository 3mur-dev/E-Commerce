package com.omar.ecommerce.services;

import com.omar.ecommerce.dtos.ProductRequest;
import com.omar.ecommerce.dtos.ProductResponse;
import com.omar.ecommerce.entities.Category;
import com.omar.ecommerce.entities.Product;
import com.omar.ecommerce.repositories.CategoryRepository;
import com.omar.ecommerce.repositories.ProductRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@AllArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    private ProductResponse mapToResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getCategory().getId()
        );
    }

    public List<ProductResponse> findAll() {
        return productRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();

    }

    public ProductResponse create(ProductRequest request) {
        validateProductRequest(request);

        Product product = new Product();
        product.setName(request.getName());
        product.setPrice(request.getPrice());

        if (productRepository.existsByName(product.getName())) {
            throw new RuntimeException("Product with this name already exists");
        }


        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new NoSuchElementException("Category with ID " + request.getCategoryId() + " not found"));

        product.setCategory(category);

        Product savedProduct = productRepository.save(product);

        // Map entity to response DTO
        return mapToResponse(savedProduct);
    }


    public ProductResponse update(long id, ProductRequest request) {
        validateProductRequest(request);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Product with ID " + id + " not found"));

        // Check for name uniqueness
        if (productRepository.existsByName(request.getName()) && !product.getName().equals(request.getName())) {
            throw new IllegalArgumentException("Product with this name already exists");
        }

        // Update fields
        product.setName(request.getName());
        product.setPrice(request.getPrice());

        // --- Update category ---
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new NoSuchElementException("Category with ID " + request.getCategoryId() + " not found"));
        product.setCategory(category);

        // Save updated product
        productRepository.save(product);

        return mapToResponse(product);
    }

    public void delete(long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Product with ID " + id + " not found"));
        productRepository.delete(product);
    }

    public ProductResponse getResponseById(long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Product with ID " + id + " not found"));

        return mapToResponse(product);
    }


    private void validateProductRequest(ProductRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Product name cannot be empty");
        }
        if (request.getPrice() < 0) {
            throw new IllegalArgumentException("Product price cannot be negative");
        }
    }


    public List<ProductResponse> searchByName(String keyword) {
        return productRepository.findByNameContainingIgnoreCase(keyword)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }
}