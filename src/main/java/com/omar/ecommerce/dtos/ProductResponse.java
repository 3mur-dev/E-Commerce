package com.omar.ecommerce.dtos;

import lombok.*;

import java.math.BigDecimal;

@Data
public class ProductResponse {

    private Long id;
    private String name;
    private BigDecimal price;
    private Long categoryId;
    private int stock;
}

