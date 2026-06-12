package com.shopverse.application.usecase.notification;

import com.shopverse.domain.event.NotificationEvent;
import com.shopverse.domain.port.NotificationPublisher;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Use case: send notifications via RabbitMQ.
 * Called directly from REST API for manual notification dispatch (admin use).
 * Automated notifications are triggered by Kafka order event consumers.
 */
@Service
public class SendNotificationUseCase {

    private final NotificationPublisher notificationPublisher;

    public SendNotificationUseCase(NotificationPublisher notificationPublisher) {
        this.notificationPublisher = notificationPublisher;
    }

    public void sendOrderConfirmation(Long orderId, Long customerId,
                                       String email, java.math.BigDecimal total) {
        notificationPublisher.publish(
            new NotificationEvent.OrderConfirmationNotification(
                orderId, customerId, email, total, Instant.now()));
    }

    public void sendShippingUpdate(Long orderId, String email, String trackingNumber) {
        notificationPublisher.publish(
            new NotificationEvent.OrderShippedNotification(
                orderId, email, trackingNumber, Instant.now()));
    }

    public void sendDeliveryConfirmation(Long orderId, String email) {
        notificationPublisher.publish(
            new NotificationEvent.OrderDeliveredNotification(orderId, email, Instant.now()));
    }

    public void sendCancellationNotice(Long orderId, String email, String reason) {
        notificationPublisher.publish(
            new NotificationEvent.OrderCancelledNotification(orderId, email, reason, Instant.now()));
    }

    public void sendPaymentSuccess(Long orderId, String email,
                                    java.math.BigDecimal amount, String paymentMethod) {
        notificationPublisher.publish(
            new NotificationEvent.PaymentSuccessNotification(
                orderId, email, amount, paymentMethod, Instant.now()));
    }

    public void sendPaymentFailed(Long orderId, String email, String reason) {
        notificationPublisher.publish(
            new NotificationEvent.PaymentFailedNotification(orderId, email, reason, Instant.now()));
    }
}
