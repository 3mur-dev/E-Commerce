package com.omar.ecommerce.controller;

import com.omar.ecommerce.dtos.AdminDashboardResponse;
import com.omar.ecommerce.dtos.AdminUserDetailResponse;
import com.omar.ecommerce.dtos.AdminUserListResponse;
import com.omar.ecommerce.dtos.ApiResponse;
import com.omar.ecommerce.dtos.OrderResponse;
import com.omar.ecommerce.dtos.ProductRequest;
import com.omar.ecommerce.dtos.ProductResponse;
import com.omar.ecommerce.dtos.UpdateOrderStatusRequest;
import com.omar.ecommerce.dtos.UpdateUserRoleRequest;
import com.omar.ecommerce.dtos.UpdateUserStatusRequest;
import com.omar.ecommerce.entities.Role;
import com.omar.ecommerce.entities.UserStatus;
import com.omar.ecommerce.services.AdminOrderService;
import com.omar.ecommerce.services.AdminService;
import com.omar.ecommerce.services.AdminUserService;
import com.omar.ecommerce.services.ProductService;
import com.omar.ecommerce.util.ApiResponseUtil;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@AllArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final AdminOrderService adminOrderService;
    private final ProductService productService;
    private final AdminUserService adminUserService;

    @GetMapping("/dashboard")
    public ApiResponse<AdminDashboardResponse> getAdminDashboard() {
        return ApiResponseUtil.success("Dashboard retrieved successfully", adminService.getDashboard());
    }

    /*
    Orders Section
     */

    @GetMapping("/orders")
    public ApiResponse<List<OrderResponse>> getOrders() {
        return ApiResponseUtil.success("Orders retrieved successfully", adminOrderService.findAllOrders());
    }

    @PatchMapping("/orders/{id}/status")
    public ApiResponse<OrderResponse> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request,
            HttpServletRequest httpRequest) {

        String ip = httpRequest.getHeader("X-Forwarded-For");
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        if (ip == null || ip.isBlank()) {
            ip = httpRequest.getRemoteAddr();
        }

        String userAgent = httpRequest.getHeader("User-Agent");

        return ApiResponseUtil.success(
                "Order status updated successfully",
                adminOrderService.updateStatus(
                        id,
                        request.getStatus(),
                        ip,
                        userAgent
                )
        );
    }

    /*
    Product Section
     */
    @GetMapping("/products")
    public ApiResponse<List<ProductResponse>> getProducts() {
        return ApiResponseUtil.success("Products retrieved successfully", productService.findAll());
    }

    @PostMapping("/products/add")
    public ApiResponse<ProductResponse> addProduct(@Valid @RequestBody ProductRequest productRequest) {
        return ApiResponseUtil.success("Product created successfully", adminService.addProduct(productRequest));
    }

    @PutMapping("/products/{id}")
    public ApiResponse<ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest productRequest) {

        return ApiResponseUtil.success("Product updated successfully", adminService.updateProduct(id, productRequest));
    }

    @DeleteMapping("/products/{id}")
    public ApiResponse<Void> deleteProduct(@PathVariable Long id) {
         adminService.deleteProduct(id);
         return ApiResponse.<Void>builder()
                 .success(true)
                 .message("Product deleted successfully")
                 .build();
    }
    /*
    User Section
     */

    @GetMapping("/users")
    public ApiResponse<Page<AdminUserListResponse>> getAllUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) UserStatus status,
            Pageable pageable) {

        return ApiResponseUtil.success("Users retrieved successfully", adminUserService.findUsers(search, role, status, pageable));
    }

    @GetMapping("/users/{id}")
    public ApiResponse<AdminUserDetailResponse> getUser(@PathVariable Long id) {
        return ApiResponseUtil.success("User retrieved successfully", adminUserService.getUser(id));
    }

    @PatchMapping("/users/{id}/status")
    public ApiResponse<AdminUserDetailResponse> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserStatusRequest request,
            Authentication authentication) {

        return ApiResponseUtil.success("User status updated successfully", adminUserService.updateStatus(id, request.getStatus(), authentication));
    }

    @PostMapping("/users/{id}/disable")
    public ApiResponse<AdminUserDetailResponse> disableUser(
            @PathVariable Long id,
            Authentication authentication) {

        return ApiResponseUtil.success("User disabled successfully", adminUserService.disableUser(id, authentication));
    }

    @PostMapping("/users/{id}/enable")
    public ApiResponse<AdminUserDetailResponse> enableUser(
            @PathVariable Long id,
            Authentication authentication) {

        return ApiResponseUtil.success("User enabled successfully", adminUserService.enableUser(id, authentication));
    }

    @PatchMapping("/users/{id}/role")
    public ApiResponse<AdminUserDetailResponse> updateUserRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRoleRequest request,
            Authentication authentication) {

        return ApiResponseUtil.success("User role updated successfully", adminUserService.updateRole(id, request.getRole(), authentication));
    }
}
