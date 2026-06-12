package com.shopverse.application.service.observer;

import com.shopverse.domain.event.OrderEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Ch02-06: Observer pattern via Spring ApplicationEventPublisher.
 * Ch03-05: @Async — non-blocking event handling.
 */
@Component
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    @Async
    @EventListener
    public void onOrderPlaced(OrderEvent.OrderPlaced event) {
        log.info("[EVENT] Order placed: orderId={}, customerId={}, total={}",
                 event.orderId(), event.customerId(), event.total());
        // TODO: send confirmation email, update analytics
    }

    @Async
    @EventListener
    public void onOrderShipped(OrderEvent.OrderShipped event) {
        log.info("[EVENT] Order shipped: orderId={}, tracking={}",
                 event.orderId(), event.trackingNumber());
        // TODO: push notification to customer
    }

    @Async
    @EventListener
    public void onOrderCancelled(OrderEvent.OrderCancelled event) {
        log.info("[EVENT] Order cancelled: orderId={}, reason={}",
                 event.orderId(), event.reason());
        // TODO: trigger stock replenishment, refund processing
    }
}
