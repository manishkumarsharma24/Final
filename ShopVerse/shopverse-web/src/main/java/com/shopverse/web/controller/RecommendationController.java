package com.shopverse.web.controller;

import com.shopverse.application.usecase.recommendation.GetRecommendationsUseCase;
import com.shopverse.application.usecase.recommendation.TrackProductInteractionUseCase;
import com.shopverse.shared.ApiResponse;
import com.shopverse.web.dto.RecommendationResponse;
import com.shopverse.web.dto.TrackViewRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Neo4j recommendation endpoints.
 *
 * GET  /api/recommendations/{productId}              — combined collaborative + view-based
 * GET  /api/recommendations/category/{category}      — top-rated in category
 * POST /api/recommendations/track/view               — record product view (VIEWED_AFTER)
 * POST /api/recommendations/track/purchase           — record purchase pair (FREQUENTLY_BOUGHT_TOGETHER)
 */
@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final GetRecommendationsUseCase      getRecommendationsUseCase;
    private final TrackProductInteractionUseCase trackInteractionUseCase;

    public RecommendationController(GetRecommendationsUseCase getRecommendationsUseCase,
                                    TrackProductInteractionUseCase trackInteractionUseCase) {
        this.getRecommendationsUseCase = getRecommendationsUseCase;
        this.trackInteractionUseCase   = trackInteractionUseCase;
    }

    /**
     * GET /api/recommendations/{productId}
     * Returns up to 10 recommendations combining FREQUENTLY_BOUGHT_TOGETHER
     * and VIEWED_AFTER graph traversals, ranked by avgRating.
     */
    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<List<RecommendationResponse>>> getRecommendations(
            @PathVariable Long productId) {
        List<RecommendationResponse> recs = getRecommendationsUseCase
                .getRecommendations(productId)
                .stream()
                .map(RecommendationResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(recs, "Recommendations retrieved"));
    }

    /**
     * GET /api/recommendations/category/{category}?excludeId={id}
     * Returns top-rated products in the same category.
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse<List<RecommendationResponse>>> getByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") Long excludeId) {
        List<RecommendationResponse> recs = getRecommendationsUseCase
                .getByCategory(category, excludeId)
                .stream()
                .map(RecommendationResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(recs, "Category recommendations retrieved"));
    }

    /**
     * POST /api/recommendations/track/view
     * Called by the frontend when a product detail page is viewed.
     * Records VIEWED_AFTER relationship and publishes analytics event.
     *
     * Body: { productId, productName, category, customerId, sessionId, previousProductId }
     */
    @PostMapping("/track/view")
    public ResponseEntity<ApiResponse<Void>> trackView(
            @Valid @RequestBody TrackViewRequest req) {
        trackInteractionUseCase.trackProductView(
                req.productId(), req.productName(), req.category(),
                req.customerId(), req.sessionId(), req.previousProductId());
        return ResponseEntity.ok(ApiResponse.ok(null, "View tracked"));
    }

    /**
     * POST /api/recommendations/track/purchase
     * Called after an order is placed to update the graph.
     * Body: { orderId, productIds[], productNames[], productCategories[], customerId, sessionId }
     */
    @PostMapping("/track/purchase")
    public ResponseEntity<ApiResponse<Void>> trackPurchase(
            @RequestBody TrackPurchaseRequest req) {
        trackInteractionUseCase.trackOrderPurchase(
                req.orderId(), req.productIds(), req.productNames(),
                req.productCategories(), req.customerId(), req.sessionId());
        return ResponseEntity.ok(ApiResponse.ok(null, "Purchase graph updated"));
    }

    /** Inline record for purchase tracking request. */
    public record TrackPurchaseRequest(
            Long orderId,
            List<Long> productIds,
            List<String> productNames,
            List<String> productCategories,
            Long customerId,
            String sessionId) {}
}
