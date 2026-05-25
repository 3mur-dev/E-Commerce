package com.omar.ecommerce.controller;

import com.omar.ecommerce.services.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class StripeWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void webhook_isReachableWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/stripe/webhook")
                        .contentType("application/json")
                        .header("Stripe-Signature", "t=1,v1=fake")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
