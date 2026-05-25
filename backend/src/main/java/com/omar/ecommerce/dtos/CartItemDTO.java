package com.omar.ecommerce.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CartItemDTO {
    private Long id;
    @NotNull
    private Long productId;
    private String productName;
    private String imageUrl;
    private BigDecimal price;
    @Min(1)
    private int quantity;
    private int stock;
    private BigDecimal subtotal;
}
