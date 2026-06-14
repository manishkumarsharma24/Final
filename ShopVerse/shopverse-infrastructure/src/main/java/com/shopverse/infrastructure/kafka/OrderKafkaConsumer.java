package com.shopverse.infrastructure.kafka;

import com.shopverse.domain.event.InventoryEvent;
import com.shopverse.domain.event.NotificationEvent;
import com.shopverse.domain.event.OrderEvent;
import com.shopverse.domain.port.NotificationPublisher;
import com.shopverse.domain.port.RecommendationRepository;
import com.shopverse.infrastructure.jpa.entity.OrderItemEntity;
import com.shopverse.infrastructure.jpa.repository.JpaOrderRepository;
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
import java.util.List;

/**
 * Kafka consumer — processes order domain events.
 *
 * Scenarios:
 *   OrderPlaced    → send confirmation email via RabbitMQ
 *                  → update Neo4j graph (FREQUENTLY_BOUGHT_TOGETHER for every product pair)
 *                  → publish InventoryEvent.StockReserved to Kafka
 *   OrderShipped   → send shipped email via RabbitMQ
 *   OrderDelivered → send delivered email via RabbitMQ
 *   OrderCancelled → publish InventoryEvent.StockReleased to Kafka
 *                  → send cancellation email via RabbitMQ
 *   OrderRefunded  → publish InventoryEvent.StockReleased to Kafka
 *                  → send refund confirmation email via RabbitMQ
 *
 * DLT: failed messages after 3 retries route to shopverse.orders.DLT
 */
