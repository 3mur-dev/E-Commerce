package com.omar.ecommerce.dtos;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class MessageRequest {

    @NotBlank
    private String message;

    @NotBlank
    private String name;

    @NotBlank
    private String email;

}
