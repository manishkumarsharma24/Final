package com.shopverse.application.usecase.recommendation;

import com.shopverse.domain.event.AnalyticsEvent;
import com.shopverse.domain.port.EventPublisher;
import com.shopverse.domain.port.RecommendationRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Use case: track product interactions for graph and analytics.
 *
 * Responsibilities:
 *   1. Record FREQUENTLY_BOUGHT_TOGETHER in Neo4j when an order is placed
 *   2. Record VIEWED_AFTER in Neo4j when a customer views a product
 *   3. Publish AnalyticsEvent to Kafka for the analytics pipeline
 *   4. Upsert product nodes before creating relationships
 */
@Service
public class TrackProductInteractionUseCase {

    private final RecommendationRepository recommendationRepository;
    private final EventPublisher           eventPublisher;

    public TrackProductInteractionUseCase(RecommendationRepository recommendationRepository,
                                          EventPublisher eventPublisher) {
        this.recommendationRepository = recommendationRepository;
        this.eventPublisher           = eventPublisher;
    }

    /**
     * Called after an order is placed.
     * Records FREQUENTLY_BOUGHT_TOGETHER for every pair of products in the order.
     */
    public void trackOrderPurchase(Long orderId, List<Long> productIds,
                                   List<String> productNames, List<String> productCategories,
                                   Long customerId, String sessionId) {
        // Upsert all product nodes first
        for (int i = 0; i < productIds.size(); i++) {
            recommendationRepository.upsertProductNode(
                    productIds.get(i),
                    productNames.get(i),
                    productCategories.get(i),
                    0.0);
        }

        // Create FREQUENTLY_BOUGHT_TOGETHER for every pair
        for (int i = 0; i < productIds.size(); i++) {
            for (int j = i + 1; j < productIds.size(); j++) {
                recommendationRepository.recordPurchasedTogether(
                        productIds.get(i), productIds.get(j), orderId);
            }
        }

        // Publish analytics conversion event
        eventPublisher.publish(new AnalyticsEvent.OrderConverted(
                orderId, customerId, java.math.BigDecimal.ZERO, sessionId, Instant.now()));
    }

    /**
     * Called when a customer views a product.
     * Records VIEWED_AFTER relationship and publishes analytics event.
     */
    public void trackProductView(Long productId, String productName, String category,
                                  Long customerId, String sessionId,
                                  Long previousProductId) {
        // Upsert the viewed product node
        recommendationRepository.upsertProductNode(productId, productName, category, 0.0);

        // If customer came from another product page, record VIEWED_AFTER
        if (previousProductId != null && !previousProductId.equals(productId)) {
            recommendationRepository.recordViewedAfter(previousProductId, productId, sessionId);
        }

        // Publish to Kafka analytics pipeline
        eventPublisher.publish(new AnalyticsEvent.ProductViewed(
                productId, productName, category, customerId, sessionId, Instant.now()));
    }
}
