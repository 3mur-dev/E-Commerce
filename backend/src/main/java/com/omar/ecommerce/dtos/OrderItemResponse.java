package com.omar.ecommerce.dtos;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String name;
    private String imageUrl;
    private int quantity;
    private BigDecimal price;
    private BigDecimal subtotal;
}
