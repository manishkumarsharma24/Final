package com.shopverse.infrastructure.neo4j;

import com.shopverse.domain.model.Recommendation;
import com.shopverse.domain.port.RecommendationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Infrastructure adapter — implements domain RecommendationRepository port using Neo4j.
 * Converts ProductNode graph entities to domain Recommendation models.
 */
@Component
public class Neo4jRecommendationAdapter implements RecommendationRepository {

    private static final Logger log = LoggerFactory.getLogger(Neo4jRecommendationAdapter.class);

    private final ProductGraphRepository graphRepository;

    public Neo4jRecommendationAdapter(ProductGraphRepository graphRepository) {
        this.graphRepository = graphRepository;
    }

    @Override
    public List<Recommendation> findRecommendations(Long productId) {
        return graphRepository.findCombinedRecommendations(productId)
                .stream()
                .map(node -> toRecommendation(node, "Frequently bought together"))
                .collect(Collectors.toList());
    }

    @Override
    public List<Recommendation> findTopRatedInCategory(String category, Long excludeProductId) {
        return graphRepository.findTopRatedInCategory(category, excludeProductId)
                .stream()
                .map(node -> toRecommendation(node, "Top rated in " + category))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional("neo4jTransactionManager")
    public void recordPurchasedTogether(Long productId1, Long productId2, Long orderId) {
        try {
            graphRepository.createOrIncrementBoughtTogether(productId1, productId2, orderId);
            log.debug("Recorded FREQUENTLY_BOUGHT_TOGETHER: {} <-> {} orderId={}",
                    productId1, productId2, orderId);
        } catch (Exception ex) {
            log.warn("Failed to record purchase graph relationship: {}", ex.getMessage());
        }
    }

    @Override
    @Transactional("neo4jTransactionManager")
    public void recordViewedAfter(Long fromProductId, Long toProductId, String sessionId) {
        try {
            graphRepository.createOrIncrementViewedAfter(fromProductId, toProductId, sessionId);
            log.debug("Recorded VIEWED_AFTER: {} -> {} session={}", fromProductId, toProductId, sessionId);
        } catch (Exception ex) {
            log.warn("Failed to record view graph relationship: {}", ex.getMessage());
        }
    }

    @Override
    @Transactional("neo4jTransactionManager")
    public void upsertProductNode(Long productId, String name, String category, double avgRating) {
        try {
            graphRepository.upsertProduct(productId, name, category, avgRating);
            log.debug("Upserted ProductNode: productId={}", productId);
        } catch (Exception ex) {
            log.warn("Failed to upsert product node in Neo4j: {}", ex.getMessage());
        }
    }

    private Recommendation toRecommendation(ProductNode node, String reason) {
        return new Recommendation(
                node.getProductId(),
                node.getName(),
                node.getCategory(),
                node.getAvgRating(),
                reason);
    }
}
