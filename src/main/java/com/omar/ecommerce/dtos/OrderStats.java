package com.omar.ecommerce.dtos;

import com.omar.ecommerce.entities.Order;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
public class OrderStats {

    private List<Order> orders;
    private long totalShipped;
    private long totalPending;
    private BigDecimal totalRevenue;

}
