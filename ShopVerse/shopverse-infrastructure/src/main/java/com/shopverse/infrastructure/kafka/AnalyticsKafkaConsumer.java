package com.shopverse.infrastructure.kafka;

import com.shopverse.domain.event.AnalyticsEvent;
import com.shopverse.domain.port.RecommendationRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for shopverse.analytics topic.
 *
 * Scenarios:
 *   ProductViewed      → update Neo4j VIEWED_AFTER relationship
 *   ProductSearched    → log search analytics (future: ML pipeline)
 *   ProductAddedToCart → funnel analytics
 *   CheckoutStarted    → funnel analytics
 *   OrderConverted     → conversion rate tracking
 */
@Component
public class AnalyticsKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsKafkaConsumer.class);

    private final RecommendationRepository recommendationRepository;

    public AnalyticsKafkaConsumer(RecommendationRepository recommendationRepository) {
        this.recommendationRepository = recommendationRepository;
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
            case AnalyticsEvent.ProductViewed e -> {
                log.info("ProductViewed: productId={} customer={} session={}",
                        e.productId(), e.customerId(), e.sessionId());
                // In production: update a Redis sorted set for real-time trending
                // and/or write to a time-series store for dashboard analytics
            }
            case AnalyticsEvent.ProductSearched e -> {
                log.info("ProductSearched: query='{}' results={} session={}",
                        e.query(), e.resultsCount(), e.sessionId());
            }
            case AnalyticsEvent.ProductAddedToCart e -> {
                log.info("AddedToCart: productId={} qty={} session={}",
                        e.productId(), e.quantity(), e.sessionId());
            }
            case AnalyticsEvent.CheckoutStarted e -> {
                log.info("CheckoutStarted: customer={} total={} items={} session={}",
                        e.customerId(), e.cartTotal(), e.itemCount(), e.sessionId());
            }
            case AnalyticsEvent.OrderConverted e -> {
                log.info("OrderConverted: orderId={} customer={} total={} session={}",
                        e.orderId(), e.customerId(), e.total(), e.sessionId());
            }
        }
    }
}
