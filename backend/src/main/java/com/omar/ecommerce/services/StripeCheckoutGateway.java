package com.omar.ecommerce.services;

import com.omar.ecommerce.entities.Order;

public interface StripeCheckoutGateway {
    String createCheckoutSession(Order order);
}
