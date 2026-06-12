package com.shopverse.infrastructure.kafka;

import com.shopverse.domain.event.OrderEvent;
import com.shopverse.domain.event.ProductEvent;
import com.shopverse.domain.port.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Ch12-01: Kafka producer — implements domain EventPublisher port.
 * Routes domain events to appropriate Kafka topics.
 * Also publishes ProductEvents as Spring application events so @EventListener beans
 * (e.g. ProductSyncService) receive them for internal processing (e.g. ES indexing).
 */
@Component
public class OrderKafkaProducer implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderKafkaProducer.class);

    private static final String ORDERS_TOPIC   = "shopverse.orders";
    private static final String PRODUCTS_TOPIC = "shopverse.products";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ApplicationEventPublisher springEventPublisher;

    public OrderKafkaProducer(KafkaTemplate<String, Object> kafkaTemplate,
                              ApplicationEventPublisher springEventPublisher) {
        this.kafkaTemplate        = kafkaTemplate;
        this.springEventPublisher = springEventPublisher;
    }

    @Override
    public void publish(Object event) {
        // ProductEvents: publish as Spring application events for internal listeners (ES sync)
        // and also send to Kafka for external/async consumers
        if (event instanceof ProductEvent) {
            springEventPublisher.publishEvent(event);
        }

        String topic = resolveTopic(event);
        String key   = resolveKey(event);

        CompletableFuture<SendResult<String, Object>> future =
            kafkaTemplate.send(topic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish event {} to {}: {}", event.getClass().getSimpleName(), topic, ex.getMessage());
            } else {
                log.debug("Published {} to {}@{}", event.getClass().getSimpleName(), topic,
                          result.getRecordMetadata().offset());
            }
        });
    }

    private String resolveTopic(Object event) {
        return switch (event) {
            case OrderEvent e   -> ORDERS_TOPIC;
            case ProductEvent e -> PRODUCTS_TOPIC;
            default             -> "shopverse.events";
        };
    }

    private String resolveKey(Object event) {
        return switch (event) {
            case OrderEvent e   -> e.orderId().toString();
            case ProductEvent e -> e.productId().toString();
            default             -> "unknown";
        };
    }
}
