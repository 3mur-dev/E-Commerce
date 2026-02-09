package com.omar.ecommerce.entities;

public enum OrderStatus {
    PENDING,
    PAID,
    SHIPPED,
    DELIVERED,
    CANCELLED;

    public String cssClass() {
        return "status-" + name().toLowerCase();
    }
}
