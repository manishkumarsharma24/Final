package com.shopverse.infrastructure.kafka;

import com.shopverse.domain.event.AnalyticsEvent;
import com.shopverse.domain.port.RecommendationRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Kafka consumer for shopverse.analytics topic.
 *
 * Scenarios:
 *   ProductViewed      → upsert ProductNode in Neo4j
 *                      → read previous product for this session from Redis
 *                      → write VIEWED_AFTER relationship in Neo4j
 *                      → store current productId as last-viewed in Redis (TTL 30 min)
 *   ProductSearched    → log search analytics (future: ML pipeline)
 *   ProductAddedToCart → funnel analytics
 *   CheckoutStarted    → funnel analytics
 *   OrderConverted     → conversion rate tracking
 *
 * Neo4j VIEWED_AFTER pattern:
 *   Each session's last-viewed product is stored in Redis under key:
 *     neo4j:session:{sessionId}:lastViewed  →  productId (String, TTL 30 min)
 *
 *   When a new ProductViewed event arrives:
 *     1. Read previous productId from Redis for this session
 *     2. Create VIEWED_AFTER edge from previous → current
 *     3. Write current productId back to Redis (refreshing TTL)
 *
 *   Example — session "sess-abc":
 *     User views Product 1  → Redis: lastViewed=1,  no edge (no previous)
 *     User views Product 3  → Redis: lastViewed=3,  (1)-[:VIEWED_AFTER]->(3)
 *     User views Product 7  → Redis: lastViewed=7,  (3)-[:VIEWED_AFTER]->(7)
 *     30 min idle           → Redis key expires, session chain resets
 */
@Component
public class AnalyticsKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsKafkaConsumer.class);

    /** Redis key for tracking last-viewed product per browsing session. */
    private static final String LAST_VIEWED_KEY = "neo4j:session:%s:lastViewed";

    /** After 30 min of inactivity the VIEWED_AFTER chain resets for the session. */
    private static final Duration SESSION_TTL = Duration.ofMinutes(30);

    private final RecommendationRepository recommendationRepository;
    private final StringRedisTemplate      redisTemplate;

    public AnalyticsKafkaConsumer(RecommendationRepository recommendationRepository,
                                  StringRedisTemplate redisTemplate) {
        this.recommendationRepository = recommendationRepository;
        this.redisTemplate            = redisTemplate;
    }

    @RetryableTopic(
        attempts = "3",
        backoff = @Backoff(delay = 500, multiplier = 2.0),
        dltTopicSuffix = ".DLT"
    )
    @KafkaListener(topics = KafkaTopicsConfig.ANALYTICS_TOPIC,
                   groupId = "shopverse-analytics-consumer",
                   containerFactory = "kafkaListenerContainerFactory")
    public void consume(ConsumerRecord<String, Object> record) {
        if (record.value() instanceof AnalyticsEvent event) {
            handleAnalyticsEvent(event);
        }
    }

    private void handleAnalyticsEvent(AnalyticsEvent event) {
        switch (event) {
            case AnalyticsEvent.ProductViewed e      -> handleProductViewed(e);
            case AnalyticsEvent.ProductSearched e    ->
                log.info("ProductSearched: query='{}' results={} session={}",
                        e.query(), e.resultsCount(), e.sessionId());
            case AnalyticsEvent.ProductAddedToCart e ->
                log.info("AddedToCart: productId={} qty={} session={}",
                        e.productId(), e.quantity(), e.sessionId());
            case AnalyticsEvent.CheckoutStarted e    ->
                log.info("CheckoutStarted: customer={} total={} items={} session={}",
                        e.customerId(), e.cartTotal(), e.itemCount(), e.sessionId());
            case AnalyticsEvent.OrderConverted e     ->
                log.info("OrderConverted: orderId={} customer={} total={} session={}",
                        e.orderId(), e.customerId(), e.total(), e.sessionId());
        }
    }

    /**
     * Handles ProductViewed — the main Neo4j write path for browsing behaviour.
     *
     * Step 1: Upsert ProductNode.
     *   MERGE (p:Product {productId: $id})
     *   SET p.name = $name, p.category = $category
     *   Every product that is ever viewed gets a node in the graph.
     *   MERGE is idempotent so repeated views don't create duplicates.
     *
     * Step 2: Read previous product from Redis and write VIEWED_AFTER.
     *   Redis key: neo4j:session:{sessionId}:lastViewed
     *   If key exists and value != current productId:
     *     MERGE (prev)-[r:VIEWED_AFTER]->(curr)
     *     ON MATCH SET r.count = r.count + 1
     *   This builds a navigation graph: which product leads to which.
     *
     * Step 3: Refresh Redis key with current productId (TTL reset to 30 min).
     *   The TTL means a session that goes idle for 30 min resets its chain —
     *   the next view starts a fresh navigation sequence.
     */
    private void handleProductViewed(AnalyticsEvent.ProductViewed e) {
        log.info("ProductViewed → Neo4j: productId={} name='{}' category={} session={}",
                e.productId(), e.productName(), e.category(), e.sessionId());

        // Step 1: ensure ProductNode exists before creating any relationship
        recommendationRepository.upsertProductNode(
                e.productId(),
                e.productName(),
                e.category(),
                0.0   // avgRating is updated separately via review aggregation
        );

        // Step 2: create VIEWED_AFTER edge if a previous product exists in this session
        String redisKey = String.format(LAST_VIEWED_KEY, e.sessionId());
        String previousProductIdStr = redisTemplate.opsForValue().get(redisKey);

        if (previousProductIdStr != null) {
            try {
                Long previousProductId = Long.parseLong(previousProductIdStr);

                if (!previousProductId.equals(e.productId())) {
                    // User navigated: previousProduct → currentProduct
                    // Neo4j: (Product:prevId)-[:VIEWED_AFTER {count++, sessionId}]->(Product:currId)
                    recommendationRepository.recordViewedAfter(
                            previousProductId, e.productId(), e.sessionId());

                    log.debug("Neo4j VIEWED_AFTER written: {} → {} (session={})",
                            previousProductId, e.productId(), e.sessionId());
                }
            } catch (NumberFormatException ex) {
                log.warn("Corrupt lastViewed value in Redis for session {}: '{}'",
                        e.sessionId(), previousProductIdStr);
            }
        }

        // Step 3: overwrite Redis key — current product is now the session's last-viewed
        // SET neo4j:session:{sessionId}:lastViewed {productId} EX 1800
        redisTemplate.opsForValue().set(redisKey, e.productId().toString(), SESSION_TTL);
    }
}
