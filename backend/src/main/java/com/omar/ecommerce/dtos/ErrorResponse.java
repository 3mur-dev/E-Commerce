package com.omar.ecommerce.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String message,
        int status,
        LocalDateTime timestamp,
        Map<String, String> errors
) {
    public ErrorResponse(String message, int status) {
        this(message, status, LocalDateTime.now(), null);
    }

    public ErrorResponse(String message, int status, Map<String, String> errors) {
        this(message, status, LocalDateTime.now(), errors);
    }
}
