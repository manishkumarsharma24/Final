package com.shopverse.infrastructure.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declarative Kafka topic creation.
 * Spring Kafka auto-creates these topics on startup if they don't exist.
 */
@Configuration
public class KafkaTopicsConfig {

    // ── Topic name constants ───────────────────────────────────────────────────
    public static final String ORDERS_TOPIC        = "shopverse.orders";
    public static final String PRODUCTS_TOPIC      = "shopverse.products";
    public static final String NOTIFICATIONS_TOPIC = "shopverse.notifications";
    public static final String INVENTORY_TOPIC     = "shopverse.inventory";
    public static final String ANALYTICS_TOPIC     = "shopverse.analytics";

    @Bean
    public NewTopic ordersTopic() {
        return TopicBuilder.name(ORDERS_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic productsTopic() {
        return TopicBuilder.name(PRODUCTS_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic notificationsTopic() {
        return TopicBuilder.name(NOTIFICATIONS_TOPIC)
                .partitions(2)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic inventoryTopic() {
        return TopicBuilder.name(INVENTORY_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic analyticsTopic() {
        // High-volume — more partitions for parallelism
        return TopicBuilder.name(ANALYTICS_TOPIC)
                .partitions(6)
                .replicas(1)
                .build();
    }
}
