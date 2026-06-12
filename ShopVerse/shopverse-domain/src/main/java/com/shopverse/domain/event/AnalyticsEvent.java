package com.shopverse.domain.event;

import java.time.Instant;

/**
 * Sealed interface for analytics domain events.
 * Kafka topic: shopverse.analytics — pipeline for product views, searches, and conversions.
 */
public sealed interface AnalyticsEvent permits
        AnalyticsEvent.ProductViewed,
        AnalyticsEvent.ProductSearched,
        AnalyticsEvent.ProductAddedToCart,
        AnalyticsEvent.CheckoutStarted,
        AnalyticsEvent.OrderConverted {

    String sessionId();
    Instant occurredAt();

    record ProductViewed(
            Long productId, String productName, String category,
            Long customerId, String sessionId, Instant occurredAt)
            implements AnalyticsEvent {}

    record ProductSearched(
            String query, int resultsCount,
            String sessionId, Instant occurredAt)
            implements AnalyticsEvent {}

    record ProductAddedToCart(
            Long productId, int quantity, Long customerId,
            String sessionId, Instant occurredAt)
            implements AnalyticsEvent {}

    record CheckoutStarted(
            Long customerId, java.math.BigDecimal cartTotal,
            int itemCount, String sessionId, Instant occurredAt)
            implements AnalyticsEvent {}

    record OrderConverted(
            Long orderId, Long customerId,
            java.math.BigDecimal total, String sessionId, Instant occurredAt)
            implements AnalyticsEvent {}
}
