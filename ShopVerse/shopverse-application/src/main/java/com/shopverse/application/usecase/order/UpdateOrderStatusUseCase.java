package com.shopverse.application.usecase.order;

import com.shopverse.domain.exception.OrderNotFoundException;
import com.shopverse.domain.model.Order;
import com.shopverse.domain.model.OrderActivity;
import com.shopverse.domain.port.OrderActivityRepository;
import com.shopverse.domain.port.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ch06-03: Update order status and log each transition as an activity event in Cassandra.
 *
 * Supported transitions (enforced by Order domain model):
 *   PENDING    → CONFIRMED
 *   CONFIRMED  → PROCESSING
 *   PROCESSING → SHIPPED  (requires trackingNumber)
 *   SHIPPED    → DELIVERED
 *   PENDING / CONFIRMED / PROCESSING → CANCELLED
 *
 * InvalidOrderTransitionException is thrown for illegal transitions (e.g. DELIVERED → CONFIRMED).
 */
@Service
public class UpdateOrderStatusUseCase {

    private static final Logger log = LoggerFactory.getLogger(UpdateOrderStatusUseCase.class);

    private final OrderRepository         orderRepository;
    private final OrderActivityRepository orderActivityRepository;

    public UpdateOrderStatusUseCase(OrderRepository orderRepository,
                                    OrderActivityRepository orderActivityRepository) {
        this.orderRepository         = orderRepository;
        this.orderActivityRepository = orderActivityRepository;
    }

    @Transactional
    public Order confirm(Long orderId) {
        Order order = load(orderId);
        order.confirm();
        Order saved = orderRepository.save(order);
        logActivity(saved, "ORDER_CONFIRMED",
                String.format("Order #%d confirmed", orderId));
        return saved;
    }

    @Transactional
    public Order startProcessing(Long orderId) {
        Order order = load(orderId);
        order.startProcessing();
        Order saved = orderRepository.save(order);
        logActivity(saved, "ORDER_PROCESSING",
                String.format("Order #%d moved to processing", orderId));
        return saved;
    }

    @Transactional
    public Order ship(Long orderId, String trackingNumber) {
        Order order = load(orderId);
        order.ship(trackingNumber);
        Order saved = orderRepository.save(order);
        logActivity(saved, "ORDER_SHIPPED",
                String.format("Order #%d shipped — tracking: %s", orderId, trackingNumber));
        return saved;
    }

    @Transactional
    public Order deliver(Long orderId) {
        Order order = load(orderId);
        order.deliver();
        Order saved = orderRepository.save(order);
        logActivity(saved, "ORDER_DELIVERED",
                String.format("Order #%d delivered", orderId));
        return saved;
    }

    @Transactional
    public Order cancel(Long orderId) {
        Order order = load(orderId);
        order.cancel();
        Order saved = orderRepository.save(order);
        logActivity(saved, "ORDER_CANCELLED",
                String.format("Order #%d cancelled", orderId));
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
