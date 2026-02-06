package com.omar.ecommerce.services;

import com.omar.ecommerce.dtos.ProductRequest;
import com.omar.ecommerce.dtos.ProductResponse;
import com.omar.ecommerce.entities.Category;
import com.omar.ecommerce.entities.Product;
import com.omar.ecommerce.repositories.CategoryRepository;
import com.omar.ecommerce.repositories.ProductRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@AllArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductResponse mapToResponse(Product product) {
        ProductResponse resp = new ProductResponse();
        resp.setId(product.getId());
        resp.setName(product.getName());
        resp.setPrice(product.getPrice());
        resp.setCategoryId(product.getCategory().getId());
        resp.setStock(product.getStock());
        return resp;
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
        product.setStock(request.getStock());


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
        product.setStock(request.getStock());

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

        if (request.getPrice() == null || request.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Product price cannot be negative or null");
        }
    }



    public List<ProductResponse> searchByName(String keyword) {
        return productRepository.findByNameContainingIgnoreCase(keyword)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /*
    Stock management
    */
    public void increaseStock(Long productId, int amount) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        product.setStock(product.getStock() + amount);
        productRepository.save(product);
    }

    public void decreaseStock(Long productId, int amount) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        int newQty = product.getStock() - amount;
        product.setStock(Math.max(newQty, 0)); // avoid negative stock
        productRepository.save(product);
    }

    public void setStock(Long productId, int newQuantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        product.setStock(newQuantity);
        productRepository.save(product);
    }
}