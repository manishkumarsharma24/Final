package com.shopverse.application.usecase.recommendation;

import com.shopverse.domain.model.Recommendation;
import com.shopverse.domain.port.RecommendationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Use case: get product recommendations from Neo4j graph.
 * Returns combined collaborative filtering + view-based recommendations.
 */
@Service
public class GetRecommendationsUseCase {

    private final RecommendationRepository recommendationRepository;

    public GetRecommendationsUseCase(RecommendationRepository recommendationRepository) {
        this.recommendationRepository = recommendationRepository;
    }

    public List<Recommendation> getRecommendations(Long productId) {
        return recommendationRepository.findRecommendations(productId);
    }

    public List<Recommendation> getByCategory(String category, Long excludeProductId) {
        return recommendationRepository.findTopRatedInCategory(category, excludeProductId);
    }
}
