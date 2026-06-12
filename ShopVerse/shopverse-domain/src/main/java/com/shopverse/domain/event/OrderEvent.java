package com.shopverse.domain.event;

import java.time.Instant;

/**
 * Ch14-01: Sealed interface for domain events — exhaustive pattern matching.
 * All permitted subtypes are records (immutable, value-based).
 */
public sealed interface OrderEvent permits
        OrderEvent.OrderPlaced,
        OrderEvent.OrderConfirmed,
        OrderEvent.OrderShipped,
        OrderEvent.OrderDelivered,
        OrderEvent.OrderCancelled,
        OrderEvent.OrderRefunded {

    Long orderId();
    Instant occurredAt();

    record OrderPlaced(Long orderId, Long customerId, java.math.BigDecimal total, Instant occurredAt)
            implements OrderEvent {}

    record OrderConfirmed(Long orderId, Instant occurredAt)
            implements OrderEvent {}

    record OrderShipped(Long orderId, String trackingNumber, Instant occurredAt)
            implements OrderEvent {}

    record OrderDelivered(Long orderId, Instant occurredAt)
            implements OrderEvent {}

    record OrderCancelled(Long orderId, String reason, Instant occurredAt)
            implements OrderEvent {}

    record OrderRefunded(Long orderId, java.math.BigDecimal refundAmount, Instant occurredAt)
            implements OrderEvent {}
}
