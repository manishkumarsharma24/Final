package com.shopverse.infrastructure.kafka;

import com.shopverse.domain.event.*;
import com.shopverse.domain.port.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka producer — implements domain EventPublisher port.
 * Routes domain events to appropriate Kafka topics.
 *
 * Topics:
 *   shopverse.orders        → OrderEvent
 *   shopverse.products      → ProductEvent (also fires Spring app event for ES sync)
 *   shopverse.notifications → NotificationEvent
 *   shopverse.inventory     → InventoryEvent
 *   shopverse.analytics     → AnalyticsEvent
 */
@Component
public class OrderKafkaProducer implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderKafkaProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ApplicationEventPublisher springEventPublisher;

    public OrderKafkaProducer(KafkaTemplate<String, Object> kafkaTemplate,
                              ApplicationEventPublisher springEventPublisher) {
        this.kafkaTemplate        = kafkaTemplate;
        this.springEventPublisher = springEventPublisher;
    }

    @Override
    public void publish(Object event) {
        // ProductEvents also fire as Spring application events for internal listeners (ES sync)
        if (event instanceof ProductEvent) {
            springEventPublisher.publishEvent(event);
        }

        String topic = resolveTopic(event);
        String key   = resolveKey(event);

        CompletableFuture<SendResult<String, Object>> future =
            kafkaTemplate.send(topic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish {} to {}: {}",
                        event.getClass().getSimpleName(), topic, ex.getMessage());
            } else {
                log.debug("Published {} to {}@{}", event.getClass().getSimpleName(),
                        topic, result.getRecordMetadata().offset());
            }
        });
    }

    private String resolveTopic(Object event) {
        return switch (event) {
            case OrderEvent e        -> KafkaTopicsConfig.ORDERS_TOPIC;
            case ProductEvent e      -> KafkaTopicsConfig.PRODUCTS_TOPIC;
            case NotificationEvent e -> KafkaTopicsConfig.NOTIFICATIONS_TOPIC;
            case InventoryEvent e    -> KafkaTopicsConfig.INVENTORY_TOPIC;
            case AnalyticsEvent e    -> KafkaTopicsConfig.ANALYTICS_TOPIC;
            default                  -> "shopverse.events";
        };
    }

    private String resolveKey(Object event) {
        return switch (event) {
            case OrderEvent e        -> e.orderId().toString();
            case ProductEvent e      -> e.productId().toString();
            case NotificationEvent e -> e.orderId().toString();
            case InventoryEvent e    -> e.productId().toString();
            case AnalyticsEvent e    -> e.sessionId();
            default                  -> "unknown";
        };
    }
}
