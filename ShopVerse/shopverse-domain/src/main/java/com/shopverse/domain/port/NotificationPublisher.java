package com.shopverse.domain.port;

import com.shopverse.domain.event.NotificationEvent;

/**
 * Domain port for publishing notifications.
 * Implementations: RabbitMQ publisher (primary), Kafka fallback.
 */
public interface NotificationPublisher {
    void publish(NotificationEvent event);
}
