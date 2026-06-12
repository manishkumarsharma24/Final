package com.shopverse.web.dto;

import com.shopverse.domain.model.Recommendation;

public record RecommendationResponse(
        Long productId,
        String name,
        String category,
        double avgRating,
        String reason) {

    public static RecommendationResponse from(Recommendation r) {
        return new RecommendationResponse(
                r.getProductId(), r.getName(), r.getCategory(),
                r.getAvgRating(), r.getReason());
    }
}
