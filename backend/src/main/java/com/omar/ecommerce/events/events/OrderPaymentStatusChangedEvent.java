package com.omar.ecommerce.events.events;

public class OrderPaymentStatusChangedEvent {

    private final Long orderId;
    private final String paymentStatus;
    private final String performedBy;
    private final String paymentIntentId;

    public OrderPaymentStatusChangedEvent(Long orderId, String paymentStatus, String performedBy, String paymentIntentId) {
        this.orderId = orderId;
        this.paymentStatus = paymentStatus;
        this.performedBy = performedBy;
        this.paymentIntentId = paymentIntentId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public String getPaymentIntentId() {
        return paymentIntentId;
    }
}
