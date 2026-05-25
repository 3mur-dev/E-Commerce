package com.omar.ecommerce.util;

import com.omar.ecommerce.dtos.ApiResponse;

public class ApiResponseUtil {

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }
}
