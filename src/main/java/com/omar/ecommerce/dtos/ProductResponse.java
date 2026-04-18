package com.omar.ecommerce.dtos;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductResponse {

    private Long id;
    private String name;
    private BigDecimal price;

    private Long categoryId;
    private String categoryName;

    private int stock;
    private String imageUrl;

    private boolean isFavorited;
}