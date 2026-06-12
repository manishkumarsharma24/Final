package com.shopverse.infrastructure.rabbitmq;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration — exchanges, queues, bindings, DLQ setup.
 *
 * Architecture:
 *   shopverse.notifications (topic exchange)
 *     ├── shopverse.queue.email          ← routing key: notification.email.#
 *     ├── shopverse.queue.sms            ← routing key: notification.sms.#
 *     └── DLQ: shopverse.queue.email.dlq (on rejection/expiry)
 *
 *   shopverse.payments (direct exchange)
 *     ├── shopverse.queue.payment.callback  ← routing key: payment.callback
 *     └── shopverse.queue.payment.webhook   ← routing key: payment.webhook
 *
 *   shopverse.webhooks (fanout exchange)
 *     └── shopverse.queue.webhook.delivery
 */
@Configuration
public class RabbitMQConfig {

    // ── Exchange names ─────────────────────────────────────────────────────────
    public static final String NOTIFICATIONS_EXCHANGE = "shopverse.notifications";
    public static final String PAYMENTS_EXCHANGE      = "shopverse.payments";
    public static final String WEBHOOKS_EXCHANGE      = "shopverse.webhooks";

    // ── Queue names ────────────────────────────────────────────────────────────
    public static final String EMAIL_QUEUE            = "shopverse.queue.email";
    public static final String SMS_QUEUE              = "shopverse.queue.sms";
    public static final String EMAIL_DLQ              = "shopverse.queue.email.dlq";
    public static final String PAYMENT_CALLBACK_QUEUE = "shopverse.queue.payment.callback";
    public static final String PAYMENT_WEBHOOK_QUEUE  = "shopverse.queue.payment.webhook";
    public static final String WEBHOOK_DELIVERY_QUEUE = "shopverse.queue.webhook.delivery";

    // ── Routing keys ──────────────────────────────────────────────────────────
    public static final String EMAIL_ROUTING_KEY            = "notification.email.#";
    public static final String SMS_ROUTING_KEY              = "notification.sms.#";
    public static final String PAYMENT_CALLBACK_ROUTING_KEY = "payment.callback";
    public static final String PAYMENT_WEBHOOK_ROUTING_KEY  = "payment.webhook";

    // ── Exchanges ─────────────────────────────────────────────────────────────

    @Bean
    public TopicExchange notificationsExchange() {
        return ExchangeBuilder.topicExchange(NOTIFICATIONS_EXCHANGE).durable(true).build();
    }

    @Bean
    public DirectExchange paymentsExchange() {
        return ExchangeBuilder.directExchange(PAYMENTS_EXCHANGE).durable(true).build();
    }

    @Bean
    public FanoutExchange webhooksExchange() {
        return ExchangeBuilder.fanoutExchange(WEBHOOKS_EXCHANGE).durable(true).build();
    }

    // ── Dead Letter Exchange ───────────────────────────────────────────────────

    @Bean
    public DirectExchange deadLetterExchange() {
        return ExchangeBuilder.directExchange("shopverse.dlx").durable(true).build();
    }

    // ── Queues ────────────────────────────────────────────────────────────────

    @Bean
    public Queue emailQueue() {
        // Messages rejected or expired go to DLX → DLQ
        return QueueBuilder.durable(EMAIL_QUEUE)
                .withArgument("x-dead-letter-exchange", "shopverse.dlx")
                .withArgument("x-dead-letter-routing-key", "email.dlq")
                .withArgument("x-message-ttl", 300_000) // 5 min TTL
                .build();
    }

    @Bean
    public Queue emailDlq() {
        return QueueBuilder.durable(EMAIL_DLQ).build();
    }

    @Bean
    public Queue smsQueue() {
        return QueueBuilder.durable(SMS_QUEUE)
                .withArgument("x-dead-letter-exchange", "shopverse.dlx")
                .withArgument("x-message-ttl", 300_000)
                .build();
    }

    @Bean
    public Queue paymentCallbackQueue() {
        return QueueBuilder.durable(PAYMENT_CALLBACK_QUEUE)
                .withArgument("x-dead-letter-exchange", "shopverse.dlx")
                .withArgument("x-message-ttl", 60_000) // 1 min TTL for payment callbacks
                .build();
    }

    @Bean
    public Queue paymentWebhookQueue() {
        return QueueBuilder.durable(PAYMENT_WEBHOOK_QUEUE).build();
    }

    @Bean
    public Queue webhookDeliveryQueue() {
        return QueueBuilder.durable(WEBHOOK_DELIVERY_QUEUE)
                .withArgument("x-dead-letter-exchange", "shopverse.dlx")
                .withArgument("x-message-ttl", 600_000) // 10 min
                .build();
    }

    // ── Bindings ──────────────────────────────────────────────────────────────

    @Bean
    public Binding emailBinding(Queue emailQueue, TopicExchange notificationsExchange) {
        return BindingBuilder.bind(emailQueue).to(notificationsExchange).with(EMAIL_ROUTING_KEY);
    }

    @Bean
    public Binding smsBinding(Queue smsQueue, TopicExchange notificationsExchange) {
        return BindingBuilder.bind(smsQueue).to(notificationsExchange).with(SMS_ROUTING_KEY);
    }

    @Bean
    public Binding emailDlqBinding(Queue emailDlq, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(emailDlq).to(deadLetterExchange).with("email.dlq");
    }

    @Bean
    public Binding paymentCallbackBinding(Queue paymentCallbackQueue, DirectExchange paymentsExchange) {
        return BindingBuilder.bind(paymentCallbackQueue).to(paymentsExchange)
                .with(PAYMENT_CALLBACK_ROUTING_KEY);
    }

    @Bean
    public Binding paymentWebhookBinding(Queue paymentWebhookQueue, DirectExchange paymentsExchange) {
        return BindingBuilder.bind(paymentWebhookQueue).to(paymentsExchange)
                .with(PAYMENT_WEBHOOK_ROUTING_KEY);
    }

    @Bean
    public Binding webhookDeliveryBinding(Queue webhookDeliveryQueue, FanoutExchange webhooksExchange) {
        return BindingBuilder.bind(webhookDeliveryQueue).to(webhooksExchange);
    }

    // ── Serialization ─────────────────────────────────────────────────────────

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        // Publisher confirms — ensures messages reach the broker
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                // Log/alert on nack
                System.err.println("RabbitMQ NACK: " + cause);
            }
        });
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());
        factory.setDefaultRequeueRejected(false); // send to DLQ on failure, don't requeue endlessly
        factory.setConcurrentConsumers(3);
        factory.setMaxConcurrentConsumers(10);
        return factory;
    }
}
