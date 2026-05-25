package com.omar.ecommerce.entities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderStatusConverterTest {

    @Test
    void convertToEntityAttribute_mapsLegacyPaidToProcessing() {
        OrderStatusConverter converter = new OrderStatusConverter();

        assertEquals(OrderStatus.PROCESSING, converter.convertToEntityAttribute("PAID"));
    }

    @Test
    void convertToEntityAttribute_preservesCurrentValues() {
        OrderStatusConverter converter = new OrderStatusConverter();

        assertEquals(OrderStatus.SHIPPED, converter.convertToEntityAttribute("SHIPPED"));
    }
}
