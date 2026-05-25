package com.omar.ecommerce.dtos;

import com.omar.ecommerce.entities.OrderStatus;
import com.omar.ecommerce.entities.PaymentMethod;
import com.omar.ecommerce.entities.PaymentStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderResponse {
    private Long id;
    private String orderNumber;
    private OrderStatus status;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private LocalDateTime creationTimestamp;
    private BigDecimal total;
    private String sessionUrl;
    private String stripeCheckoutUrl;
    private OrderUserResponse user;
    private String customerName;
    private String customerEmail;
    private String phone;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private String note;
    private List<OrderItemResponse> items;
}