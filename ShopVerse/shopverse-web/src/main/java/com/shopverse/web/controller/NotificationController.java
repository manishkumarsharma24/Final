package com.shopverse.web.controller;

import com.shopverse.application.usecase.notification.SendNotificationUseCase;
import com.shopverse.infrastructure.rabbitmq.NotificationRabbitPublisher;
import com.shopverse.shared.ApiResponse;
import com.shopverse.web.dto.NotificationRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * RabbitMQ notification endpoints.
 *
 * POST /api/notifications/send               — admin: manually dispatch a notification
 * POST /api/notifications/payment/callback   — payment gateway webhook receiver
 * POST /api/notifications/webhook/register   — register merchant webhook URL
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final SendNotificationUseCase    sendNotificationUseCase;
    private final NotificationRabbitPublisher rabbitPublisher;

    public NotificationController(SendNotificationUseCase sendNotificationUseCase,
                                   NotificationRabbitPublisher rabbitPublisher) {
        this.sendNotificationUseCase = sendNotificationUseCase;
        this.rabbitPublisher         = rabbitPublisher;
    }

    /**
     * POST /api/notifications/send
     * Admin endpoint to manually dispatch a notification for any order.
     * Publishes to RabbitMQ → EmailNotificationConsumer delivers the email.
     */
    @PostMapping("/send")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> sendNotification(
            @Valid @RequestBody NotificationRequest req) {
        switch (req.type()) {
            case "ORDER_CONFIRMED" ->
                sendNotificationUseCase.sendOrderConfirmation(
                    req.orderId(), req.customerId(), req.customerEmail(), req.amount());
            case "ORDER_SHIPPED" ->
                sendNotificationUseCase.sendShippingUpdate(
                    req.orderId(), req.customerEmail(), req.trackingNumber());
            case "ORDER_DELIVERED" ->
                sendNotificationUseCase.sendDeliveryConfirmation(
                    req.orderId(), req.customerEmail());
            case "ORDER_CANCELLED" ->
                sendNotificationUseCase.sendCancellationNotice(
                    req.orderId(), req.customerEmail(), req.reason());
            case "PAYMENT_SUCCESS" ->
                sendNotificationUseCase.sendPaymentSuccess(
                    req.orderId(), req.customerEmail(), req.amount(), req.paymentMethod());
            case "PAYMENT_FAILED" ->
                sendNotificationUseCase.sendPaymentFailed(
                    req.orderId(), req.customerEmail(), req.reason());
            default -> throw new IllegalArgumentException("Unknown notification type: " + req.type());
        }
        return ResponseEntity.ok(ApiResponse.ok(null, "Notification queued via RabbitMQ"));
    }

    /**
     * POST /api/notifications/payment/callback
     * Receives payment gateway webhooks (Stripe/PayPal) and publishes to RabbitMQ.
     * PaymentCallbackConsumer processes it asynchronously.
     */
    @PostMapping("/payment/callback")
    public ResponseEntity<ApiResponse<Void>> handlePaymentCallback(
            @RequestBody Map<String, Object> payload) {
        rabbitPublisher.publishPaymentCallback(payload);
        return ResponseEntity.ok(ApiResponse.ok(null, "Payment callback received"));
    }

    /**
     * POST /api/notifications/webhook/register
     * Registers a merchant webhook URL to receive order events via RabbitMQ fanout.
     * Body: { "webhookUrl": "https://merchant.example.com/hooks", "events": ["ORDER_PLACED"] }
     */
    @PostMapping("/webhook/register")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> registerWebhook(
            @RequestBody Map<String, Object> payload) {
        // Broadcast the registration event to all webhook delivery consumers
        rabbitPublisher.publishWebhook(payload);
        return ResponseEntity.ok(ApiResponse.ok(null, "Webhook registered and event published"));
    }
}
