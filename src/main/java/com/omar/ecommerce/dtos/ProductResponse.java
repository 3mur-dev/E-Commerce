package com.omar.ecommerce.dtos;

import lombok.*;

@Data
@AllArgsConstructor
public class ProductResponse {

    private Long id;
    private String name;
    private double price;
    private long categoryId;
}

