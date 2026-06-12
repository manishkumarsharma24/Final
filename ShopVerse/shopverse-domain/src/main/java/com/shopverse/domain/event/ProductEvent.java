package com.shopverse.domain.event;

import java.time.Instant;

/** Ch14-01: Sealed interface for product domain events. */
public sealed interface ProductEvent permits
        ProductEvent.ProductCreated,
        ProductEvent.ProductUpdated,
        ProductEvent.StockDepleted,
        ProductEvent.StockReplenished {

    Long productId();
    Instant occurredAt();

    record ProductCreated(Long productId, String name, Instant occurredAt) implements ProductEvent {}
    record ProductUpdated(Long productId, String field, Instant occurredAt) implements ProductEvent {}
    record StockDepleted(Long productId, Instant occurredAt) implements ProductEvent {}
    record StockReplenished(Long productId, int quantity, Instant occurredAt) implements ProductEvent {}
}
