package com.omar.ecommerce.dtos;

import com.omar.ecommerce.entities.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateUserRoleRequest {
    @NotNull
    private Role role;
}
