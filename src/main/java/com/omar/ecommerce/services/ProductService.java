package com.omar.ecommerce.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.omar.ecommerce.dtos.ProductRequest;
import com.omar.ecommerce.dtos.ProductResponse;
import com.omar.ecommerce.dtos.ProductSearchRequest;
import com.omar.ecommerce.entities.Category;
import com.omar.ecommerce.entities.Product;
import com.omar.ecommerce.entities.User;
import com.omar.ecommerce.mapper.ProductMapper;
import com.omar.ecommerce.repositories.CategoryRepository;
import com.omar.ecommerce.repositories.FavoriteRepository;
import com.omar.ecommerce.repositories.ProductRepository;
import com.omar.ecommerce.repositories.WishlistItemRepository;
import com.omar.ecommerce.util.ProductSpecification;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final FavoriteRepository favoriteRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final ProductMapper productMapper;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @Value("${CLOUDINARY_URL:}")
    private String cloudinaryUrl;

    private Cloudinary cloudinary;

    @PostConstruct
    void normalizeCloudinaryConfig() {
        cloudinaryUrl = normalizeCloudinaryUrl(cloudinaryUrl);
    }

    public Page<ProductResponse> findAll(String sort, int page) {

        Sort.Direction direction = sort.isEmpty() ? Sort.Direction.ASC :
                (sort.endsWith(",desc") ? Sort.Direction.DESC : Sort.Direction.ASC);
        String sortField = sort.isEmpty() ? "name" : sort.split(",")[0];

        Pageable pageable = PageRequest.of(page, 12, Sort.by(direction, sortField));
        Page<Product> productPage = productRepository.findAllActive(pageable);

        return productPage.map(this::mapToResponse);
    }

    public Page<ProductResponse> searchByName(String keyword, String sort, int page) {
        Sort.Direction direction = sort.isEmpty() ? Sort.Direction.ASC :
                (sort.endsWith(",desc") ? Sort.Direction.DESC : Sort.Direction.ASC);
        String sortField = sort.isEmpty() ? "name" : sort.split(",")[0];

        Pageable pageable = PageRequest.of(page, 12, Sort.by(direction, sortField));
        Page<Product> productPage = productRepository
                .findByNameContainingIgnoreCase(keyword, pageable);

        return productPage.map(this::mapToResponse);
    }

    public ProductResponse mapToResponse(Product product) {
        ProductResponse resp = new ProductResponse();
        resp.setId(product.getId());
        resp.setName(product.getName());
        resp.setPrice(product.getPrice());
        if (product.getCategory() != null) {
            resp.setCategoryId(product.getCategory().getId());
            resp.setCategoryName(product.getCategory().getName());
        }
        resp.setStock(product.getStock());
        resp.setImageUrl(product.getImageUrl());
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

                if (isCloudinaryEnabled()) {
                    String imageUrl = uploadToCloudinary(image);
                    product.setImageUrl(imageUrl);
                } else {
                    Path uploadPath = Paths.get(uploadDir, "products");
                    Files.createDirectories(uploadPath);

                    String fileName = UUID.randomUUID() + getFileExtension(image);
                    Path filePath = uploadPath.resolve(fileName).normalize();
                    Files.copy(image.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

                    product.setImageUrl("/images/products/" + fileName);
                }
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

    private boolean isCloudinaryEnabled() {
        cloudinaryUrl = normalizeCloudinaryUrl(cloudinaryUrl);
        return cloudinaryUrl != null && cloudinaryUrl.startsWith("cloudinary://");
    }

    private Cloudinary getCloudinary() {
        if (cloudinary == null) {
            cloudinary = new Cloudinary(cloudinaryUrl);
            cloudinary.config.secure = true;
        }
        return cloudinary;
    }

    private String normalizeCloudinaryUrl(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\""))
                || (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
        }
        int schemeIndex = trimmed.indexOf("cloudinary://");
        if (schemeIndex >= 0) {
            return trimmed.substring(schemeIndex).trim();
        }
        String prefix = "CLOUDINARY_URL=";
        if (trimmed.startsWith(prefix)) {
            return trimmed.substring(prefix.length()).trim();
        }
        return trimmed;
    }

    private String uploadToCloudinary(MultipartFile file) throws IOException {
        Cloudinary client = getCloudinary();
        var uploadResult = client.uploader().upload(file.getBytes(), ObjectUtils.asMap("folder", "products"));
        Object secureUrl = uploadResult.get("secure_url");
        if (secureUrl == null) {
            throw new RuntimeException("Cloudinary upload failed: missing secure_url");
        }
        return secureUrl.toString();
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
        wishlistItemRepository.deleteByProductId(id);

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
        if (amount <= 0) {
            throw new IllegalArgumentException("Increase amount must be positive");
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        product.setStock(product.getStock() + amount);
        productRepository.save(product);
    }

    public void decreaseStock(Long productId, int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Decrease amount must be positive");
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        int newQty = product.getStock() - amount;
        product.setStock(Math.max(newQty, 0)); // avoid negative stock
        productRepository.save(product);
    }

    public void setStock(Long productId, int newQuantity) {
        if (newQuantity < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative");
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        product.setStock(newQuantity);
        productRepository.save(product);
    }

    public Page<ProductResponse> search(ProductSearchRequest req) {
        sanitizeSearchRequest(req);

        // 1. Build dynamic filters
        Specification<Product> spec = ProductSpecification.build(req);

        // 2. Build sorting
        Sort sort = buildSort(req);

        // 3. Build pagination
        Pageable pageable = PageRequest.of(req.getPage(), req.getSize(), sort);

        // 4. Execute query
        Page<Product> productPage = productRepository.findAll(spec, pageable);

        // 5. Map to response DTO
        return productPage.map(productMapper::toResponse);
    }

    private Sort buildSort(ProductSearchRequest req) {
        String sortBy = switch ((req.getSortBy() == null ? "" : req.getSortBy().trim().toLowerCase())) {
            case "name" -> "name";
            case "price" -> "price";
            case "stock" -> "stock";
            default -> "id";
        };

        Sort.Direction direction = "asc".equalsIgnoreCase(req.getSortDir())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return Sort.by(direction, sortBy);
    }

    private void sanitizeSearchRequest(ProductSearchRequest req) {
        if (req.getPage() < 0) {
            req.setPage(0);
        }

        if (req.getSize() <= 0 || req.getSize() > 48) {
            req.setSize(12);
        }

        if (req.getKeyword() != null) {
            req.setKeyword(req.getKeyword().trim());
        }

        if (req.getMinPrice() != null && req.getMinPrice().compareTo(BigDecimal.ZERO) < 0) {
            req.setMinPrice(BigDecimal.ZERO);
        }

        if (req.getMaxPrice() != null && req.getMaxPrice().compareTo(BigDecimal.ZERO) < 0) {
            req.setMaxPrice(BigDecimal.ZERO);
        }

        if (req.getMinPrice() != null && req.getMaxPrice() != null
                && req.getMinPrice().compareTo(req.getMaxPrice()) > 0) {
            BigDecimal originalMin = req.getMinPrice();
            req.setMinPrice(req.getMaxPrice());
            req.setMaxPrice(originalMin);
        }
    }

    public ProductResponse findById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found"));

        return mapToResponse(product);
    }
}
