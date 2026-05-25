package com.omar.ecommerce.dtos;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class QuantityRequest {
    @Min(1)
    private int quantity;
}
