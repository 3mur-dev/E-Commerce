package com.omar.ecommerce.dtos;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductRequest {

    @NotBlank
    @NotNull
    private String name;

    @Min(value = 0, message = "Stock cannot be negative")
    @NotNull
    private int stock;

    @Size(max = 500, message = "Short description cannot exceed 500 characters")
    private String shortDescription;

    private String longDescription;

    @NotNull
    private BigDecimal price;

    @NotNull
    private Long categoryId;

    private String imageUrl;
}
