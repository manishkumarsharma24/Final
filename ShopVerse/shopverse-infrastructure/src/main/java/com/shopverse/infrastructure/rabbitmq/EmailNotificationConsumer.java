package com.shopverse.infrastructure.rabbitmq;

import com.shopverse.domain.event.NotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ consumer for email notifications.
 * Listens on shopverse.queue.email.
 *
 * In production this would integrate with SendGrid / SES / JavaMail.
 * DLQ is configured at queue level — failed messages auto-route to shopverse.queue.email.dlq.
 */
@Component
public class EmailNotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationConsumer.class);

    @RabbitListener(queues = RabbitMQConfig.EMAIL_QUEUE,
                    containerFactory = "rabbitListenerContainerFactory")
    public void handleEmailNotification(NotificationEvent event) {
        log.info("Processing email notification: type={} orderId={} to={}",
                event.getClass().getSimpleName(), event.orderId(), event.customerEmail());
        try {
            sendEmail(event);
        } catch (Exception ex) {
            log.error("Failed to send email for orderId={}: {}", event.orderId(), ex.getMessage());
            // Throw to trigger DLQ routing
            throw new RuntimeException("Email send failed", ex);
        }
    }

    /** DLQ consumer — handles permanently failed notifications (alert/store for manual replay). */
    @RabbitListener(queues = RabbitMQConfig.EMAIL_DLQ,
                    containerFactory = "rabbitListenerContainerFactory")
    public void handleDlq(Object message) {
        log.error("DLQ: Email notification permanently failed. Message: {}", message);
        // In production: alert on-call, store to DB for manual reprocessing
    }

    private void sendEmail(NotificationEvent event) {
        String subject = resolveSubject(event);
        String body    = resolveBody(event);
        // Production: emailService.send(event.customerEmail(), subject, body);
        log.info("EMAIL SENT → to={} subject='{}' body='{}'", event.customerEmail(), subject, body);
    }

    private String resolveSubject(NotificationEvent event) {
        return switch (event) {
            case NotificationEvent.OrderConfirmationNotification e ->
                    "Order #" + e.orderId() + " Confirmed — ShopVerse";
            case NotificationEvent.OrderShippedNotification e ->
                    "Your Order #" + e.orderId() + " Has Shipped! Tracking: " + e.trackingNumber();
            case NotificationEvent.OrderDeliveredNotification e ->
                    "Order #" + e.orderId() + " Delivered — Thank you!";
            case NotificationEvent.OrderCancelledNotification e ->
                    "Order #" + e.orderId() + " Cancelled";
            case NotificationEvent.PaymentSuccessNotification e ->
                    "Payment Confirmed — Order #" + e.orderId();
            case NotificationEvent.PaymentFailedNotification e ->
                    "Payment Failed — Order #" + e.orderId();
        };
    }

    private String resolveBody(NotificationEvent event) {
        return switch (event) {
            case NotificationEvent.OrderConfirmationNotification e ->
                    String.format("Hi! Your order #%d totalling $%.2f has been confirmed.",
                            e.orderId(), e.total());
            case NotificationEvent.OrderShippedNotification e ->
                    String.format("Your order #%d has shipped. Track it with: %s",
                            e.orderId(), e.trackingNumber());
            case NotificationEvent.OrderDeliveredNotification e ->
                    "Your order #" + e.orderId() + " has been delivered. Enjoy!";
            case NotificationEvent.OrderCancelledNotification e ->
                    "Your order #" + e.orderId() + " has been cancelled. Reason: " + e.reason();
            case NotificationEvent.PaymentSuccessNotification e ->
                    String.format("Payment of $%.2f via %s confirmed for order #%d.",
                            e.amount(), e.paymentMethod(), e.orderId());
            case NotificationEvent.PaymentFailedNotification e ->
                    "Payment failed for order #" + e.orderId() + ". Reason: " + e.failureReason();
        };
    }
}
