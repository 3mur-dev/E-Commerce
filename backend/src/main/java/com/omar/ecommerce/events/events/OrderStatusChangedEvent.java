package com.omar.ecommerce.events.events;

import com.omar.ecommerce.entities.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class OrderStatusChangedEvent {
    private Long orderId;
    private OrderStatus status;
    private String performedBy;
    private String ipAddress;
    private String userAgent;
}