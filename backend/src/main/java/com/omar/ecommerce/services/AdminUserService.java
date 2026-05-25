package com.omar.ecommerce.services;

import com.omar.ecommerce.dtos.AdminUserDetailResponse;
import com.omar.ecommerce.dtos.AdminUserListResponse;
import com.omar.ecommerce.dtos.OrderItemResponse;
import com.omar.ecommerce.dtos.OrderResponse;
import com.omar.ecommerce.dtos.OrderUserResponse;
import com.omar.ecommerce.entities.Order;
import com.omar.ecommerce.entities.OrderItem;
import com.omar.ecommerce.entities.OrderStatus;
import com.omar.ecommerce.entities.Role;
import com.omar.ecommerce.entities.User;
import com.omar.ecommerce.entities.UserStatus;
import com.omar.ecommerce.repositories.OrderRepository;
import com.omar.ecommerce.repositories.UserRepository;
import com.omar.ecommerce.util.UserSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public Page<AdminUserListResponse> findUsers(String search, Role role, UserStatus status, Pageable pageable) {
        Page<User> users = userRepository.findAll(UserSpecifications.matches(search, role, status), pageable);
        List<Long> userIds = users.stream().map(User::getId).toList();

        if (userIds.isEmpty()) {
            return users.map(this::toListResponse);
        }

        var statsByUserId = orderRepository.findUserOrderStats(userIds).stream()
                .collect(java.util.stream.Collectors.toMap(
                        OrderRepository.UserOrderStats::getUserId,
                        stat -> stat
                ));

        return users.map(user -> toListResponse(user, statsByUserId.get(user.getId())));
    }

    @Transactional
    public AdminUserDetailResponse getUser(Long userId) {
        User user = getUserOrThrow(userId);
        List<Order> orders = orderRepository.findByUserIdWithItems(userId).stream()
                .sorted(Comparator.comparing(Order::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .reversed()
                        .thenComparing(Order::getId, Comparator.reverseOrder()))
                .toList();
        return toDetailResponse(user, orders);
    }

    @Transactional
    public AdminUserDetailResponse updateStatus(Long userId, UserStatus status, Authentication authentication) {
        if (status == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status is required");
        }

        User user = getUserOrThrow(userId);
        if (authentication != null && authentication.getName() != null
                && authentication.getName().equalsIgnoreCase(user.getUsername())
                && status == UserStatus.DISABLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot disable your own account");
        }
        user.setStatus(status);
        userRepository.save(user);
        return getUser(userId);
    }

    @Transactional
    public AdminUserDetailResponse enableUser(Long userId, Authentication authentication) {
        return updateStatus(userId, UserStatus.ACTIVE, authentication);
    }

    @Transactional
    public AdminUserDetailResponse disableUser(Long userId, Authentication authentication) {
        return updateStatus(userId, UserStatus.DISABLED, authentication);
    }

    @Transactional
    public AdminUserDetailResponse updateRole(Long userId, Role role, Authentication authentication) {
        if (role == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role is required");
        }

        User user = getUserOrThrow(userId);
        if (authentication != null && authentication.getName() != null
                && authentication.getName().equalsIgnoreCase(user.getUsername())
                && role != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot demote your own account");
        }

        user.setRole(role);
        userRepository.save(user);
        return getUser(userId);
    }

    private User getUserOrThrow(Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User id is required");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private AdminUserListResponse toListResponse(User user) {
        return toListResponse(user, null);
    }

    private AdminUserListResponse toListResponse(User user, OrderRepository.UserOrderStats stats) {
        AdminUserListResponse response = new AdminUserListResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        response.setStatus(resolveStatus(user));
        response.setCreatedAt(user.getCreatedAt());
        if (stats != null) {
            response.setOrderCount(stats.getOrderCount().intValue());
            response.setTotalSpent(stats.getTotalSpent());
        } else {
            response.setOrderCount(0);
            response.setTotalSpent(BigDecimal.ZERO);
        }
        return response;
    }

    private AdminUserDetailResponse toDetailResponse(User user, List<Order> orders) {
        AdminUserDetailResponse response = new AdminUserDetailResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        response.setStatus(resolveStatus(user));
        response.setCreatedAt(user.getCreatedAt());
        response.setOrderCount(orders.size());
        response.setTotalSpent(sumOrders(orders));
        response.setOrders(orders.stream().map(this::toOrderResponse).toList());
        return response;
    }

    private UserStatus resolveStatus(User user) {
        return user.getStatus() == null ? UserStatus.ACTIVE : user.getStatus();
    }

    private BigDecimal sumOrders(List<Order> orders) {
        return orders.stream()
                .map(order -> order.getTotal() == null ? BigDecimal.ZERO : order.getTotal())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private OrderResponse toOrderResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setOrderNumber(order.getOrderNumber());
        response.setStatus(order.getStatus());
        response.setPaymentMethod(order.getPaymentMethod());
        response.setPaymentStatus(order.getPaymentStatus());
        response.setCreationTimestamp(order.getCreatedAt());
        response.setTotal(order.getTotal());
        response.setUser(toUserResponse(order));
        response.setCustomerName(order.getCustomerName());
        response.setCustomerEmail(order.getCustomerEmail());
        response.setPhone(order.getPhone());
        response.setAddressLine1(order.getAddressLine1());
        response.setAddressLine2(order.getAddressLine2());
        response.setCity(order.getCity());
        response.setState(order.getState());
        response.setPostalCode(order.getPostalCode());
        response.setCountry(order.getCountry());
        response.setNote(order.getNote());
        response.setItems(order.getItems().stream().map(this::toItemResponse).toList());
        return response;
    }

    private OrderUserResponse toUserResponse(Order order) {
        if (order.getUser() == null) {
            return null;
        }

        OrderUserResponse response = new OrderUserResponse();
        response.setId(order.getUser().getId());
        response.setName(order.getUser().getUsername());
        response.setEmail(order.getUser().getEmail());
        return response;
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        OrderItemResponse response = new OrderItemResponse();
        response.setId(item.getId());
        response.setProductId(item.getProduct().getId());
        response.setProductName(item.getProduct().getName());
        response.setName(item.getProduct().getName());
        response.setImageUrl(item.getProduct().getImageUrl());
        response.setQuantity(item.getQuantity());
        response.setPrice(item.getPrice());
        response.setSubtotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        return response;
    }
}
