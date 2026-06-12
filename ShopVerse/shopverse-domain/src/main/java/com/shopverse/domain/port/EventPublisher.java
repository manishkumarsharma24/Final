package com.shopverse.domain.port;

/**
 * Ch14-01: Domain event publisher port — infrastructure adapter publishes to Kafka.
 */
public interface EventPublisher {
    void publish(Object event);
}
