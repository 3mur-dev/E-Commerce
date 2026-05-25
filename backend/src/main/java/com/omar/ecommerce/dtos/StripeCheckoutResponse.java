package com.omar.ecommerce.dtos;

public record StripeCheckoutResponse(
        String url,
        Long orderId
) {}
