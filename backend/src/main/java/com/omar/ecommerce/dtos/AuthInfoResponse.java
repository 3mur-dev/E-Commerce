package com.omar.ecommerce.dtos;

public record AuthInfoResponse(
        String login,
        String register,
        String me
) {
}
