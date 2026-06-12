package com.shopverse.web.controller;

import com.shopverse.application.usecase.analytics.TrackAnalyticsUseCase;
import com.shopverse.shared.ApiResponse;
import com.shopverse.web.dto.AnalyticsTrackRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Analytics tracking endpoints — publishes events to Kafka shopverse.analytics topic.
 *
 * POST /api/analytics/track          — track any user interaction (fire-and-forget)
 * POST /api/analytics/track/view     — shorthand for product view tracking
 * POST /api/analytics/track/search   — shorthand for search tracking
 * POST /api/analytics/track/cart     — shorthand for add-to-cart tracking
 */
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final TrackAnalyticsUseCase trackAnalyticsUseCase;

    public AnalyticsController(TrackAnalyticsUseCase trackAnalyticsUseCase) {
        this.trackAnalyticsUseCase = trackAnalyticsUseCase;
    }

    /**
     * POST /api/analytics/track
     * Generic analytics event — routes based on eventType field.
     * Frontend calls this on every user interaction.
     */
    @PostMapping("/track")
    public ResponseEntity<ApiResponse<Void>> track(@Valid @RequestBody AnalyticsTrackRequest req) {
        switch (req.eventType()) {
            case "PRODUCT_VIEW" ->
                trackAnalyticsUseCase.trackProductView(
                    req.productId(), req.productName(), req.category(),
                    req.customerId(), req.sessionId());
            case "SEARCH" ->
                trackAnalyticsUseCase.trackSearch(
                    req.query(), req.resultsCount() != null ? req.resultsCount() : 0,
                    req.sessionId());
            case "ADD_TO_CART" ->
                trackAnalyticsUseCase.trackAddToCart(
                    req.productId(), req.quantity() != null ? req.quantity() : 1,
                    req.customerId(), req.sessionId());
            case "CHECKOUT_STARTED" ->
                trackAnalyticsUseCase.trackCheckoutStarted(
                    req.customerId(), req.cartTotal(), req.itemCount() != null ? req.itemCount() : 0,
                    req.sessionId());
            case "ORDER_CONVERTED" ->
                trackAnalyticsUseCase.trackOrderConverted(
                    req.orderId(), req.customerId(), req.total(), req.sessionId());
            default -> throw new IllegalArgumentException("Unknown event type: " + req.eventType());
        }
        return ResponseEntity.ok(ApiResponse.ok(null, "Analytics event tracked"));
    }

    /** POST /api/analytics/track/view — shorthand for product view. */
    @PostMapping("/track/view")
    public ResponseEntity<ApiResponse<Void>> trackView(@Valid @RequestBody AnalyticsTrackRequest req) {
        trackAnalyticsUseCase.trackProductView(
                req.productId(), req.productName(), req.category(),
                req.customerId(), req.sessionId());
        return ResponseEntity.ok(ApiResponse.ok(null, "Product view tracked"));
    }

    /** POST /api/analytics/track/search — shorthand for search event. */
    @PostMapping("/track/search")
    public ResponseEntity<ApiResponse<Void>> trackSearch(@Valid @RequestBody AnalyticsTrackRequest req) {
        trackAnalyticsUseCase.trackSearch(
                req.query(), req.resultsCount() != null ? req.resultsCount() : 0, req.sessionId());
        return ResponseEntity.ok(ApiResponse.ok(null, "Search tracked"));
    }

    /** POST /api/analytics/track/cart — shorthand for add-to-cart. */
    @PostMapping("/track/cart")
    public ResponseEntity<ApiResponse<Void>> trackCart(@Valid @RequestBody AnalyticsTrackRequest req) {
        trackAnalyticsUseCase.trackAddToCart(
                req.productId(), req.quantity() != null ? req.quantity() : 1,
                req.customerId(), req.sessionId());
        return ResponseEntity.ok(ApiResponse.ok(null, "Cart event tracked"));
    }
}
