package com.shopverse.domain.port;

import com.shopverse.domain.model.Recommendation;

import java.util.List;

/**
 * Domain port for Neo4j recommendation graph.
 * Implementations live in shopverse-infrastructure (Neo4j adapter).
 */
public interface RecommendationRepository {

    /** Get "frequently bought together" + "viewed after" recommendations for a product. */
    List<Recommendation> findRecommendations(Long productId);

    /** Get top-rated products in same category. */
    List<Recommendation> findTopRatedInCategory(String category, Long excludeProductId);

    /** Record a "frequently bought together" relationship after an order is placed. */
    void recordPurchasedTogether(Long productId1, Long productId2, Long orderId);

    /** Record a "viewed after" relationship — user viewed productId2 after productId1. */
    void recordViewedAfter(Long fromProductId, Long toProductId, String sessionId);

    /** Ensure a ProductNode exists for the given product (upsert). */
    void upsertProductNode(Long productId, String name, String category, double avgRating);
}
