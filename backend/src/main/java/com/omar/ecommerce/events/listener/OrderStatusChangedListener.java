package com.omar.ecommerce.events.listener;

import com.omar.ecommerce.entities.AuditLog;
import com.omar.ecommerce.events.events.OrderStatusChangedEvent;
import com.omar.ecommerce.repositories.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderStatusChangedListener {

    public static final String ACTION_ORDER_STATUS_CHANGE = "ORDER_STATUS_CHANGE";
    public static final String ENTITY_ORDER = "ORDER";
    private final AuditLogRepository auditLogRepository;


    @Async
    @EventListener
    public void handle(OrderStatusChangedEvent event) {

        AuditLog auditLog = new AuditLog();
        auditLog.setAction(ACTION_ORDER_STATUS_CHANGE);
        auditLog.setEntityType(ENTITY_ORDER);
        auditLog.setEntityId(event.getOrderId());
        auditLog.setPerformedBy(event.getPerformedBy());
        auditLog.setDetails("Order status changed to " + event.getStatus());
        auditLog.setIpAddress(event.getIpAddress());
        auditLog.setUserAgent(event.getUserAgent());

        auditLogRepository.save(auditLog);
    }
}