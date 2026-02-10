package com.omar.ecommerce.services;

import com.omar.ecommerce.dtos.ProductRequest;
import com.omar.ecommerce.dtos.ProductResponse;
import com.omar.ecommerce.entities.Category;
import com.omar.ecommerce.entities.Product;
import com.omar.ecommerce.repositories.CategoryRepository;
import com.omar.ecommerce.repositories.FavoriteRepository;
import com.omar.ecommerce.repositories.ProductRepository;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final FavoriteRepository  favoriteRepository;

    public List<ProductResponse> findAll(String sort, int page) {

        Sort.Direction direction = sort.isEmpty() ? Sort.Direction.ASC :
                (sort.endsWith(",desc") ? Sort.Direction.DESC : Sort.Direction.ASC);
        String sortField = sort.isEmpty() ? "name" : sort.split(",")[0];

        Pageable pageable = PageRequest.of(page, 12, Sort.by(direction, sortField));
        Page<Product> productPage = productRepository.findAllActive(pageable);

        return productPage.getContent().stream()
                .map(this::mapToResponse)  // Your existing mapper method
                .collect(Collectors.toList());
    }
    public List<ProductResponse> searchByName(String keyword, String sort, int page) {
        Sort.Direction direction = sort.isEmpty() ? Sort.Direction.ASC :
                (sort.endsWith(",desc") ? Sort.Direction.DESC : Sort.Direction.ASC);
        String sortField = sort.isEmpty() ? "name" : sort.split(",")[0];

        Pageable pageable = PageRequest.of(page, 12, Sort.by(direction, sortField));
        Page<Product> productPage = productRepository
                .findByNameContainingIgnoreCase(keyword, pageable);

        return productPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ProductResponse mapToResponse(Product product) {
        ProductResponse resp = new ProductResponse();
        resp.setId(product.getId());
        resp.setName(product.getName());
        resp.setPrice(product.getPrice());
        resp.setCategoryId(product.getCategory().getId());
        resp.setStock(product.getStock());
        resp.setImageUrl(product.getImageUrl());
        resp.setDeleted(Boolean.TRUE.equals(product.getDeleted()));
        return resp;
    }

    public List<ProductResponse> findAll() {
        return productRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();

    }

    public ProductResponse create(ProductRequest request, MultipartFile image) {
        validateProductRequest(request);


        Product product = new Product();
        product.setName(request.getName());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());

        // FIXED: Use absolute runtime path
        if (image != null && !image.isEmpty()) {
            try {
                if (!isValidImage(image)) {
                    throw new RuntimeException("Invalid image: Only JPEG/PNG/WebP allowed, max 5MB");
                }

                Path uploadDir = Paths.get("uploads", "products");
                Files.createDirectories(uploadDir);

                String fileName = UUID.randomUUID() + getFileExtension(image);
                Path filePath = uploadDir.resolve(fileName).normalize();
                Files.copy(image.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

                product.setImageUrl("/images/products/" + fileName);
            } catch (IOException e) {
                throw new RuntimeException("Image upload failed", e);
            }
        }

        if (productRepository.existsByName(product.getName())) {
            throw new RuntimeException("Product with this name already exists");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new NoSuchElementException("Category not found: " + request.getCategoryId()));

        product.setCategory(category);
        Product savedProduct = productRepository.save(product);

        return mapToResponse(savedProduct);
    }

    // Add these helper methods
    private String getUploadDirectory() {
        return Paths.get(System.getProperty("user.dir"), "uploads", "products").toString();
    }

    private boolean isValidImage(MultipartFile file) {
        String contentType = file.getContentType();
        long maxSize = 5 * 1024 * 1024L; // 5MB

        return (contentType != null) &&
                (contentType.startsWith("image/") &&
                        (contentType.equals("image/jpeg") || contentType.equals("image/png") || contentType.equals("image/webp"))) &&
                file.getSize() <= maxSize;
    }

    private String getFileExtension(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        if (originalName == null) {
            return ".jpg";
        }
        String sanitized = Paths.get(originalName).getFileName().toString();
        return sanitized.contains(".") ? sanitized.substring(sanitized.lastIndexOf(".")) : ".jpg";
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

    @Transactional
    public void delete(long id) {

        // DELETE FAVORITES FIRST
        favoriteRepository.deleteByProductId(id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Product with ID " + id + " not found"));
        product.setDeleted(true);
        productRepository.save(product);
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
