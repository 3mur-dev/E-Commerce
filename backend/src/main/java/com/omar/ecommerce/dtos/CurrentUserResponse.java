package com.omar.ecommerce.dtos;

import com.omar.ecommerce.entities.Role;

public record CurrentUserResponse(
        Long id,
        String username,
        String email,
        Role role
) {
}
