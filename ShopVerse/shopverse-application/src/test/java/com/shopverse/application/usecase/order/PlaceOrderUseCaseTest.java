package com.shopverse.application.usecase.order;

import com.shopverse.application.command.PlaceOrderCommand;
import com.shopverse.application.service.discount.TierDiscountStrategy;
import com.shopverse.domain.exception.CustomerNotFoundException;
import com.shopverse.domain.exception.ProductNotFoundException;
import com.shopverse.domain.model.*;
import com.shopverse.domain.port.*;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlaceOrderUseCase")
class PlaceOrderUseCaseTest {

    @Mock private OrderRepository         orderRepository;
    @Mock private ProductRepository       productRepository;
    @Mock private CustomerRepository      customerRepository;
    @Mock private EventPublisher          eventPublisher;
    @Mock private TierDiscountStrategy    discountStrategy;
    @Mock private OrderActivityRepository orderActivityRepository;

    @InjectMocks
    private PlaceOrderUseCase useCase;

    private Customer customer;
    private Product  product;
    private final PlaceOrderCommand cmd = new PlaceOrderCommand(
            1L, "1 Main St", "Springfield", "IL", "62701", "US",
            List.of(new PlaceOrderCommand.ItemRequest(10L, 2)));

    @BeforeEach
    void setUp() {
        customer = new Customer(1L, "Alice", "Smith", "alice@example.com");
        product  = Product.builder()
                .id(10L).name("Widget")
                .price(new Money(new BigDecimal("20.00"), "USD"))
                .stockQuantity(100).build();

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(discountStrategy.apply(any(), any())).thenAnswer(inv -> inv.getArgument(0));

        // Simulate save: return same order with an id
        when(orderRepository.save(any())).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            return new Order(99L, o.getCustomerId(), o.getShippingAddress()) {{
                o.getItems().forEach(this::addItem);
                confirm(); // order is confirmed by PlaceOrderUseCase
            }};
        });
        when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("places order and returns saved order")
    void places_order_successfully() {
        Order result = useCase.execute(cmd);

        assertNotNull(result);
        assertEquals(99L, result.getId());
        assertEquals(1L, result.getCustomerId());
        verify(orderRepository).save(any(Order.class));
        verify(productRepository).save(any(Product.class)); // stock reduced
        verify(eventPublisher).publish(any());
    }

    @Test
    @DisplayName("throws CustomerNotFoundException when customer does not exist")
    void throws_when_customer_not_found() {
        when(customerRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(CustomerNotFoundException.class, () -> useCase.execute(cmd));
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("throws ProductNotFoundException when product does not exist")
    void throws_when_product_not_found() {
        when(productRepository.findById(10L)).thenReturn(Optional.empty());
        assertThrows(ProductNotFoundException.class, () -> useCase.execute(cmd));
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("logs ORDER_PLACED activity to Cassandra after order is saved")
    void logs_cassandra_activity() {
        useCase.execute(cmd);
        ArgumentCaptor<OrderActivity> captor = ArgumentCaptor.forClass(OrderActivity.class);
        verify(orderActivityRepository).save(captor.capture());
        assertEquals("ORDER_PLACED", captor.getValue().getEventType());
        assertEquals(1L, captor.getValue().getCustomerId());
    }

    @Test
    @DisplayName("continues successfully even if Cassandra throws")
    void cassandra_failure_does_not_roll_back_order() {
        doThrow(new RuntimeException("Cassandra down"))
                .when(orderActivityRepository).save(any());

        // Should not throw — Cassandra failure is swallowed by try-catch
        assertDoesNotThrow(() -> useCase.execute(cmd));
        verify(orderRepository).save(any()); // PostgreSQL order still saved
    }

    @Test
    @DisplayName("reduces product stock by the ordered quantity")
    void reduces_product_stock() {
        useCase.execute(cmd);
        // product started with 100, ordered 2 → 98 remaining
        assertEquals(98, product.getStockQuantity());
    }

    @Test
    @DisplayName("adds loyalty points to customer")
    void adds_loyalty_points() {
        useCase.execute(cmd);
        verify(customerRepository).save(argThat(c -> c.getLoyaltyPoints() > 0));
    }
}
