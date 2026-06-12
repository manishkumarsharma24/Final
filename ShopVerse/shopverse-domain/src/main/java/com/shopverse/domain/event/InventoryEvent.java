package com.shopverse.domain.event;

import java.time.Instant;

/**
 * Sealed interface for inventory domain events.
 * Kafka topic: shopverse.inventory
 */
public sealed interface InventoryEvent permits
        InventoryEvent.StockReserved,
        InventoryEvent.StockReleased,
        InventoryEvent.StockLow,
        InventoryEvent.StockExhausted {

    Long productId();
    Instant occurredAt();

    record StockReserved(Long productId, int quantity, Long orderId, Instant occurredAt)
            implements InventoryEvent {}

    record StockReleased(Long productId, int quantity, Long orderId, String reason, Instant occurredAt)
            implements InventoryEvent {}

    record StockLow(Long productId, int remainingStock, int threshold, Instant occurredAt)
            implements InventoryEvent {}

    record StockExhausted(Long productId, Instant occurredAt)
            implements InventoryEvent {}
}
