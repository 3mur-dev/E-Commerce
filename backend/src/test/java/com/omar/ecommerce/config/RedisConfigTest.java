package com.omar.ecommerce.config;

import com.omar.ecommerce.dtos.OrderItemResponse;
import com.omar.ecommerce.dtos.OrderResponse;
import com.omar.ecommerce.dtos.ProductResponse;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedisConfigTest {

    @Test
    void redisSerializerPreservesDtoType() {
        RedisConfig config = new RedisConfig();
        GenericJackson2JsonRedisSerializer serializer =
                config.redisValueSerializer();

        ProductResponse response = new ProductResponse();
        response.setId(7L);
        response.setName("Keyboard");
        response.setPrice(new BigDecimal("24.99"));
        response.setStock(10);

        byte[] payload = serializer.serialize(response);
        ProductResponse restored = serializer.deserialize(payload, ProductResponse.class);

        assertNotNull(restored);
        assertEquals(7L, restored.getId());
        assertEquals("Keyboard", restored.getName());
        assertEquals(new BigDecimal("24.99"), restored.getPrice());
        assertEquals(10, restored.getStock());
    }

    @Test
    void redisSerializerPreservesOrderListPayload() {
        RedisConfig config = new RedisConfig();
        org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer<Object> serializer =
                config.redisListValueSerializer(config.redisObjectMapper());

        OrderItemResponse item = new OrderItemResponse();
        item.setId(11L);
        item.setProductId(99L);
        item.setProductName("Mouse");
        item.setQuantity(2);
        item.setPrice(new BigDecimal("15.00"));
        item.setSubtotal(new BigDecimal("30.00"));

        OrderResponse order = new OrderResponse();
        order.setId(26L);
        order.setOrderNumber("ORD-26");
        order.setCreationTimestamp(LocalDateTime.parse("2026-05-24T16:55:57"));
        order.setTotal(new BigDecimal("30.00"));
        order.setItems(List.of(item));

        byte[] payload = serializer.serialize(List.of(order));
        Object restored = serializer.deserialize(payload);

        assertNotNull(restored);
        assertTrue(restored instanceof List<?>, "Restored type: " + restored.getClass().getName());

        List<?> restoredOrders = (List<?>) restored;
        assertEquals(1, restoredOrders.size());
        assertTrue(restoredOrders.get(0) instanceof Map<?, ?>,
                "Restored element type: " + restoredOrders.get(0).getClass().getName());

        Map<?, ?> restoredOrder = (Map<?, ?>) restoredOrders.get(0);
        assertEquals(26L, ((Number) restoredOrder.get("id")).longValue());
        assertEquals("ORD-26", restoredOrder.get("orderNumber"));
        assertEquals("2026-05-24T16:55:57", restoredOrder.get("creationTimestamp").toString());
        assertEquals(0, new BigDecimal("30.00").compareTo(new BigDecimal(restoredOrder.get("total").toString())));
    }
}
