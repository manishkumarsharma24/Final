package com.shopverse.application.usecase.analytics;

import com.shopverse.domain.event.AnalyticsEvent;
import com.shopverse.domain.port.EventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Use case: publish analytics events to Kafka for the analytics pipeline.
 * Called from REST endpoints when frontend reports user interactions.
 */
@Service
public class TrackAnalyticsUseCase {

    private final EventPublisher eventPublisher;

    public TrackAnalyticsUseCase(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void trackProductView(Long productId, String productName, String category,
                                  Long customerId, String sessionId) {
        eventPublisher.publish(new AnalyticsEvent.ProductViewed(
                productId, productName, category, customerId, sessionId, Instant.now()));
    }

    public void trackSearch(String query, int resultsCount, String sessionId) {
        eventPublisher.publish(new AnalyticsEvent.ProductSearched(
                query, resultsCount, sessionId, Instant.now()));
    }

    public void trackAddToCart(Long productId, int quantity, Long customerId, String sessionId) {
        eventPublisher.publish(new AnalyticsEvent.ProductAddedToCart(
                productId, quantity, customerId, sessionId, Instant.now()));
    }

    public void trackCheckoutStarted(Long customerId, BigDecimal cartTotal,
                                      int itemCount, String sessionId) {
        eventPublisher.publish(new AnalyticsEvent.CheckoutStarted(
                customerId, cartTotal, itemCount, sessionId, Instant.now()));
    }

    public void trackOrderConverted(Long orderId, Long customerId,
                                     BigDecimal total, String sessionId) {
        eventPublisher.publish(new AnalyticsEvent.OrderConverted(
                orderId, customerId, total, sessionId, Instant.now()));
    }
}
