package com.omar.ecommerce.events.listener;

import com.omar.ecommerce.entities.AuditLog;
import com.omar.ecommerce.events.events.OrderPaymentStatusChangedEvent;
import com.omar.ecommerce.repositories.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderPaymentStatusChangedListener {

    public static final String ACTION_ORDER_PAYMENT_STATUS_CHANGE = "ORDER_PAYMENT_STATUS_CHANGE";
    public static final String ENTITY_ORDER = "ORDER";

    private final AuditLogRepository auditLogRepository;
    private final HttpServletRequest request;

    @Async
    @EventListener
    public void handle(OrderPaymentStatusChangedEvent event) {
        String ipAddress = getClientIp();
        String userAgent = request.getHeader("User-Agent");

        AuditLog auditLog = new AuditLog();
        auditLog.setAction(ACTION_ORDER_PAYMENT_STATUS_CHANGE);
        auditLog.setEntityType(ENTITY_ORDER);
        auditLog.setEntityId(event.getOrderId());
        auditLog.setPerformedBy(event.getPerformedBy());
        auditLog.setDetails("Order payment status changed to " + event.getPaymentStatus()
                + (event.getPaymentIntentId() == null ? "" : ", payment intent: " + event.getPaymentIntentId()));
        auditLog.setIpAddress(ipAddress);
        auditLog.setUserAgent(userAgent);

        auditLogRepository.save(auditLog);
    }

    private String getClientIp() {
        String xfHeader = request.getHeader("X-Forwarded-For");

        if (xfHeader == null || xfHeader.isBlank()) {
            return request.getRemoteAddr();
        }

        return xfHeader.split(",")[0];
    }
}
