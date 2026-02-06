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
    private int stock;
    private boolean favorited = false;
    private String imageUrl;
}