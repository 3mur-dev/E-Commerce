package com.omar.ecommerce.controller;

import com.omar.ecommerce.dtos.AdminDashboardResponse;
import com.omar.ecommerce.dtos.AdminUserDetailResponse;
import com.omar.ecommerce.dtos.AdminUserListResponse;
import com.omar.ecommerce.dtos.OrderItemResponse;
import com.omar.ecommerce.dtos.OrderResponse;
import com.omar.ecommerce.dtos.OrderUserResponse;
import com.omar.ecommerce.dtos.ProductRequest;
import com.omar.ecommerce.dtos.ProductResponse;
import com.omar.ecommerce.entities.OrderStatus;
import com.omar.ecommerce.entities.Role;
import com.omar.ecommerce.entities.UserStatus;
import com.omar.ecommerce.services.AdminOrderService;
import com.omar.ecommerce.services.AdminService;
import com.omar.ecommerce.services.AdminUserService;
import com.omar.ecommerce.services.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminControllerTest {

    private MockMvc mockMvc;
    private StubAdminOrderService adminOrderService;
    private StubAdminUserService adminUserService;

    @BeforeEach
    void setUp() {
        adminOrderService = new StubAdminOrderService();
        adminUserService = new StubAdminUserService();

        AdminService adminService = new AdminService(null, null, null, null, null, null, null, null) {
            @Override
            public AdminDashboardResponse getDashboard() {
                return new AdminDashboardResponse(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0L, 0L, 0L);
            }

            @Override
            public ProductResponse addProduct(ProductRequest request) {
                return null;
            }
        };

        ProductService productService = new ProductService(null, null, null) {
            @Override
            public List<ProductResponse> findAll() {
                return List.of();
            }
        };

        AdminController controller = new AdminController(adminService, adminOrderService, productService, adminUserService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void getOrders_returnsAdminOrdersShape() throws Exception {
        OrderUserResponse user = new OrderUserResponse();
        user.setId(1L);
        user.setName("Omar");
        user.setEmail("omar@example.com");

        OrderItemResponse item = new OrderItemResponse();
        item.setId(7L);
        item.setProductId(99L);
        item.setProductName("Mouse");
        item.setName("Mouse");
        item.setQuantity(2);
        item.setPrice(new BigDecimal("25.00"));
        item.setSubtotal(new BigDecimal("50.00"));

        OrderResponse order = new OrderResponse();
        order.setId(55L);
        order.setOrderNumber("ORD-123");
        order.setStatus(OrderStatus.PENDING);
        order.setCreationTimestamp(LocalDateTime.of(2026, 5, 11, 10, 30));
        order.setTotal(new BigDecimal("50.00"));
        order.setUser(user);
        order.setItems(List.of(item));

        adminOrderService.orders = List.of(order);

        mockMvc.perform(get("/api/admin/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].orderNumber").value("ORD-123"))
                .andExpect(jsonPath("$.data[0].user.name").value("Omar"))
                .andExpect(jsonPath("$.data[0].items[0].name").value("Mouse"));
    }

    @Test
    void updateOrderStatus_usesStatusEndpoint() throws Exception {
        OrderResponse order = new OrderResponse();
        order.setId(55L);
        order.setOrderNumber("ORD-123");
        order.setStatus(OrderStatus.SHIPPED);

        adminOrderService.updatedOrder = order;

        mockMvc.perform(patch("/api/admin/orders/55/status")
                        .contentType("application/json")
                        .content("""
                                {"status":"SHIPPED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SHIPPED"));
    }

    @Test
    void getUsers_returnsPagedUserList() throws Exception {
        AdminUserListResponse user = new AdminUserListResponse();
        user.setId(1L);
        user.setUsername("omar");
        user.setEmail("omar@example.com");
        user.setRole(Role.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setOrderCount(2L);
        user.setTotalSpent(new BigDecimal("80.00"));

        adminUserService.users = new PageImpl<>(List.of(user), PageRequest.of(0, 20), 1);

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].username").value("omar"))
                .andExpect(jsonPath("$.data.content[0].status").value("ACTIVE"));
    }

    @Test
    void getUser_returnsDetailPayload() throws Exception {
        AdminUserDetailResponse detail = new AdminUserDetailResponse();
        detail.setId(1L);
        detail.setUsername("omar");
        detail.setEmail("omar@example.com");
        detail.setRole(Role.USER);
        detail.setStatus(UserStatus.ACTIVE);

        adminUserService.detail = detail;

        mockMvc.perform(get("/api/admin/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("omar@example.com"));
    }

    private static class StubAdminOrderService extends AdminOrderService {
        List<OrderResponse> orders = List.of();
        OrderResponse updatedOrder;

        StubAdminOrderService() {
            super(null, null, null, null);
        }

        @Override
        public List<OrderResponse> findAllOrders() {
            return orders;
        }

        @Override
        public OrderResponse updateStatus(Long orderId, OrderStatus status, String ip, String userAgent) {
            return updatedOrder;
        }
    }

    private static class StubAdminUserService extends AdminUserService {
        Page<AdminUserListResponse> users = Page.empty();
        AdminUserDetailResponse detail;

        StubAdminUserService() {
            super(null, null);
        }

        @Override
        public Page<AdminUserListResponse> findUsers(String search, Role role, UserStatus status, org.springframework.data.domain.Pageable pageable) {
            return users;
        }

        @Override
        public AdminUserDetailResponse getUser(Long userId) {
            return detail;
        }

        @Override
        public AdminUserDetailResponse updateStatus(Long userId, UserStatus status, org.springframework.security.core.Authentication authentication) {
            return detail;
        }

        @Override
        public AdminUserDetailResponse enableUser(Long userId, org.springframework.security.core.Authentication authentication) {
            return detail;
        }

        @Override
        public AdminUserDetailResponse disableUser(Long userId, org.springframework.security.core.Authentication authentication) {
            return detail;
        }

        @Override
        public AdminUserDetailResponse updateRole(Long userId, Role role, org.springframework.security.core.Authentication authentication) {
            return detail;
        }
    }
}
