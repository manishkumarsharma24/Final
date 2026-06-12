package com.shopverse.domain.event;

import java.time.Instant;

/**
 * Sealed interface for notification domain events.
 * Published by order lifecycle transitions; consumed by notification handlers.
 */
public sealed interface NotificationEvent permits
        NotificationEvent.OrderConfirmationNotification,
        NotificationEvent.OrderShippedNotification,
        NotificationEvent.OrderDeliveredNotification,
        NotificationEvent.OrderCancelledNotification,
        NotificationEvent.PaymentSuccessNotification,
        NotificationEvent.PaymentFailedNotification {

    Long orderId();
    String customerEmail();
    Instant occurredAt();

    record OrderConfirmationNotification(
            Long orderId, Long customerId, String customerEmail,
            java.math.BigDecimal total, Instant occurredAt)
            implements NotificationEvent {}

    record OrderShippedNotification(
            Long orderId, String customerEmail,
            String trackingNumber, Instant occurredAt)
            implements NotificationEvent {}

    record OrderDeliveredNotification(
            Long orderId, String customerEmail, Instant occurredAt)
            implements NotificationEvent {}

    record OrderCancelledNotification(
            Long orderId, String customerEmail, String reason, Instant occurredAt)
            implements NotificationEvent {}

    record PaymentSuccessNotification(
            Long orderId, String customerEmail,
            java.math.BigDecimal amount, String paymentMethod, Instant occurredAt)
            implements NotificationEvent {}

    record PaymentFailedNotification(
            Long orderId, String customerEmail,
            String failureReason, Instant occurredAt)
            implements NotificationEvent {}
}
