package com.shopverse.infrastructure.rabbitmq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * RabbitMQ fanout consumer for outgoing webhook delivery.
 * Receives all events broadcast on shopverse.webhooks exchange.
 *
 * Use case: merchants register webhook URLs; ShopVerse POSTs order events to them.
 * This consumer reads from the fanout queue and delivers to each registered endpoint.
 *
 * Retry: Spring AMQP retries up to 3 times with exponential backoff.
 * DLQ: permanently failed deliveries go to shopverse.dlx for alerting.
 */
@Component
public class WebhookDeliveryConsumer {

    private static final Logger log = LoggerFactory.getLogger(WebhookDeliveryConsumer.class);

    @RabbitListener(queues = RabbitMQConfig.WEBHOOK_DELIVERY_QUEUE,
                    containerFactory = "rabbitListenerContainerFactory")
    public void handleWebhookDelivery(Map<String, Object> payload) {
        String webhookUrl = (String) payload.getOrDefault("webhookUrl", "");
        String eventType  = (String) payload.getOrDefault("eventType", "");
        String orderId    = String.valueOf(payload.getOrDefault("orderId", ""));

        log.info("Delivering webhook: url={} eventType={} orderId={}", webhookUrl, eventType, orderId);

        try {
            deliverWebhook(webhookUrl, eventType, payload);
            log.info("Webhook delivered successfully to {}", webhookUrl);
        } catch (Exception ex) {
            log.error("Webhook delivery failed to {}: {}", webhookUrl, ex.getMessage());
            throw new RuntimeException("Webhook delivery failed — will retry", ex);
        }
    }

    private void deliverWebhook(String url, String eventType, Map<String, Object> payload) {
        // Production: use RestTemplate / WebClient to POST to the merchant's registered URL
        // with HMAC-SHA256 signature header for verification
        log.info("POST {} → eventType={} payload={}", url, eventType, payload);
    }
}
