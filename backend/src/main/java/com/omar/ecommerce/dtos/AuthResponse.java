package com.omar.ecommerce.dtos;

public record AuthResponse(
        String token,
        String tokenType,
        String username
) {
}
