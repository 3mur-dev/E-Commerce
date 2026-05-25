package com.omar.ecommerce.dtos;

import com.omar.ecommerce.entities.Role;
import com.omar.ecommerce.entities.UserStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AdminUserListResponse {
    private Long id;
    private String username;
    private String email;
    private Role role;
    private UserStatus status;
    private LocalDateTime createdAt;
    private long orderCount;
    private BigDecimal totalSpent;
}
