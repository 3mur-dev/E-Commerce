package com.omar.ecommerce.entities;

public enum OrderStatus {

    PENDING,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    REFUNDED;

    public String cssClass() {
        return "status-" + name().toLowerCase();
    }
}
