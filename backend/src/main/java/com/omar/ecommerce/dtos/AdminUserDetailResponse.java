package com.omar.ecommerce.dtos;

import com.omar.ecommerce.entities.Role;
import com.omar.ecommerce.entities.UserStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AdminUserDetailResponse {
    private Long id;
    private String username;
    private String email;
    private Role role;
    private UserStatus status;
    private LocalDateTime createdAt;
    private long orderCount;
    private BigDecimal totalSpent;
    private List<OrderResponse> orders;
}
