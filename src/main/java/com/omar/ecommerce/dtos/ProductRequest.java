package com.omar.ecommerce.dtos;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductRequest {

    @NotBlank
    private String name;

    private BigDecimal price;
    private Long categoryId;
    @Min(value = 0, message = "Stock cannot be negative")
    private int stock;
}
