package com.omar.ecommerce.events.events;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProductUpdateEvent {

    private final Long productId;
    private final int stock;
    private final String performedBy;
    private final String ipAddress;
    private final String userAgent;
}