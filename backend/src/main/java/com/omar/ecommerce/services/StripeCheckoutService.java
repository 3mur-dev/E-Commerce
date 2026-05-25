package com.omar.ecommerce.services;

import com.omar.ecommerce.entities.Order;
import com.omar.ecommerce.entities.OrderItem;
import com.omar.ecommerce.entities.PaymentMethod;
import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class StripeCheckoutService implements StripeCheckoutGateway {

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${stripe.currency:usd}")
    private String currency;

    @Value("${stripe.secret.key}")
    private String secretKey;

    @PostConstruct
    public void init() {
        if (StringUtils.hasText(secretKey)) {
            Stripe.apiKey = secretKey;
        }
    }

    public String createCheckoutSession(Order order) {
        if (order == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order is required");
        }
        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order has no items");
        }
        if (order.getPaymentMethod() == PaymentMethod.CASH_ON_DELIVERY) {
            return null;
        }
        if (!StringUtils.hasText(secretKey)) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Stripe is not configured");
        }

        try {
            SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setCustomerEmail(order.getCustomerEmail())
                    .setClientReferenceId(order.getId().toString())
                    .setSuccessUrl(buildSuccessUrl())
                    .setCancelUrl(buildCancelUrl())
                    .putMetadata("orderId", String.valueOf(order.getId()))
                    .putMetadata("orderNumber", order.getOrderNumber())
                    .setPaymentIntentData(
                            SessionCreateParams.PaymentIntentData.builder()
                                    .putMetadata("orderId", String.valueOf(order.getId()))
                                    .putMetadata("orderNumber", order.getOrderNumber())
                                    .build()
                    )
                    .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD);

            for (OrderItem item : order.getItems()) {
                paramsBuilder.addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity((long) item.getQuantity())
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency(currency)
                                                .setUnitAmount(toMinorUnits(item.getPrice()))
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName(item.getProduct().getName())
                                                                .setDescription(buildDescription(item))
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                );
            }

            Session session = Session.create(paramsBuilder.build());
            return session.getUrl();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to create Stripe checkout session", e);
        }
    }

    private String buildSuccessUrl() {
        return normalizeBaseUrl() + "/checkout/success?session_id={CHECKOUT_SESSION_ID}";
    }

    private String buildCancelUrl() {
        return normalizeBaseUrl() + "/checkout/cancel";
    }

    private String normalizeBaseUrl() {
        if (frontendUrl == null || frontendUrl.isBlank()) {
            return "http://localhost:5173";
        }
        return frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
    }

    private Long toMinorUnits(BigDecimal amount) {
        if (amount == null) {
            return 0L;
        }
        return amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    private String buildDescription(OrderItem item) {
        String productName = item.getProduct() != null ? item.getProduct().getName() : null;
        if (productName == null || productName.isBlank()) {
            return null;
        }
        return "Order item for " + productName;
    }
}
