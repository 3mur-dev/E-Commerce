package com.omar.ecommerce.dtos;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ProductRequest {

    @NotBlank
    private String name;

    @NotNull
    @Min(value = 1, message = ("Price must be greater than 0"))
    private double price;
    private Long categoryId;

}
