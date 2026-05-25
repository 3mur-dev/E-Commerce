package com.omar.ecommerce.dtos;

import com.omar.ecommerce.entities.Order;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CheckoutResult {
    private final Order order;
    private final String stripeCheckoutUrl;
}
