package com.shopverse.infrastructure.neo4j;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Ch06-06: Spring Data Neo4j repository — Cypher queries for recommendations.
 */
public interface ProductGraphRepository extends Neo4jRepository<ProductNode, Long> {

    Optional<ProductNode> findByProductId(Long productId);

    /** Ch06-06: 2-hop collaborative filtering — "customers who bought A also bought..." */
    @Query("""
        MATCH (p:Product {productId: $productId})-[:FREQUENTLY_BOUGHT_TOGETHER*1..2]-(rec:Product)
        WHERE rec.productId <> $productId
        RETURN DISTINCT rec
        ORDER BY rec.avgRating DESC
        LIMIT 10
        """)
    List<ProductNode> findRecommendations(@Param("productId") Long productId);

    /** Ch06-06: Category affinity — products in same category, sorted by rating. */
    @Query("""
        MATCH (p:Product)
        WHERE p.category = $category AND p.productId <> $excludeId
        RETURN p ORDER BY p.avgRating DESC LIMIT 5
        """)
    List<ProductNode> findTopRatedInCategory(@Param("category") String category,
                                             @Param("excludeId") Long excludeId);
}
