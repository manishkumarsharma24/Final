package com.shopverse.application.usecase.order;

import com.shopverse.domain.event.OrderEvent;
import com.shopverse.domain.exception.OrderNotFoundException;
import com.shopverse.domain.model.Order;
import com.shopverse.domain.model.OrderActivity;
import com.shopverse.domain.port.EventPublisher;
import com.shopverse.domain.port.OrderActivityRepository;
import com.shopverse.domain.port.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Ch06-03: Update order status and log each transition as an activity event in Cassandra.
 * Ch14-01: Publishes OrderEvent domain events to Kafka on each transition.
 *
 * Supported transitions (enforced by Order domain model):
 *   PENDING    → CONFIRMED
 *   CONFIRMED  → PROCESSING
 *   PROCESSING → SHIPPED  (requires trackingNumber)
 *   SHIPPED    → DELIVERED
 *   DELIVERED  → REFUNDED
 *   PENDING / CONFIRMED / PROCESSING → CANCELLED
 *
 * InvalidOrderTransitionException is thrown for illegal transitions (e.g. DELIVERED → CONFIRMED).
 *
 * Kafka events published (consumed by OrderKafkaConsumer):
 *   OrderConfirmed  → (logged only, no consumer action)
 *   OrderShipped    → consumer sends OrderShippedNotification via RabbitMQ
 *   OrderDelivered  → consumer sends OrderDeliveredNotification via RabbitMQ
 *   OrderCancelled  → consumer releases inventory + sends OrderCancelledNotification
 *   OrderRefunded   → consumer releases inventory + sends PaymentSuccessNotification (refund)
 */
@Service
public class UpdateOrderStatusUseCase {

    private static final Logger log = LoggerFactory.getLogger(UpdateOrderStatusUseCase.class);

    private final OrderRepository         orderRepository;
    private final OrderActivityRepository orderActivityRepository;
    private final EventPublisher          eventPublisher;

    public UpdateOrderStatusUseCase(OrderRepository orderRepository,
                                    OrderActivityRepository orderActivityRepository,
                                    EventPublisher eventPublisher) {
        this.orderRepository         = orderRepository;
        this.orderActivityRepository = orderActivityRepository;
        this.eventPublisher          = eventPublisher;
    }

    @Transactional
    public Order confirm(Long orderId) {
        Order order = load(orderId);
        order.confirm();
        Order saved = orderRepository.save(order);
        logActivity(saved, "ORDER_CONFIRMED",
                String.format("Order #%d confirmed", orderId));
        eventPublisher.publish(new OrderEvent.OrderConfirmed(orderId, Instant.now()));
        return saved;
    }

    @Transactional
    public Order startProcessing(Long orderId) {
        Order order = load(orderId);
        order.startProcessing();
        Order saved = orderRepository.save(order);
        logActivity(saved, "ORDER_PROCESSING",
                String.format("Order #%d moved to processing", orderId));
        // No dedicated Kafka event for PROCESSING — internal state, no consumer action needed
        return saved;
    }

    @Transactional
    public Order ship(Long orderId, String trackingNumber) {
        Order order = load(orderId);
        order.ship(trackingNumber);
        Order saved = orderRepository.save(order);
        logActivity(saved, "ORDER_SHIPPED",
                String.format("Order #%d shipped — tracking: %s", orderId, trackingNumber));
        eventPublisher.publish(
                new OrderEvent.OrderShipped(orderId, trackingNumber, Instant.now()));
        return saved;
    }

    @Transactional
    public Order deliver(Long orderId) {
        Order order = load(orderId);
        order.deliver();
        Order saved = orderRepository.save(order);
        logActivity(saved, "ORDER_DELIVERED",
                String.format("Order #%d delivered", orderId));
        eventPublisher.publish(new OrderEvent.OrderDelivered(orderId, Instant.now()));
        return saved;
    }

    @Transactional
    public Order cancel(Long orderId) {
        Order order = load(orderId);
        order.cancel();
        Order saved = orderRepository.save(order);
        logActivity(saved, "ORDER_CANCELLED",
                String.format("Order #%d cancelled", orderId));
        eventPublisher.publish(
                new OrderEvent.OrderCancelled(orderId, "Customer requested cancellation",
                        Instant.now()));
        return saved;
    }

    /**
     * DELIVERED → REFUNDED.
     * Publishes OrderRefunded to Kafka; consumer releases inventory + sends refund email.
     * refundAmount defaults to the full order total (partial refunds can be added later).
     */
    @Transactional
    public Order refund(Long orderId) {
        Order order = load(orderId);
        BigDecimal refundAmount = order.total().amount();
        order.refund();
        Order saved = orderRepository.save(order);
        logActivity(saved, "ORDER_REFUNDED",
                String.format("Order #%d refunded — amount $%.2f", orderId, refundAmount));
        eventPublisher.publish(
                new OrderEvent.OrderRefunded(orderId, refundAmount, Instant.now()));
        return saved;
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Order load(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    /**
     * Cassandra write is outside @Transactional boundary — wrapped in try-catch
     * so a Cassandra failure never rolls back the PostgreSQL status update.
     */
    private void logActivity(Order order, String eventType, String details) {
        try {
            orderActivityRepository.save(
                    new OrderActivity(order.getCustomerId(), order.getId(), eventType, details));
        } catch (Exception ex) {
            log.warn("Failed to log {} to Cassandra for orderId={}: {}",
                    eventType, order.getId(), ex.getMessage());
        }
    }
}
