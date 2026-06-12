package com.shopverse.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopverse.test.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AnalyticsControllerTest extends BaseIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void trackProductView_acceptsWithoutAuth() throws Exception {
        var body = Map.of(
            "eventType", "PRODUCT_VIEW",
            "sessionId", "sess-001",
            "productId", 1,
            "productName", "Laptop",
            "category", "Electronics",
            "customerId", 10
        );

        mockMvc.perform(post("/api/analytics/track")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void trackSearch_publishesSearchEvent() throws Exception {
        var body = Map.of(
            "eventType", "SEARCH",
            "sessionId", "sess-002",
            "query", "laptop",
            "resultsCount", 15
        );

        mockMvc.perform(post("/api/analytics/track")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    @Test
    void trackAddToCart_publishesCartEvent() throws Exception {
        var body = Map.of(
            "eventType", "ADD_TO_CART",
            "sessionId", "sess-003",
            "productId", 2,
            "quantity", 1,
            "customerId", 10
        );

        mockMvc.perform(post("/api/analytics/track/cart")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cart event tracked"));
    }

    @Test
    void trackView_shorthandEndpoint_works() throws Exception {
        var body = Map.of(
            "eventType", "PRODUCT_VIEW",
            "sessionId", "sess-004",
            "productId", 3,
            "productName", "Phone",
            "category", "Mobile",
            "customerId", 20
        );

        mockMvc.perform(post("/api/analytics/track/view")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Product view tracked"));
    }

    @Test
    void trackSearch_shorthandEndpoint_works() throws Exception {
        var body = Map.of(
            "eventType", "SEARCH",
            "sessionId", "sess-005",
            "query", "wireless headphones",
            "resultsCount", 8
        );

        mockMvc.perform(post("/api/analytics/track/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Search tracked"));
    }

    @Test
    void unknownEventType_returns400() throws Exception {
        var body = Map.of(
            "eventType", "UNKNOWN_EVENT",
            "sessionId", "sess-006"
        );

        mockMvc.perform(post("/api/analytics/track")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().is4xxClientError());
    }
}
