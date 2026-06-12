package com.shopverse.application.usecase.order;

import com.shopverse.domain.exception.InvalidOrderTransitionException;
import com.shopverse.domain.exception.OrderNotFoundException;
import com.shopverse.domain.model.*;
import com.shopverse.domain.port.OrderActivityRepository;
import com.shopverse.domain.port.OrderRepository;
import com.shopverse.domain.vo.Address;
import com.shopverse.domain.vo.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateOrderStatusUseCase")
class UpdateOrderStatusUseCaseTest {

    @Mock private OrderRepository         orderRepository;
    @Mock private OrderActivityRepository orderActivityRepository;

    @InjectMocks
    private UpdateOrderStatusUseCase useCase;

    private Order pendingOrder;
    private final Address address = new Address("1 St", "City", null, "12345", "US");

    @BeforeEach
    void setUp() {
        pendingOrder = new Order(1L, 42L, address);
        pendingOrder.addItem(new OrderItem(10L, "Widget", 1,
                new Money(new BigDecimal("10.00"), "USD")));

        // save returns the same order
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private void givenOrderInState(OrderStatus state) {
        Order order = new Order(1L, 42L, address);
        order.addItem(new OrderItem(10L, "Widget", 1, new Money(BigDecimal.TEN, "USD")));
        switch (state) {
            case CONFIRMED   -> order.confirm();
            case PROCESSING  -> { order.confirm(); order.startProcessing(); }
            case SHIPPED     -> { order.confirm(); order.startProcessing(); order.ship("TRK"); }
            case DELIVERED   -> { order.confirm(); order.startProcessing(); order.ship("TRK"); order.deliver(); }
            case CANCELLED   -> order.cancel();
            default -> {}
        }
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
    }

    @Test
    @DisplayName("confirm transitions PENDING → CONFIRMED")
    void confirm_order() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));
        Order result = useCase.confirm(1L);
        assertEquals(OrderStatus.CONFIRMED, result.getStatus());
        verifyActivityLogged("ORDER_CONFIRMED");
    }

    @Test
    @DisplayName("startProcessing transitions CONFIRMED → PROCESSING")
    void process_order() {
        givenOrderInState(OrderStatus.CONFIRMED);
        Order result = useCase.startProcessing(1L);
        assertEquals(OrderStatus.PROCESSING, result.getStatus());
        verifyActivityLogged("ORDER_PROCESSING");
    }

    @Test
    @DisplayName("ship transitions PROCESSING → SHIPPED with tracking number")
    void ship_order() {
        givenOrderInState(OrderStatus.PROCESSING);
        Order result = useCase.ship(1L, "TRK-ABC123");
        assertEquals(OrderStatus.SHIPPED, result.getStatus());
        assertEquals("TRK-ABC123", result.getTrackingNumber());
        verifyActivityLogged("ORDER_SHIPPED");
    }

    @Test
    @DisplayName("deliver transitions SHIPPED → DELIVERED")
    void deliver_order() {
        givenOrderInState(OrderStatus.SHIPPED);
        Order result = useCase.deliver(1L);
        assertEquals(OrderStatus.DELIVERED, result.getStatus());
        verifyActivityLogged("ORDER_DELIVERED");
    }

    @Test
    @DisplayName("cancel transitions PENDING → CANCELLED")
    void cancel_order_from_pending() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));
        Order result = useCase.cancel(1L);
        assertEquals(OrderStatus.CANCELLED, result.getStatus());
        verifyActivityLogged("ORDER_CANCELLED");
    }

    @Test
    @DisplayName("throws OrderNotFoundException when order does not exist")
    void throws_when_order_not_found() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(OrderNotFoundException.class, () -> useCase.confirm(99L));
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("invalid transition (DELIVERED → CONFIRMED) propagates as domain exception")
    void invalid_transition_throws() {
        givenOrderInState(OrderStatus.DELIVERED);
        assertThrows(InvalidOrderTransitionException.class, () -> useCase.confirm(1L));
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cassandra failure does not prevent PostgreSQL status update")
    void cassandra_failure_does_not_roll_back() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(pendingOrder));
        doThrow(new RuntimeException("Cassandra down"))
                .when(orderActivityRepository).save(any());

        assertDoesNotThrow(() -> useCase.confirm(1L));
        verify(orderRepository).save(any());
    }

    private void verifyActivityLogged(String eventType) {
        ArgumentCaptor<OrderActivity> captor = ArgumentCaptor.forClass(OrderActivity.class);
        verify(orderActivityRepository).save(captor.capture());
        assertEquals(eventType, captor.getValue().getEventType());
    }
}
