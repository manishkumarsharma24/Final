package com.shopverse.infrastructure.rabbitmq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * RabbitMQ consumer for payment gateway callbacks (Stripe / PayPal webhooks).
 *
 * Flow:
 *   Payment gateway → POST /api/payments/callback (REST endpoint)
 *   → PaymentCallbackController publishes to shopverse.payments exchange
 *   → This consumer processes the callback asynchronously
 *   → On success: publishes PaymentSuccessNotification
 *   → On failure: publishes PaymentFailedNotification + triggers retry logic
 *
 * DLQ: messages that fail 3 times route to shopverse.dlx
 */
@Component
public class PaymentCallbackConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentCallbackConsumer.class);

    private final NotificationRabbitPublisher notificationPublisher;

    public PaymentCallbackConsumer(NotificationRabbitPublisher notificationPublisher) {
        this.notificationPublisher = notificationPublisher;
    }

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_CALLBACK_QUEUE,
                    containerFactory = "rabbitListenerContainerFactory")
    public void handlePaymentCallback(Map<String, Object> payload) {
        String status   = (String) payload.getOrDefault("status", "unknown");
        Long orderId    = Long.valueOf(payload.get("orderId").toString());
        String email    = (String) payload.getOrDefault("customerEmail", "");

        log.info("Payment callback received: orderId={} status={}", orderId, status);

        switch (status) {
            case "SUCCESS" -> {
                var amount        = new java.math.BigDecimal(payload.get("amount").toString());
                var paymentMethod = (String) payload.getOrDefault("paymentMethod", "CARD");
                notificationPublisher.publish(
                    new com.shopverse.domain.event.NotificationEvent.PaymentSuccessNotification(
                        orderId, email, amount, paymentMethod, java.time.Instant.now()));
                log.info("Payment SUCCESS processed for orderId={}", orderId);
            }
            case "FAILED" -> {
                var reason = (String) payload.getOrDefault("failureReason", "Unknown");
                notificationPublisher.publish(
                    new com.shopverse.domain.event.NotificationEvent.PaymentFailedNotification(
                        orderId, email, reason, java.time.Instant.now()));
                log.warn("Payment FAILED for orderId={} reason={}", orderId, reason);
            }
            default -> log.warn("Unknown payment status={} for orderId={}", status, orderId);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_WEBHOOK_QUEUE,
                    containerFactory = "rabbitListenerContainerFactory")
    public void handlePaymentWebhook(Map<String, Object> payload) {
        log.info("Payment webhook received: event={}", payload.get("event"));
        // Idempotency check: if already processed this webhook ID, skip
        String webhookId = (String) payload.getOrDefault("webhookId", "");
        log.info("Processing webhook id={}", webhookId);
        // In production: verify signature, idempotency check, update order state
    }
}
