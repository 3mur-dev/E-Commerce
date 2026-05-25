package com.omar.ecommerce.events.listener;

import com.omar.ecommerce.entities.AuditLog;
import com.omar.ecommerce.events.events.ProductAddEvent;
import com.omar.ecommerce.events.events.ProductDeleteEvent;
import com.omar.ecommerce.events.events.ProductUpdateEvent;
import com.omar.ecommerce.repositories.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductAuditListener {

    public static final String ACTION_PRODUCT_ADD = "PRODUCT_ADD";
    public static final String ACTION_PRODUCT_UPDATE = "PRODUCT_UPDATE";
    public static final String ACTION_PRODUCT_DELETE = "PRODUCT_DELETE";
    public static final String ENTITY_PRODUCT = "PRODUCT";

    private final AuditLogRepository auditLogRepository;



    @Async
    @EventListener
    public void handleAdd(ProductAddEvent event) {

        AuditLog auditLog = new AuditLog();
        auditLog.setAction(ACTION_PRODUCT_ADD);
        auditLog.setEntityType(ENTITY_PRODUCT);
        auditLog.setEntityId(event.getProductId());
        auditLog.setPerformedBy(event.getPerformedBy());
        auditLog.setDetails("Product add with stock: " + event.getStock());
        auditLog.setIpAddress(event.getIpAddress());
        auditLog.setUserAgent(event.getUserAgent());

        auditLogRepository.save(auditLog);
    }

    @Async
    @EventListener
    public void handleUpdate(ProductUpdateEvent event) {

        AuditLog auditLog = new AuditLog();
        auditLog.setAction(ACTION_PRODUCT_UPDATE);
        auditLog.setEntityType(ENTITY_PRODUCT);
        auditLog.setEntityId(event.getProductId());
        auditLog.setPerformedBy(event.getPerformedBy());
        auditLog.setDetails("Product updated with stock: " + event.getStock());
        auditLog.setIpAddress(event.getIpAddress());
        auditLog.setUserAgent(event.getUserAgent());

        auditLogRepository.save(auditLog);
    }

    @Async
    @EventListener
    public void handleDelete(ProductDeleteEvent event) {
        AuditLog auditLog = new AuditLog();

        auditLog.setAction(ACTION_PRODUCT_DELETE);
        auditLog.setEntityType(ENTITY_PRODUCT);
        auditLog.setEntityId(event.getProductId());
        auditLog.setPerformedBy(event.getPerformedBy());
        auditLog.setDetails("Product deleted");
        auditLog.setIpAddress(event.getIpAddress());
        auditLog.setUserAgent(event.getUserAgent());

        auditLogRepository.save(auditLog);
    }
}