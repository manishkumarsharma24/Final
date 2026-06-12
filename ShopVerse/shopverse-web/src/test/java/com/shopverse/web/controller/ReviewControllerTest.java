package com.shopverse.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopverse.domain.model.Review;
import com.shopverse.domain.port.ReviewRepository;
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

@DisplayName("ReviewController — MongoDB reviews")
class ReviewControllerTest extends BaseIntegrationTest {

    @Autowired ObjectMapper objectMapper;
    @Autowired ReviewRepository reviewRepository;  // domain port — backed by MongoReviewRepositoryAdapter

    private Long   productId;
    private Long   customerId;
    private String userJwt;

    @BeforeEach
    void setUp() throws Exception {
        String adminJwt = adminToken("admin@shopverse.com");

        // Create a product
        String prodResp = mockMvc.perform(post("/api/products")
                        .header("Authorization", adminJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Reviewable Widget", "description", "Review me",
                                "price", "19.99", "currency", "USD",
                                "category", "Test", "stockQuantity", 50))))
                .andReturn().getResponse().getContentAsString();
        productId = objectMapper.readTree(prodResp).path("data").path("id").longValue();

        // Register a customer
        String custResp = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "firstName", "Eve", "lastName", "Reviewer",
                                "email", "eve@test.com", "password", "pass"))))
                .andReturn().getResponse().getContentAsString();
        customerId = objectMapper.readTree(custResp).path("data").path("customerId").longValue();
        userJwt    = "Bearer " + objectMapper.readTree(custResp).path("data").path("token").asText();

        // Stub: MongoDB save returns the review with an id
        when(reviewMongoRepository.save(any())).thenAnswer(inv -> {
            var doc = inv.getArgument(0);
            // reflection-free: just return the same doc (it has setId)
            return doc;
        });
    }

    @Test
    @DisplayName("POST /api/products/{id}/reviews → 201 saves review (MongoDB)")
    void submit_review_returns_201() throws Exception {
        // Stub reviewMongoRepository.save to return a document with an ID
        when(reviewMongoRepository.save(any())).thenAnswer(inv -> {
            var doc = (com.shopverse.infrastructure.mongo.ReviewDocument) inv.getArgument(0);
            doc.setId("mongo-id-abc");
            return doc;
        });

        mockMvc.perform(post("/api/products/" + productId + "/reviews")
                        .header("Authorization", userJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "customerId", customerId,
                                "rating",     5,
                                "title",      "Fantastic!",
                                "body",       "Really loved this product.",
                                "tags",       List.of("quality"),
                                "verified",   true))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.review.rating").value(5))
                .andExpect(jsonPath("$.data.review.title").value("Fantastic!"))
                .andExpect(jsonPath("$.data.customer.email").value("eve@test.com"));
    }

    @Test
    @DisplayName("POST /api/products/{id}/reviews → 401 when unauthenticated")
    void submit_review_unauthenticated() throws Exception {
        mockMvc.perform(post("/api/products/" + productId + "/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "customerId", customerId, "rating", 4,
                                "title", "Good", "body", "Nice"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/products/{id}/reviews → 200 lists reviews from MongoDB")
    void get_reviews_for_product() throws Exception {
        // Stub MongoDB repo to return some reviews
        when(reviewMongoRepository.findByProductIdOrderByCreatedAtDesc(productId))
                .thenReturn(List.of(
                        makeDoc("id-1", productId, customerId, 5, "Great"),
                        makeDoc("id-2", productId, customerId, 4, "Good")));

        mockMvc.perform(get("/api/products/" + productId + "/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @DisplayName("GET /api/products/{id}/reviews/stats → 200 returns aggregated stats")
    void get_review_stats() throws Exception {
        when(reviewMongoRepository.aggregateRating(productId))
                .thenReturn(new ReviewMongoStatsStub(4.5, 10L));

        mockMvc.perform(get("/api/products/" + productId + "/reviews/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.averageRating").value(4.5))
                .andExpect(jsonPath("$.data.totalReviews").value(10));
    }

    @Test
    @DisplayName("POST /api/products/{id}/reviews → 400 with invalid rating")
    void submit_review_invalid_rating() throws Exception {
        mockMvc.perform(post("/api/products/" + productId + "/reviews")
                        .header("Authorization", userJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "customerId", customerId,
                                "rating",     10,       // out of 1-5
                                "title",      "Too high",
                                "body",       "Off scale"))))
                .andExpect(status().is4xxClientError());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private com.shopverse.infrastructure.mongo.ReviewDocument makeDoc(
            String id, Long productId, Long customerId, int rating, String title) {
        var doc = new com.shopverse.infrastructure.mongo.ReviewDocument();
        doc.setId(id);
        doc.setProductId(productId);
        doc.setCustomerId(customerId);
        doc.setRating(rating);
        doc.setTitle(title);
        doc.setBody("Body for " + title);
        doc.setCreatedAt(Instant.now());
        return doc;
    }

    /** Stub implementing ReviewMongoRepository.RatingStats for test use. */
    static class ReviewMongoStatsStub
            implements com.shopverse.infrastructure.mongo.ReviewMongoRepository.RatingStats {
        private final double avg;
        private final long count;
        ReviewMongoStatsStub(double avg, long count) { this.avg = avg; this.count = count; }
        @Override public Double getAvg()   { return avg; }
        @Override public Long getCount()   { return count; }
    }
}
