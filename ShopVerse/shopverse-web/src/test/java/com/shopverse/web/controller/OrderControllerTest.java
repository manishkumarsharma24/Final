package com.shopverse.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopverse.domain.model.OrderActivity;
import com.shopverse.test.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("OrderController — place, lifecycle, activity")
class OrderControllerTest extends BaseIntegrationTest {

    @Autowired ObjectMapper objectMapper;

    private Long   customerId;
    private Long   productId;
    private Long   orderId;
    private String userJwt;
    private String adminJwt;

    @BeforeEach
    void setUp() throws Exception {
        // Register a customer
        String regResp = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "firstName", "Charlie",
                                "lastName",  "Brown",
                                "email",     "charlie@test.com",
                                "password",  "pass123"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        customerId = objectMapper.readTree(regResp).path("data").path("customerId").longValue();
        userJwt    = "Bearer " + objectMapper.readTree(regResp).path("data").path("token").asText();
        adminJwt   = adminToken("admin@shopverse.com");

        // Create a product (admin)
        String prodResp = mockMvc.perform(post("/api/products")
                        .header("Authorization", adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name",          "Order Widget",
                                "description",   "For ordering",
                                "price",         "10.00",
                                "currency",      "USD",
                                "category",      "Test",
                                "stockQuantity", 100))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        productId = objectMapper.readTree(prodResp).path("data").path("id").longValue();

        // Place an order
        String orderResp = mockMvc.perform(post("/api/orders")
                        .header("Authorization", userJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "customerId",    customerId,
                                "street",        "1 Test St",
                                "city",          "Testville",
                                "state",         "CA",
                                "postalCode",    "90210",
                                "country",       "US",
                                "items",         List.of(Map.of(
                                        "productId", productId,
                                        "quantity",  2))))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        orderId = objectMapper.readTree(orderResp).path("data").path("id").longValue();
    }

    // ── Place order ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/orders → 201 with order details")
    void place_order_returns_201() throws Exception {
        // Verified by setUp(); just assert the data we got
        mockMvc.perform(get("/api/orders/activity/" + customerId)
                        .header("Authorization", userJwt))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/orders → 401 when unauthenticated")
    void place_order_unauthenticated() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "customerId", customerId,
                                "street", "1 St", "city", "City", "postalCode", "12345",
                                "country", "US",
                                "items", List.of(Map.of("productId", productId, "quantity", 1))))))
                .andExpect(status().isUnauthorized());
    }

    // ── Status transitions ────────────────────────────────────────────────────

    @Test
    @DisplayName("Full lifecycle: place → confirm → process → ship → deliver")
    void full_order_lifecycle() throws Exception {
        // CONFIRM
        mockMvc.perform(patch("/api/orders/" + orderId + "/confirm")
                        .header("Authorization", adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));

        // PROCESS
        mockMvc.perform(patch("/api/orders/" + orderId + "/process")
                        .header("Authorization", adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PROCESSING"));

        // SHIP
        mockMvc.perform(patch("/api/orders/" + orderId + "/ship")
                        .header("Authorization", adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("trackingNumber", "TRK-TEST-001"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SHIPPED"));

        // DELIVER
        mockMvc.perform(patch("/api/orders/" + orderId + "/deliver")
                        .header("Authorization", adminJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DELIVERED"));
    }

    @Test
    @DisplayName("PATCH /api/orders/{id}/cancel → 200 cancels from PENDING")
    void cancel_order_from_pending() throws Exception {
        mockMvc.perform(patch("/api/orders/" + orderId + "/cancel")
                        .header("Authorization", userJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("PATCH /api/orders/{id}/confirm → 403 when not admin")
    void confirm_order_as_user_forbidden() throws Exception {
        mockMvc.perform(patch("/api/orders/" + orderId + "/confirm")
                        .header("Authorization", userJwt))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH /api/orders/{id}/confirm → 422 on invalid transition (DELIVERED → CONFIRMED)")
    void invalid_transition_returns_422() throws Exception {
        // Drive order to DELIVERED
        mockMvc.perform(patch("/api/orders/" + orderId + "/confirm").header("Authorization", adminJwt));
        mockMvc.perform(patch("/api/orders/" + orderId + "/process").header("Authorization", adminJwt));
        mockMvc.perform(patch("/api/orders/" + orderId + "/ship")
                        .header("Authorization", adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trackingNumber\":\"TRK-X\"}"));
        mockMvc.perform(patch("/api/orders/" + orderId + "/deliver").header("Authorization", adminJwt));

        // Now try to CONFIRM again — should be 422
        mockMvc.perform(patch("/api/orders/" + orderId + "/confirm")
                        .header("Authorization", adminJwt))
                .andExpect(status().isUnprocessableEntity());
    }

    // ── Activity log ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/orders/activity/{customerId} → 200 (reads from Cassandra mock)")
    void get_order_activity() throws Exception {
        // Cassandra mock returns empty list by default; just verify endpoint works
        when(orderActivityCassandraRepository.findByCustomerIdOrderByEventTimeDesc(customerId))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/orders/activity/" + customerId)
                        .header("Authorization", userJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /api/orders/activity/{customerId} → 401 when unauthenticated")
    void get_activity_unauthenticated() throws Exception {
        mockMvc.perform(get("/api/orders/activity/" + customerId))
                .andExpect(status().isUnauthorized());
    }
}
