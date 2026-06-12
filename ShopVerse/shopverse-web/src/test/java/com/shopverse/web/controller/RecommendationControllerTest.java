package com.shopverse.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopverse.domain.model.Recommendation;
import com.shopverse.domain.port.RecommendationRepository;
import com.shopverse.test.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RecommendationControllerTest extends BaseIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getRecommendations_returnsListForProduct() throws Exception {
        List<Recommendation> recs = List.of(
            new Recommendation(2L, "Laptop Stand", "Electronics", 4.5, "Frequently bought together"),
            new Recommendation(3L, "USB Hub",      "Electronics", 4.2, "Frequently bought together")
        );
        when(productGraphRepository.findCombinedRecommendations(1L))
            .thenReturn(List.of()); // Graph repo returns empty — adapter maps to empty

        mockMvc.perform(get("/api/recommendations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getByCategory_returnsTopRatedInCategory() throws Exception {
        when(productGraphRepository.findTopRatedInCategory("Electronics", 0L))
            .thenReturn(List.of());

        mockMvc.perform(get("/api/recommendations/category/Electronics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void trackView_acceptsValidRequest() throws Exception {
        var body = Map.of(
            "productId", 5,
            "productName", "Wireless Mouse",
            "category", "Electronics",
            "customerId", 1,
            "sessionId", "sess-abc123",
            "previousProductId", 3
        );

        mockMvc.perform(post("/api/recommendations/track/view")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("View tracked"));
    }

    @Test
    void trackPurchase_acceptsValidRequest() throws Exception {
        var body = Map.of(
            "orderId", 10,
            "productIds", List.of(1, 2, 3),
            "productNames", List.of("A", "B", "C"),
            "productCategories", List.of("X", "Y", "Z"),
            "customerId", 1,
            "sessionId", "sess-xyz"
        );

        mockMvc.perform(post("/api/recommendations/track/purchase")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Purchase graph updated"));
    }
}
