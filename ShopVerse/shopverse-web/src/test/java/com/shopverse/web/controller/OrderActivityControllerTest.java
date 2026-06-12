package com.shopverse.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopverse.infrastructure.cassandra.OrderActivityEntity;
import com.shopverse.test.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("OrderActivityController — Cassandra activity log")
class OrderActivityControllerTest extends BaseIntegrationTest {

    @Autowired ObjectMapper objectMapper;

    private Long   customerId;
    private String userJwt;

    @BeforeEach
    void setUp() throws Exception {
        String resp = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "firstName", "Frank",
                                "lastName",  "Activity",
                                "email",     "frank@test.com",
                                "password",  "pass123"))))
                .andReturn().getResponse().getContentAsString();
        customerId = objectMapper.readTree(resp).path("data").path("customerId").longValue();
        userJwt    = "Bearer " + objectMapper.readTree(resp).path("data").path("token").asText();
    }

    @Test
    @DisplayName("GET /api/orders/activity/{customerId} → 200 returns activity from Cassandra")
    void get_all_activity_returns_200() throws Exception {
        when(orderActivityCassandraRepository.findByCustomerIdOrderByEventTimeDesc(customerId))
                .thenReturn(List.of(
                        makeEntity(customerId, 1L, "ORDER_PLACED",   "Order #1 placed"),
                        makeEntity(customerId, 1L, "ORDER_CONFIRMED","Order #1 confirmed")));

        mockMvc.perform(get("/api/orders/activity/" + customerId)
                        .header("Authorization", userJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].eventType").value("ORDER_PLACED"))
                .andExpect(jsonPath("$.data[1].eventType").value("ORDER_CONFIRMED"));
    }

    @Test
    @DisplayName("GET /api/orders/activity/{customerId}?since=... → 200 returns recent activity")
    void get_recent_activity_with_since_filter() throws Exception {
        Instant since = Instant.now().minusSeconds(3600);
        when(orderActivityCassandraRepository.findRecentActivity(eq(customerId), any()))
                .thenReturn(List.of(
                        makeEntity(customerId, 2L, "ORDER_SHIPPED", "Order #2 shipped")));

        mockMvc.perform(get("/api/orders/activity/" + customerId)
                        .param("since", since.toString())
                        .header("Authorization", userJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].eventType").value("ORDER_SHIPPED"));
    }

    @Test
    @DisplayName("GET /api/orders/activity/{customerId} → 200 returns empty list when no activity")
    void get_activity_empty_list() throws Exception {
        when(orderActivityCassandraRepository.findByCustomerIdOrderByEventTimeDesc(customerId))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/orders/activity/" + customerId)
                        .header("Authorization", userJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("GET /api/orders/activity/{customerId} → 401 when unauthenticated")
    void get_activity_unauthenticated() throws Exception {
        mockMvc.perform(get("/api/orders/activity/" + customerId))
                .andExpect(status().isUnauthorized());
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private OrderActivityEntity makeEntity(Long customerId, Long orderId,
                                           String eventType, String details) {
        // Uses the parameterized constructor — entity has no setters
        return new OrderActivityEntity(customerId, orderId, eventType, details);
    }
}
