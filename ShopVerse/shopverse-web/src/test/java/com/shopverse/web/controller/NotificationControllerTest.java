package com.shopverse.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopverse.test.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class NotificationControllerTest extends BaseIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void sendNotification_requiresAdminRole() throws Exception {
        var body = Map.of(
            "orderId", 1,
            "customerEmail", "user@test.com",
            "type", "ORDER_CONFIRMED",
            "customerId", 10,
            "amount", "99.99"
        );

        // Without auth — should be forbidden
        mockMvc.perform(post("/api/notifications/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
    }

    @Test
    void sendNotification_adminCanSendOrderConfirmed() throws Exception {
        var body = Map.of(
            "orderId", 1,
            "customerEmail", "user@test.com",
            "type", "ORDER_CONFIRMED",
            "customerId", 10,
            "amount", "99.99"
        );

        mockMvc.perform(post("/api/notifications/send")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", adminToken("admin@shopverse.com"))
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Notification queued via RabbitMQ"));
    }

    @Test
    void sendNotification_adminCanSendShippingUpdate() throws Exception {
        var body = Map.of(
            "orderId", 2,
            "customerEmail", "user@test.com",
            "type", "ORDER_SHIPPED",
            "trackingNumber", "TRK-12345"
        );

        mockMvc.perform(post("/api/notifications/send")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", adminToken("admin@shopverse.com"))
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    @Test
    void paymentCallback_acceptsWithoutAuth() throws Exception {
        var body = Map.of(
            "orderId", 1,
            "status", "SUCCESS",
            "amount", "49.99",
            "paymentMethod", "STRIPE",
            "customerEmail", "user@test.com"
        );

        mockMvc.perform(post("/api/notifications/payment/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Payment callback received"));
    }

    @Test
    void registerWebhook_requiresAuthentication() throws Exception {
        var body = Map.of("webhookUrl", "https://merchant.com/hooks");

        // Without auth
        mockMvc.perform(post("/api/notifications/webhook/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());

        // With auth
        mockMvc.perform(post("/api/notifications/webhook/register")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", userToken("user@shopverse.com"))
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
    }
}
