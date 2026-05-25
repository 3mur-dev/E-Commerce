package com.omar.ecommerce.dtos;

import com.omar.ecommerce.entities.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CheckoutRequest {

    @NotBlank
    @Size(max = 100)
    private String customerName;

    @NotBlank
    @Size(max = 150)
    private String customerEmail;

    @NotBlank
    @Size(max = 30)
    private String phone;

    @NotBlank
    @Size(max = 150)
    private String addressLine1;

    @Size(max = 150)
    private String addressLine2;

    @NotBlank
    @Size(max = 80)
    private String city;

    @Size(max = 80)
    private String state;

    @NotBlank
    @Size(max = 20)
    private String postalCode;

    @NotBlank
    @Size(max = 80)
    private String country;

    @NotNull
    private PaymentMethod paymentMethod;

    @Size(max = 500)
    private String note;

    @NotBlank
    @Size(max = 80)
    private String idempotencyKey;
}
