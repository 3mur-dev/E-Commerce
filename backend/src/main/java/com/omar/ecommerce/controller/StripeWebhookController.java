package com.omar.ecommerce.controller;

import com.omar.ecommerce.services.OrderService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stripe")
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookController {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    @Value("${stripe.webhook.secret:}")
    private String endpointSecret;

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader
    ) {
        try {
            Event event = Webhook.constructEvent(
                    payload, sigHeader, endpointSecret
            );
            log.info("Received Stripe webhook event {} ({})", event.getId(), event.getType());
            processEvent(event, payload);
            return ResponseEntity.ok("success");
        } catch (SignatureVerificationException e) {
            log.warn("Rejected Stripe webhook with invalid signature: {}", e.getMessage());
            return ResponseEntity.badRequest().body("invalid signature");
        } catch (Exception e) {
            log.error("Stripe webhook processing failed", e);
            return ResponseEntity.internalServerError().body("webhook error");
        }
    }

    private void processEvent(Event event, String payload) throws Exception {
        switch (event.getType()) {
            case "checkout.session.completed":
                handleCheckoutSessionCompleted(event, payload);
                break;
            case "payment_intent.succeeded":
                handlePaymentIntentSucceeded(event, payload);
                break;
            case "payment_intent.payment_failed":
                handlePaymentIntentFailed(event, payload);
                break;
            case "checkout.session.expired":
                handleCheckoutSessionExpired(event, payload);
                break;
            default:
                log.info("Ignoring unsupported Stripe event type {}", event.getType());
        }
    }

    private void handleCheckoutSessionCompleted(Event event, String payload) throws Exception {
        Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
        if (session == null) {
            String sessionId = extractObjectId(payload);
            if (sessionId == null) {
                log.warn("Stripe checkout.session.completed webhook received without session object or id");
                return;
            }
            session = Session.retrieve(sessionId);
        }

        String orderIdStr = resolveOrderId(
                session.getMetadata() != null ? session.getMetadata().get("orderId") : null,
                session.getClientReferenceId()
        );
        if (orderIdStr == null) {
            log.warn("Stripe checkout session completed without order metadata");
            return;
        }

        orderService.markPaymentPaid(Long.parseLong(orderIdStr), session.getPaymentIntent());
        log.info("Marked order {} as paid from Stripe webhook", orderIdStr);
    }

    private void handlePaymentIntentSucceeded(Event event, String payload) throws Exception {
        PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);
        if (paymentIntent == null) {
            String extractedPaymentIntentId = extractObjectId(payload);
            if (extractedPaymentIntentId == null) {
                log.warn("Stripe payment_intent.succeeded webhook received without payment intent object or id");
                return;
            }
            paymentIntent = PaymentIntent.retrieve(extractedPaymentIntentId);
        }

        String paymentOrderIdStr = paymentIntent.getMetadata() != null
                ? paymentIntent.getMetadata().get("orderId")
                : null;
        if (paymentOrderIdStr == null) {
            log.warn("Stripe payment intent succeeded without order metadata");
            return;
        }

        orderService.markPaymentPaid(Long.parseLong(paymentOrderIdStr), paymentIntent.getId());
        log.info("Marked order {} as paid from Stripe payment intent webhook", paymentOrderIdStr);
    }

    private void handlePaymentIntentFailed(Event event, String payload) throws Exception {
        PaymentIntent failedPaymentIntent = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);
        if (failedPaymentIntent == null) {
            String failedPaymentIntentId = extractObjectId(payload);
            if (failedPaymentIntentId == null) {
                log.warn("Stripe payment_intent.payment_failed webhook received without payment intent object or id");
                return;
            }
            failedPaymentIntent = PaymentIntent.retrieve(failedPaymentIntentId);
        }

        String failedOrderIdStr = failedPaymentIntent.getMetadata() != null
                ? failedPaymentIntent.getMetadata().get("orderId")
                : null;
        if (failedOrderIdStr == null) {
            log.warn("Stripe payment intent failed without order metadata");
            return;
        }

        orderService.markPaymentFailed(Long.parseLong(failedOrderIdStr), "Stripe payment failed");
        log.info("Marked order {} as payment failed from Stripe payment intent webhook", failedOrderIdStr);
    }

    private void handleCheckoutSessionExpired(Event event, String payload) throws Exception {
        Session expiredSession = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
        if (expiredSession == null) {
            String expiredSessionId = extractObjectId(payload);
            if (expiredSessionId == null) {
                log.warn("Stripe checkout.session.expired webhook received without session object or id");
                return;
            }
            expiredSession = Session.retrieve(expiredSessionId);
        }

        String expiredOrderIdStr = resolveOrderId(
                expiredSession.getMetadata() != null ? expiredSession.getMetadata().get("orderId") : null,
                expiredSession.getClientReferenceId()
        );
        if (expiredOrderIdStr == null) {
            log.warn("Stripe checkout session expired without order metadata");
            return;
        }

        orderService.markPaymentFailed(Long.parseLong(expiredOrderIdStr), "Stripe checkout session expired");
        log.info("Marked order {} as expired from Stripe checkout session webhook", expiredOrderIdStr);
    }

    private String resolveOrderId(String metadataOrderId, String clientReferenceId) {
        if (metadataOrderId != null && !metadataOrderId.isBlank()) {
            return metadataOrderId;
        }
        if (clientReferenceId != null && !clientReferenceId.isBlank()) {
            return clientReferenceId;
        }
        return null;
    }

    private String extractObjectId(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode objectNode = root.path("data").path("object");
            String id = objectNode.path("id").asText(null);
            return id == null || id.isBlank() ? null : id;
        } catch (Exception e) {
            log.warn("Failed to parse Stripe webhook payload for object id", e);
            return null;
        }
    }
}
