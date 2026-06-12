package com.shopverse.infrastructure.rabbitmq;

import com.shopverse.domain.event.NotificationEvent;
import com.shopverse.domain.port.NotificationPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ implementation of the NotificationPublisher port.
 * Routes each NotificationEvent to the correct exchange + routing key.
 *
 * Routing:
 *   OrderConfirmation / Shipped / Delivered / Cancelled → notification.email.order
 *   PaymentSuccess / Failed → notification.email.payment
 *   (SMS variants would use notification.sms.*)
 */
@Component
public class NotificationRabbitPublisher implements NotificationPublisher {

    private static final Logger log = LoggerFactory.getLogger(NotificationRabbitPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public NotificationRabbitPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publish(NotificationEvent event) {
        String routingKey = resolveRoutingKey(event);
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.NOTIFICATIONS_EXCHANGE, routingKey, event);
            log.info("Published {} to exchange={} routingKey={}",
                    event.getClass().getSimpleName(), RabbitMQConfig.NOTIFICATIONS_EXCHANGE, routingKey);
        } catch (Exception ex) {
            log.error("Failed to publish notification event {}: {}",
                    event.getClass().getSimpleName(), ex.getMessage());
        }
    }

    /** Also publish payment callbacks directly to payments exchange. */
    public void publishPaymentCallback(Object payload) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.PAYMENTS_EXCHANGE,
                RabbitMQConfig.PAYMENT_CALLBACK_ROUTING_KEY, payload);
    }

    /** Broadcast webhook events to all bound queues via fanout exchange. */
    public void publishWebhook(Object payload) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.WEBHOOKS_EXCHANGE, "", payload);
    }

    private String resolveRoutingKey(NotificationEvent event) {
        return switch (event) {
            case NotificationEvent.OrderConfirmationNotification e -> "notification.email.order.confirmed";
            case NotificationEvent.OrderShippedNotification e      -> "notification.email.order.shipped";
            case NotificationEvent.OrderDeliveredNotification e    -> "notification.email.order.delivered";
            case NotificationEvent.OrderCancelledNotification e    -> "notification.email.order.cancelled";
            case NotificationEvent.PaymentSuccessNotification e    -> "notification.email.payment.success";
            case NotificationEvent.PaymentFailedNotification e     -> "notification.email.payment.failed";
        };
    }
}
