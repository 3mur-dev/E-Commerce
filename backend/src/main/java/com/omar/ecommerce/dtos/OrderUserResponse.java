package com.omar.ecommerce.dtos;

import lombok.Data;

@Data
public class OrderUserResponse {
    private Long id;
    private String name;
    private String email;
}
