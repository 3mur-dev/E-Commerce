package com.omar.ecommerce.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.omar.ecommerce.dtos.AdminDashboardResponse;
import com.omar.ecommerce.dtos.ProductRequest;
import com.omar.ecommerce.dtos.ProductResponse;
import com.omar.ecommerce.entities.*;
import com.omar.ecommerce.events.events.ProductAddEvent;
import com.omar.ecommerce.events.events.ProductDeleteEvent;
import com.omar.ecommerce.events.events.ProductUpdateEvent;
import com.omar.ecommerce.exception.CategoryNotFoundException;
import com.omar.ecommerce.exception.ConflictException;
import com.omar.ecommerce.exception.ProductNotFoundException;
import com.omar.ecommerce.mapper.ProductMapper;
import com.omar.ecommerce.repositories.*;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;
    private final WishlistItemRepository wishlistItemRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    private final HttpServletRequest httpServletRequest;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @Value("${CLOUDINARY_URL:}")
    private String cloudinaryUrl;

    private Cloudinary cloudinary;

    @PostConstruct
    void normalizeCloudinaryConfig() {
        cloudinaryUrl = normalizeCloudinaryUrl(cloudinaryUrl);
    }
    public BigDecimal getTodayRevenue() {
        LocalDate today = LocalDate.now();
        LocalDateTime from = today.atStartOfDay();
        LocalDateTime to = today.plusDays(1).atStartOfDay();

        return orderRepository.getRevenueBetween(PaymentStatus.PAID, from, to);
    }

    public BigDecimal getMonthlyRevenue() {
        LocalDateTime from = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime to = from.plusMonths(1);

        return orderRepository.getRevenueBetween(PaymentStatus.PAID, from, to);
    }

    public BigDecimal getRevenue() {
        return orderRepository.getTotalRevenue(PaymentStatus.PAID);
    }
    public Long getTotalOrders() {
        return orderRepository.count();
    }
    public Long getTotalUsers() {
        return userRepository.count();
    }
    public Long getTotalProducts() {
        return productRepository.count();
    }

    public AdminDashboardResponse getDashboard() {
        return new AdminDashboardResponse(
                getTodayRevenue(),
                getMonthlyRevenue(),
                getRevenue(),
                getTotalOrders(),
                getTotalUsers(),
                getTotalProducts()
        );
    }

    public ProductResponse addProduct(ProductRequest request) {
        return create(request, null);
    }

    @CacheEvict(value = "product_list", allEntries = true)
    @Transactional
    public ProductResponse create(ProductRequest request, MultipartFile image) {
        validateProductRequest(request);

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String performedBy = authentication.getName();

        Product product = new Product();
        product.setName(request.getName());
        product.setShortDescription(request.getShortDescription());
        product.setLongDescription(request.getLongDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());

        if (image != null && !image.isEmpty()) {
            try {
                if (!isValidImage(image)) {
                    throw new RuntimeException("Invalid image: Only JPEG/PNG/WebP allowed, max 5MB");
                }

                if (isCloudinaryEnabled()) {
                    String imageUrl = uploadToCloudinary(image);
                    product.setImageUrl(imageUrl);
                } else {
                    Path uploadPath = resolveUploadPath();
                    Files.createDirectories(uploadPath);

                    String fileName = UUID.randomUUID() + safeFileExtension(image);
                    Path filePath = uploadPath.resolve(fileName).normalize();
                    Files.copy(image.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

                    product.setImageUrl("/images/products/" + fileName);
                }
            } catch (IOException e) {
                throw new RuntimeException("Image upload failed", e);
            }
        } else if (request.getImageUrl() != null && !request.getImageUrl().isBlank()) {
            product.setImageUrl(request.getImageUrl().trim());
        }

        if (productRepository.existsByName(product.getName())) {
            throw new ConflictException("Product with this name already exists");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new CategoryNotFoundException("Category not found: " + request.getCategoryId()));

        product.setCategory(category);
        Product savedProduct = productRepository.save(product);

        String ipAddress = getClientIp(httpServletRequest);
        String userAgent = httpServletRequest.getHeader("User-Agent");

        applicationEventPublisher.publishEvent(
                new ProductAddEvent(
                        savedProduct.getId(),
                        savedProduct.getStock(),
                        performedBy,
                        ipAddress,
                        userAgent)
        );

        return mapToResponse(savedProduct);
    }

    public ProductResponse mapToResponse(Product product) {
        return productMapper.toResponse(product);
    }

    private void validateProductRequest(ProductRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Product name cannot be empty");
        }

        if (request.getPrice() == null || request.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Product price cannot be negative or null");
        }
    }

    @CacheEvict(value = "product_list", allEntries = true)
    @Transactional
    public ProductResponse updateProduct(Long productId, ProductRequest request) {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String performedBy = authentication.getName();

        Product existing = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found: " + productId));

        Category requestCategory = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new CategoryNotFoundException("Category not found: " + request.getCategoryId()));

        existing.setName(request.getName());
        existing.setShortDescription(request.getShortDescription());
        existing.setLongDescription(request.getLongDescription());
        existing.setPrice(request.getPrice());
        existing.setStock(request.getStock());
        existing.setImageUrl(request.getImageUrl());
        existing.setCategory(requestCategory);

        Product updatedProduct = productRepository.save(existing);

        String ipAddress = getClientIp(httpServletRequest);
        String userAgent = httpServletRequest.getHeader("User-Agent");

        applicationEventPublisher.publishEvent(
                new ProductUpdateEvent(updatedProduct.getId(),
                        updatedProduct.getStock(),
                        performedBy,
                        ipAddress,
                        userAgent)
        );

        Product responseProduct = productRepository.findWithCategoryById(updatedProduct.getId())
                .orElse(updatedProduct);

        return mapToResponse(responseProduct);
    }


    @Transactional
    @CacheEvict(value = "product_list", allEntries = true)
    public void deleteProduct(long id) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String performedBy = authentication.getName();

        wishlistItemRepository.deleteByProductId(id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product with ID " + id + " not found"));
        product.setDeleted(true);
        productRepository.save(product);

        String ipAddress = getClientIp(httpServletRequest);
        String userAgent = httpServletRequest.getHeader("User-Agent");

        applicationEventPublisher.publishEvent(
                new ProductDeleteEvent(
                        product.getId(),
                        product.getStock(),
                        performedBy,
                        ipAddress,
                        userAgent)
        );
    }

    private Path resolveUploadPath() {
        String configured = (uploadDir == null || uploadDir.isBlank())
                ? "./uploads"
                : uploadDir.trim();

        Path uploadPath = Paths.get(configured).toAbsolutePath().normalize();
        return uploadPath.resolve("products").normalize();
    }

    private boolean isValidImage(MultipartFile file) {
        String contentType = file.getContentType();
        long maxSize = 5 * 1024 * 1024L;

        return (contentType != null) &&
                (contentType.startsWith("image/") &&
                        (contentType.equals("image/jpeg") || contentType.equals("image/png") || contentType.equals("image/webp"))) &&
                file.getSize() <= maxSize;
    }

    private String safeFileExtension(MultipartFile file) {
        String contentType = file.getContentType();
        if ("image/png".equals(contentType)) {
            return ".png";
        }
        if ("image/webp".equals(contentType)) {
            return ".webp";
        }
        return ".jpg";
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
    private String getClientIp(HttpServletRequest httpServletRequest) {
        if (httpServletRequest == null) {
            return "unknown";
        }

        String xfHeader = httpServletRequest.getHeader("X-Forwarded-For");

        if (xfHeader == null || xfHeader.isBlank()) {
            return httpServletRequest.getRemoteAddr();
        }

        return xfHeader.split(",")[0];
    }
}
