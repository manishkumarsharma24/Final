package com.shopverse.infrastructure.kafka;

import com.shopverse.domain.event.InventoryEvent;
import com.shopverse.domain.event.ProductEvent;
import com.shopverse.domain.port.EventPublisher;
import com.shopverse.infrastructure.jpa.repository.JpaProductRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Kafka consumer for shopverse.inventory topic.
 *
 * Scenarios:
 *   StockReserved  → confirm stock was already reduced by PlaceOrderUseCase (idempotent log)
 *   StockReleased  → restore product stock in PostgreSQL on order cancellation
 *   StockLow       → alert admin / trigger reorder workflow
 *   StockExhausted → mark product as out-of-stock
 */
@Component
public class InventoryKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(InventoryKafkaConsumer.class);

    private final JpaProductRepository productRepository;
    private final EventPublisher       eventPublisher;

    public InventoryKafkaConsumer(JpaProductRepository productRepository,
                                  EventPublisher eventPublisher) {
        this.productRepository = productRepository;
        this.eventPublisher    = eventPublisher;
    }

    @RetryableTopic(
        attempts = "3",
        backoff = @Backoff(delay = 2000, multiplier = 2.0),
        dltTopicSuffix = ".DLT"
    )
    @KafkaListener(topics = KafkaTopicsConfig.INVENTORY_TOPIC,
                   groupId = "shopverse-inventory-consumer",
                   containerFactory = "kafkaListenerContainerFactory")
    public void consume(ConsumerRecord<String, Object> record) {
        log.info("Inventory event: key={}", record.key());
        if (record.value() instanceof InventoryEvent event) {
            handleInventoryEvent(event);
        }
    }

    private void handleInventoryEvent(InventoryEvent event) {
        switch (event) {
            case InventoryEvent.StockReserved e -> {
                log.info("StockReserved: productId={} qty={} orderId={}",
                        e.productId(), e.quantity(), e.orderId());
                // Stock already reduced by PlaceOrderUseCase — this is an audit log
            }
            case InventoryEvent.StockReleased e -> {
                log.info("StockReleased: productId={} qty={} orderId={} reason={}",
                        e.productId(), e.quantity(), e.orderId(), e.reason());
                if (e.productId() != null && e.quantity() > 0) {
                    restoreStock(e.productId(), e.quantity());
                }
            }
            case InventoryEvent.StockLow e -> {
                log.warn("StockLow ALERT: productId={} remaining={} threshold={}",
                        e.productId(), e.remainingStock(), e.threshold());
                // Production: trigger PurchaseOrder workflow, alert procurement team
            }
            case InventoryEvent.StockExhausted e -> {
                log.warn("StockExhausted: productId={}", e.productId());
                eventPublisher.publish(new ProductEvent.StockDepleted(e.productId(), Instant.now()));
            }
        }
    }

    private void restoreStock(Long productId, int quantity) {
        productRepository.findById(productId).ifPresent(product -> {
            product.setStockQuantity(product.getStockQuantity() + quantity);
            productRepository.save(product);
            log.info("Stock restored: productId={} qty=+{} newTotal={}",
                    productId, quantity, product.getStockQuantity());
        });
    }
}
