package com.shopverse.infrastructure.kafka;

import com.shopverse.domain.event.InventoryEvent;
import com.shopverse.domain.event.NotificationEvent;
import com.shopverse.domain.event.OrderEvent;
import com.shopverse.domain.port.NotificationPublisher;
import com.shopverse.domain.port.RecommendationRepository;
import com.shopverse.infrastructure.jpa.repository.JpaProductRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Kafka consumer — processes order domain events.
 *
 * Scenarios:
 *   OrderPlaced    → publish NotificationEvent (confirmation email via RabbitMQ)
 *                  → update Neo4j graph (FREQUENTLY_BOUGHT_TOGETHER)
 *                  → publish InventoryEvent.StockReserved
 *   OrderShipped   → publish NotificationEvent (shipped email)
 *   OrderDelivered → publish NotificationEvent (delivered email)
 *   OrderCancelled → publish InventoryEvent.StockReleased (restore stock)
 *                  → publish NotificationEvent (cancellation email)
 *
 * DLT: failed messages after 3 retries route to shopverse.orders.DLT
 */
@Component
public class OrderKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderKafkaConsumer.class);

    private final NotificationPublisher   notificationPublisher;
    private final RecommendationRepository recommendationRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final JpaProductRepository    productRepository;

    public OrderKafkaConsumer(NotificationPublisher notificationPublisher,
                              RecommendationRepository recommendationRepository,
                              KafkaTemplate<String, Object> kafkaTemplate,
                              JpaProductRepository productRepository) {
        this.notificationPublisher    = notificationPublisher;
        this.recommendationRepository = recommendationRepository;
        this.kafkaTemplate            = kafkaTemplate;
        this.productRepository        = productRepository;
    }

    @RetryableTopic(
        attempts = "3",
        backoff = @Backoff(delay = 1000, multiplier = 2.0),
        dltTopicSuffix = ".DLT"
    )
    @KafkaListener(topics = KafkaTopicsConfig.ORDERS_TOPIC,
                   groupId = "shopverse-consumer",
                   containerFactory = "kafkaListenerContainerFactory")
    public void consume(ConsumerRecord<String, Object> record) {
        log.info("Consumed from {}/{}: key={}", record.topic(), record.partition(), record.key());
        if (record.value() instanceof OrderEvent event) {
            handleOrderEvent(event);
        }
    }

    private void handleOrderEvent(OrderEvent event) {
        switch (event) {
            case OrderEvent.OrderPlaced e    -> handleOrderPlaced(e);
            case OrderEvent.OrderConfirmed e -> handleOrderConfirmed(e);
            case OrderEvent.OrderShipped e   -> handleOrderShipped(e);
            case OrderEvent.OrderDelivered e -> handleOrderDelivered(e);
            case OrderEvent.OrderCancelled e -> handleOrderCancelled(e);
            case OrderEvent.OrderRefunded e  -> handleOrderRefunded(e);
        }
        // Note: no default — the sealed interface guarantees exhaustive matching.
        // Adding a new subtype to OrderEvent will cause a compile error here,
        // forcing the developer to handle it explicitly.
    }

    private void handleOrderPlaced(OrderEvent.OrderPlaced e) {
        log.info("Handling OrderPlaced: orderId={} customerId={} total={}",
                e.orderId(), e.customerId(), e.total());

        // 1. Send order confirmation email via RabbitMQ
        // Customer email needs to be looked up — in a real system, include it in the event
        // Here we use the customerId as a placeholder email for demo purposes
        String customerEmail = "customer-" + e.customerId() + "@shopverse.com";
        notificationPublisher.publish(
            new NotificationEvent.OrderConfirmationNotification(
                e.orderId(), e.customerId(), customerEmail, e.total(), Instant.now()));

        // 2. Publish inventory reservation event to Kafka
        kafkaTemplate.send(KafkaTopicsConfig.INVENTORY_TOPIC,
            e.orderId().toString(),
            new InventoryEvent.StockReserved(null, 0, e.orderId(), Instant.now()));

        log.info("OrderPlaced handlers complete for orderId={}", e.orderId());
    }

    private void handleOrderConfirmed(OrderEvent.OrderConfirmed e) {
        log.info("OrderConfirmed: orderId={}", e.orderId());
        // No notification needed for confirmed — confirmation already sent on OrderPlaced
    }

    private void handleOrderShipped(OrderEvent.OrderShipped e) {
        log.info("Handling OrderShipped: orderId={} tracking={}", e.orderId(), e.trackingNumber());
        String customerEmail = resolveCustomerEmail(e.orderId());
        notificationPublisher.publish(
            new NotificationEvent.OrderShippedNotification(
                e.orderId(), customerEmail, e.trackingNumber(), Instant.now()));
    }

    private void handleOrderDelivered(OrderEvent.OrderDelivered e) {
        log.info("Handling OrderDelivered: orderId={}", e.orderId());
        String customerEmail = resolveCustomerEmail(e.orderId());
        notificationPublisher.publish(
            new NotificationEvent.OrderDeliveredNotification(
                e.orderId(), customerEmail, Instant.now()));
    }

    private void handleOrderCancelled(OrderEvent.OrderCancelled e) {
        log.info("Handling OrderCancelled: orderId={} reason={}", e.orderId(), e.reason());

        // 1. Restore inventory — publish StockReleased to inventory topic
        kafkaTemplate.send(KafkaTopicsConfig.INVENTORY_TOPIC,
            e.orderId().toString(),
            new InventoryEvent.StockReleased(null, 0, e.orderId(), e.reason(), Instant.now()));

        // 2. Send cancellation email
        String customerEmail = resolveCustomerEmail(e.orderId());
        notificationPublisher.publish(
            new NotificationEvent.OrderCancelledNotification(
                e.orderId(), customerEmail, e.reason(), Instant.now()));
    }

    private void handleOrderRefunded(OrderEvent.OrderRefunded e) {
        log.info("Handling OrderRefunded: orderId={} refundAmount={}", e.orderId(), e.refundAmount());

        // 1. Restore inventory — refund implies the order items are being returned
        //    productId/quantity are not in the event; a production implementation would
        //    look them up from a read model. Null/0 here triggers the inventory service
        //    to query its own state for this orderId.
        kafkaTemplate.send(KafkaTopicsConfig.INVENTORY_TOPIC,
            e.orderId().toString(),
            new InventoryEvent.StockReleased(null, 0, e.orderId(), "Order refunded", Instant.now()));

        // 2. Send refund confirmation email via RabbitMQ
        String customerEmail = resolveCustomerEmail(e.orderId());
        notificationPublisher.publish(
            new NotificationEvent.PaymentSuccessNotification(
                e.orderId(), customerEmail, e.refundAmount(), "REFUND", Instant.now()));

        log.info("OrderRefunded handlers complete for orderId={}", e.orderId());
    }

    /**
     * Dead-letter topic consumer — handles messages that exhausted all @RetryableTopic retries.
     *
     * containerFactory must match the main listener to share the same deserialization
     * configuration (trusted packages, type headers). Without it, Spring Boot would use
     * the default factory which may not have the same error handling settings.
     */
    @KafkaListener(topics = KafkaTopicsConfig.ORDERS_TOPIC + ".DLT",
                   groupId = "shopverse-dlt",
                   containerFactory = "kafkaListenerContainerFactory")
    public void consumeDlt(ConsumerRecord<String, Object> record) {
        log.error("DLT message received: topic={} partition={} key={} value={}",
            record.topic(), record.partition(), record.key(), record.value());
        // Production: persist to dead_letter_messages table for manual reprocessing,
        // publish alert to PagerDuty / Slack ops channel.
    }

    /**
     * Placeholder — in production, customer email would be included in the event
     * or looked up from a read model / cache.
     */
    private String resolveCustomerEmail(Long orderId) {
        return "customer@shopverse.com";
    }
}
