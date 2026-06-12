package com.shopverse.domain.model;

import com.shopverse.domain.exception.InvalidOrderTransitionException;
import com.shopverse.domain.vo.Address;
import com.shopverse.domain.vo.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Order domain model")
class OrderTest {

    private Order order;
    private final Address address = new Address("1 Main St", "Springfield", "IL", "62701", "US");

    @BeforeEach
    void setUp() {
        order = new Order(null, 1L, address);
        order.addItem(new OrderItem(10L, "Widget", 2,
                new Money(new BigDecimal("15.00"), "USD")));
    }

    @Test
    @DisplayName("starts in PENDING status")
    void starts_pending() {
        assertEquals(OrderStatus.PENDING, order.getStatus());
    }

    @Test
    @DisplayName("calculates total correctly")
    void total_is_correct() {
        // 2 × $15 = $30
        assertEquals(new BigDecimal("30.00"), order.total().amount());
        assertEquals("USD", order.total().currency());
    }

    @Test
    @DisplayName("cannot add items after confirmation")
    void cannot_add_items_after_confirm() {
        order.confirm();
        assertThrows(IllegalStateException.class,
                () -> order.addItem(new OrderItem(11L, "Gadget", 1,
                        new Money(new BigDecimal("5.00"), "USD"))));
    }

    @Test
    @DisplayName("full lifecycle: PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED")
    void full_lifecycle() {
        order.confirm();
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());

        order.startProcessing();
        assertEquals(OrderStatus.PROCESSING, order.getStatus());

        order.ship("TRK-12345");
        assertEquals(OrderStatus.SHIPPED, order.getStatus());
        assertEquals("TRK-12345", order.getTrackingNumber());

        order.deliver();
        assertEquals(OrderStatus.DELIVERED, order.getStatus());
    }

    @Test
    @DisplayName("can cancel from PENDING")
    void cancel_from_pending() {
        order.cancel();
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    @Test
    @DisplayName("can cancel from CONFIRMED")
    void cancel_from_confirmed() {
        order.confirm();
        order.cancel();
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    @Test
    @DisplayName("can cancel from PROCESSING")
    void cancel_from_processing() {
        order.confirm();
        order.startProcessing();
        order.cancel();
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    @Test
    @DisplayName("cannot cancel after SHIPPED — throws InvalidOrderTransitionException")
    void cannot_cancel_after_shipped() {
        order.confirm();
        order.startProcessing();
        order.ship("TRK-99");
        assertThrows(InvalidOrderTransitionException.class, order::cancel);
    }

    @Test
    @DisplayName("cannot jump PENDING → DELIVERED — throws InvalidOrderTransitionException")
    void cannot_skip_transitions() {
        assertThrows(InvalidOrderTransitionException.class, order::deliver);
    }

    @Test
    @DisplayName("cannot re-confirm after DELIVERED")
    void cannot_re_confirm_after_delivered() {
        order.confirm();
        order.startProcessing();
        order.ship("TRK-X");
        order.deliver();
        assertThrows(InvalidOrderTransitionException.class, order::confirm);
    }

    @Test
    @DisplayName("isEmpty returns true when no items")
    void empty_order() {
        Order empty = new Order(null, 1L, address);
        assertTrue(empty.isEmpty());
    }
}
