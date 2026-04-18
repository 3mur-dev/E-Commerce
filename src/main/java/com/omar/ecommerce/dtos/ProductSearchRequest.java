package com.omar.ecommerce.dtos;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductSearchRequest {

    private String keyword;

    private Long categoryId;

    private BigDecimal minPrice;
    private BigDecimal maxPrice;

    private Boolean inStock;

    private String sortBy = "id";
    private String sortDir = "desc";

    private int page = 0;
    private int size = 12;
}
