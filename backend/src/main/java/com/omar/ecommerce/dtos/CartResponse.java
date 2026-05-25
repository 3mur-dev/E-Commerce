package com.omar.ecommerce.dtos;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CartResponse {
    private List<CartItemDTO> items;
    private BigDecimal total;
}