@Component
public class OrderKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderKafkaConsumer.class);

    private final NotificationPublisher      notificationPublisher;
    private final RecommendationRepository   recommendationRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final JpaOrderRepository         orderRepository;
    private final JpaProductRepository       productRepository;

    public OrderKafkaConsumer(NotificationPublisher notificationPublisher,
                              RecommendationRepository recommendationRepository,
                              KafkaTemplate<String, Object> kafkaTemplate,
                              JpaOrderRepository orderRepository,
                              JpaProductRepository productRepository) {
        this.notificationPublisher    = notificationPublisher;
        this.recommendationRepository = recommendationRepository;
        this.kafkaTemplate            = kafkaTemplate;
        this.orderRepository          = orderRepository;
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
        // Sealed interface — exhaustive. Adding a new OrderEvent subtype causes a compile error here,
        // forcing explicit handling before the code compiles.
    }

    /**
     * Handles OrderPlaced — the busiest handler.
     *
     * 1. Confirmation email → RabbitMQ
     *    NotificationRabbitPublisher routes this to shopverse.queue.email via
     *    routing key "notification.email.order.confirmed".
     *
     * 2. Neo4j FREQUENTLY_BOUGHT_TOGETHER graph update
     *    Look up the saved order from PostgreSQL (it was committed before this
     *    Kafka event was published, so the row is guaranteed to exist).
     *
     *    For each pair of products in the order:
     *      MERGE (a:Product {productId: $p1})-[r:FREQUENTLY_BOUGHT_TOGETHER]-(b:Product {productId: $p2})
     *      ON CREATE SET r.count = 1, r.orderId = $orderId
     *      ON MATCH  SET r.count = r.count + 1
     *
     *    Example — order #155 contains Laptop (553) + Speaker (10) + Charger (6):
     *      Pairs: (553,10), (553,6), (10,6)
     *      Neo4j gets 3 FREQUENTLY_BOUGHT_TOGETHER edges (or count++ if they exist).
     *      Next time a user views Laptop, the recommendation query traverses these
     *      edges and suggests Speaker and Charger.
     *
     * 3. StockReserved → Kafka shopverse.inventory
     *    Triggers the inventory consumer to audit the stock reduction.
     */
    private void handleOrderPlaced(OrderEvent.OrderPlaced e) {
        log.info("Handling OrderPlaced: orderId={} customerId={} total={}",
                e.orderId(), e.customerId(), e.total());

        // 1. Confirmation email via RabbitMQ
        String customerEmail = "customer-" + e.customerId() + "@shopverse.com";
        notificationPublisher.publish(
            new NotificationEvent.OrderConfirmationNotification(
                e.orderId(), e.customerId(), customerEmail, e.total(), Instant.now()));

        // 2. Neo4j graph update — FREQUENTLY_BOUGHT_TOGETHER for every product pair
        updateNeo4jPurchaseGraph(e.orderId());

        // 3. Inventory reservation event
        kafkaTemplate.send(KafkaTopicsConfig.INVENTORY_TOPIC,
            e.orderId().toString(),
            new InventoryEvent.StockReserved(null, 0, e.orderId(), Instant.now()));

        log.info("OrderPlaced handlers complete for orderId={}", e.orderId());
    }

    /**
     * Loads the order's items from PostgreSQL and writes FREQUENTLY_BOUGHT_TOGETHER
     * relationships to Neo4j for every pair of products in the order.
     *
     * Why look up from DB instead of including items in the Kafka event?
     * The OrderPlaced event payload intentionally stays small (orderId, customerId, total).
     * Full item details live in PostgreSQL and are fetched here on-demand.
     * This is the "thin event, rich read model" pattern.
     *
     * Why is it safe to read PostgreSQL here?
     * The event is published AFTER the transaction commits (via @TransactionalEventListener
     * or post-commit hook in PlaceOrderUseCase), so the order rows are visible.
     *
     * Failures are logged but not re-thrown — a Neo4j write failure must not
     * cause the order confirmation email or stock reservation to roll back.
     */
    private void updateNeo4jPurchaseGraph(Long orderId) {
        try {
            // JOIN FETCH loads items eagerly in one query — avoids LazyInitializationException
            // when getItems() is called outside a JPA transaction (Kafka consumer thread)
            var orderOpt = orderRepository.findWithItemsById(orderId);
            if (orderOpt.isEmpty()) {
                log.warn("Neo4j graph update skipped — order {} not found in DB", orderId);
                return;
            }

            List<OrderItemEntity> items = orderOpt.get().getItems();
            if (items.size() < 2) {
                // Single-item orders don't produce any FREQUENTLY_BOUGHT_TOGETHER pairs
                // but we still upsert the ProductNode so it exists for VIEWED_AFTER edges
                if (!items.isEmpty()) {
                    upsertProductNodeFromItem(items.get(0));
                }
                log.debug("Neo4j: single-item order {}, no pair edges created", orderId);
                return;
            }

            // Upsert a ProductNode for every item in the order
            // MERGE (p:Product {productId: $id}) SET p.name = $name, p.category = $category
            for (OrderItemEntity item : items) {
                upsertProductNodeFromItem(item);
            }

            // Create FREQUENTLY_BOUGHT_TOGETHER for every pair (i, j) where i < j
            // e.g. items = [Laptop, Speaker, Charger] → pairs: (Laptop,Speaker), (Laptop,Charger), (Speaker,Charger)
            int edgesCreated = 0;
            for (int i = 0; i < items.size(); i++) {
                for (int j = i + 1; j < items.size(); j++) {
                    Long productId1 = items.get(i).getProductId();
                    Long productId2 = items.get(j).getProductId();

                    recommendationRepository.recordPurchasedTogether(productId1, productId2, orderId);
                    edgesCreated++;

                    log.debug("Neo4j FREQUENTLY_BOUGHT_TOGETHER: {} <-> {} (orderId={})",
                            productId1, productId2, orderId);
                }
            }

            log.info("Neo4j graph updated for order {}: {} nodes upserted, {} edges created/incremented",
                    orderId, items.size(), edgesCreated);

        } catch (Exception ex) {
            // Never fail order processing because of a graph write failure
            log.error("Neo4j graph update failed for order {} — graph may be stale: {}",
                    orderId, ex.getMessage());
        }
    }

    /**
     * Upserts a ProductNode from an OrderItemEntity.
     * Uses productName from the order item (denormalised snapshot at purchase time).
     * Category is fetched from PostgreSQL products table; falls back to empty string
     * if the product has been deleted since the order was placed.
     */
    private void upsertProductNodeFromItem(OrderItemEntity item) {
        String category = productRepository.findById(item.getProductId())
                .map(p -> p.getCategory() != null ? p.getCategory() : "")
                .orElse("");

        recommendationRepository.upsertProductNode(
                item.getProductId(),
                item.getProductName(),
                category,
                0.0   // avgRating updated separately
        );
    }

    private void handleOrderConfirmed(OrderEvent.OrderConfirmed e) {
        log.info("OrderConfirmed: orderId={}", e.orderId());
        // Confirmation email already sent on OrderPlaced — no additional action needed
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

        kafkaTemplate.send(KafkaTopicsConfig.INVENTORY_TOPIC,
            e.orderId().toString(),
            new InventoryEvent.StockReleased(null, 0, e.orderId(), e.reason(), Instant.now()));

        String customerEmail = resolveCustomerEmail(e.orderId());
        notificationPublisher.publish(
            new NotificationEvent.OrderCancelledNotification(
                e.orderId(), customerEmail, e.reason(), Instant.now()));
    }

    private void handleOrderRefunded(OrderEvent.OrderRefunded e) {
        log.info("Handling OrderRefunded: orderId={} refundAmount={}", e.orderId(), e.refundAmount());

        kafkaTemplate.send(KafkaTopicsConfig.INVENTORY_TOPIC,
            e.orderId().toString(),
            new InventoryEvent.StockReleased(null, 0, e.orderId(), "Order refunded", Instant.now()));

        String customerEmail = resolveCustomerEmail(e.orderId());
        notificationPublisher.publish(
            new NotificationEvent.PaymentSuccessNotification(
                e.orderId(), customerEmail, e.refundAmount(), "REFUND", Instant.now()));

        log.info("OrderRefunded handlers complete for orderId={}", e.orderId());
    }

    /**
     * Dead-letter topic consumer — handles messages that exhausted all @RetryableTopic retries.
     * In production: persist to dead_letter_messages table, alert on-call via PagerDuty/Slack.
     */
    @KafkaListener(topics = KafkaTopicsConfig.ORDERS_TOPIC + ".DLT",
                   groupId = "shopverse-dlt",
                   containerFactory = "kafkaListenerContainerFactory")
    public void consumeDlt(ConsumerRecord<String, Object> record) {
        log.error("DLT message: topic={} partition={} key={} value={}",
            record.topic(), record.partition(), record.key(), record.value());
    }

    /**
     * In production, customer email would be included in the event payload
     * or looked up from a Redis read model / customer cache.
     */
    private String resolveCustomerEmail(Long orderId) {
        return "customer@shopverse.com";
    }
}
