package com.shopverse.infrastructure.kafka;

import com.shopverse.domain.event.OrderEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

/**
 * Ch12-01: Kafka consumer — processes order domain events.
 * Ch12-03: @RetryableTopic — automatic DLT (dead-letter-topic) on failure.
 */
@Component
public class OrderKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderKafkaConsumer.class);

    @RetryableTopic(
        attempts = "3",
        backoff = @Backoff(delay = 1000, multiplier = 2.0),
        dltTopicSuffix = ".DLT"
    )
    @KafkaListener(topics = "shopverse.orders", groupId = "shopverse-consumer",
                   containerFactory = "kafkaListenerContainerFactory")
    public void consume(ConsumerRecord<String, Object> record) {
        log.info("Consumed from {}/{}: key={}", record.topic(), record.partition(), record.key());
        // Route to appropriate handler based on event type
        if (record.value() instanceof OrderEvent event) {
            handleOrderEvent(event);
        }
    }

    private void handleOrderEvent(OrderEvent event) {
        switch (event) {
            case OrderEvent.OrderPlaced e    -> log.info("Processing OrderPlaced: {}", e.orderId());
            case OrderEvent.OrderShipped e   -> log.info("Processing OrderShipped: {} tracking={}", e.orderId(), e.trackingNumber());
            case OrderEvent.OrderCancelled e -> log.info("Processing OrderCancelled: {}", e.orderId());
            default                          -> log.debug("Unhandled event: {}", event.getClass().getSimpleName());
        }
    }

    @KafkaListener(topics = "shopverse.orders.DLT", groupId = "shopverse-dlt")
    public void consumeDlt(ConsumerRecord<String, Object> record) {
        log.error("DLT message received: key={}, value={}", record.key(), record.value());
        // Alert, store in DB for manual reprocessing
    }
}
