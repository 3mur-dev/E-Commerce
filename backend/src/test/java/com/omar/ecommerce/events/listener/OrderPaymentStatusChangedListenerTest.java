package com.omar.ecommerce.events.listener;

import com.omar.ecommerce.entities.AuditLog;
import com.omar.ecommerce.events.events.OrderPaymentStatusChangedEvent;
import com.omar.ecommerce.repositories.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderPaymentStatusChangedListenerTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private HttpServletRequest request;

    @Test
    void handle_savesPaymentAuditLog() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.10, 10.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("JUnit");

        OrderPaymentStatusChangedListener listener = new OrderPaymentStatusChangedListener(auditLogRepository, request);

        listener.handle(new OrderPaymentStatusChangedEvent(55L, "PAID", "omar", "pi_123"));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog auditLog = captor.getValue();
        assertEquals("ORDER_PAYMENT_STATUS_CHANGE", auditLog.getAction());
        assertEquals("ORDER", auditLog.getEntityType());
        assertEquals(55L, auditLog.getEntityId());
        assertEquals("omar", auditLog.getPerformedBy());
        assertEquals("Order payment status changed to PAID, payment intent: pi_123", auditLog.getDetails());
        assertEquals("203.0.113.10", auditLog.getIpAddress());
        assertEquals("JUnit", auditLog.getUserAgent());
    }
}
