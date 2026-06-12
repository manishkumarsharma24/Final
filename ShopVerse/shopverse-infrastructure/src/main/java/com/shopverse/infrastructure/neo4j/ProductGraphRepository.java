package com.shopverse.infrastructure.neo4j;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data Neo4j repository — Cypher queries for the product recommendation graph.
 *
 * Graph schema:
 *   (Product)-[:FREQUENTLY_BOUGHT_TOGETHER {orderId, count}]->(Product)
 *   (Product)-[:VIEWED_AFTER {sessionId, count}]->(Product)
 *   (Product)-[:IN_CATEGORY]->(Category)
 */
public interface ProductGraphRepository extends Neo4jRepository<ProductNode, Long> {

    Optional<ProductNode> findByProductId(Long productId);

    // ── Recommendations ────────────────────────────────────────────────────────

    /** 2-hop collaborative filtering — "customers who bought A also bought..." */
    @Query("""
        MATCH (p:Product {productId: $productId})-[:FREQUENTLY_BOUGHT_TOGETHER*1..2]-(rec:Product)
        WHERE rec.productId <> $productId
        RETURN DISTINCT rec
        ORDER BY rec.avgRating DESC
        LIMIT 10
        """)
    List<ProductNode> findRecommendations(@Param("productId") Long productId);

    /** Category affinity — top-rated products in same category. */
    @Query("""
        MATCH (p:Product)
        WHERE p.category = $category AND p.productId <> $excludeId
        RETURN p ORDER BY p.avgRating DESC LIMIT 5
        """)
    List<ProductNode> findTopRatedInCategory(@Param("category") String category,
                                             @Param("excludeId") Long excludeId);

    /** VIEWED_AFTER recommendations — "people who viewed X also viewed..." */
    @Query("""
        MATCH (p:Product {productId: $productId})-[:VIEWED_AFTER]->(rec:Product)
        WHERE rec.productId <> $productId
        RETURN DISTINCT rec
        ORDER BY rec.avgRating DESC
        LIMIT 5
        """)
    List<ProductNode> findViewedAfterRecommendations(@Param("productId") Long productId);

    /** Combined recommendation — merge collaborative + view-based, ranked by rating. */
    @Query("""
        MATCH (p:Product {productId: $productId})
        OPTIONAL MATCH (p)-[:FREQUENTLY_BOUGHT_TOGETHER*1..2]-(bought:Product)
        WHERE bought.productId <> $productId
        OPTIONAL MATCH (p)-[:VIEWED_AFTER]->(viewed:Product)
        WHERE viewed.productId <> $productId
        WITH collect(DISTINCT bought) + collect(DISTINCT viewed) AS recs
        UNWIND recs AS rec
        RETURN DISTINCT rec
        ORDER BY rec.avgRating DESC
        LIMIT 10
        """)
    List<ProductNode> findCombinedRecommendations(@Param("productId") Long productId);

    // ── Graph mutations ────────────────────────────────────────────────────────

    /** Upsert product node — MERGE ensures idempotency. */
    @Query("""
        MERGE (p:Product {productId: $productId})
        SET p.name = $name, p.category = $category, p.avgRating = $avgRating
        RETURN p
        """)
    void upsertProduct(@Param("productId") Long productId,
                       @Param("name") String name,
                       @Param("category") String category,
                       @Param("avgRating") double avgRating);

    /**
     * Create or strengthen FREQUENTLY_BOUGHT_TOGETHER relationship.
     * MERGE prevents duplicates; ON MATCH increments co-purchase count.
     */
    @Query("""
        MATCH (a:Product {productId: $productId1}), (b:Product {productId: $productId2})
        MERGE (a)-[r:FREQUENTLY_BOUGHT_TOGETHER]-(b)
        ON CREATE SET r.count = 1, r.orderId = $orderId
        ON MATCH  SET r.count = r.count + 1
        """)
    void createOrIncrementBoughtTogether(@Param("productId1") Long productId1,
                                         @Param("productId2") Long productId2,
                                         @Param("orderId") Long orderId);

    /**
     * Create or strengthen VIEWED_AFTER relationship.
     * Tracks session-level navigation patterns.
     */
    @Query("""
        MATCH (a:Product {productId: $fromProductId}), (b:Product {productId: $toProductId})
        MERGE (a)-[r:VIEWED_AFTER]->(b)
        ON CREATE SET r.count = 1, r.sessionId = $sessionId
        ON MATCH  SET r.count = r.count + 1
        """)
    void createOrIncrementViewedAfter(@Param("fromProductId") Long fromProductId,
                                      @Param("toProductId") Long toProductId,
                                      @Param("sessionId") String sessionId);

    /** Trending products — most frequently bought in last N orders. */
    @Query("""
        MATCH (p:Product)-[r:FREQUENTLY_BOUGHT_TOGETHER]-()
        RETURN p ORDER BY r.count DESC LIMIT $limit
        """)
    List<ProductNode> findTrendingProducts(@Param("limit") int limit);
}
