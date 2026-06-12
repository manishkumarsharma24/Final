package com.shopverse.application.usecase.order;

import com.shopverse.domain.model.OrderActivity;
import com.shopverse.domain.port.OrderActivityRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetOrderActivityUseCase")
class GetOrderActivityUseCaseTest {

    @Mock private OrderActivityRepository activityRepository;
    @InjectMocks private GetOrderActivityUseCase useCase;

    private final OrderActivity placed = new OrderActivity(1L, 100L, "ORDER_PLACED", "Order placed");
    private final OrderActivity confirmed = new OrderActivity(1L, 100L, "ORDER_CONFIRMED", "Confirmed");

    @Test
    @DisplayName("returns all activity for a customer")
    void get_all_activity_by_customer() {
        when(activityRepository.findByCustomerId(1L)).thenReturn(List.of(placed, confirmed));

        List<OrderActivity> results = useCase.getByCustomer(1L);

        assertEquals(2, results.size());
        verify(activityRepository).findByCustomerId(1L);
    }

    @Test
    @DisplayName("returns recent activity since a given timestamp")
    void get_recent_activity_since() {
        Instant since = Instant.now().minusSeconds(3600);
        when(activityRepository.findRecentByCustomerId(1L, since))
                .thenReturn(List.of(confirmed));

        List<OrderActivity> results = useCase.getRecentByCustomer(1L, since);

        assertEquals(1, results.size());
        assertEquals("ORDER_CONFIRMED", results.get(0).getEventType());
        verify(activityRepository).findRecentByCustomerId(1L, since);
    }

    @Test
    @DisplayName("returns empty list when customer has no activity")
    void returns_empty_when_no_activity() {
        when(activityRepository.findByCustomerId(99L)).thenReturn(List.of());

        List<OrderActivity> results = useCase.getByCustomer(99L);
        assertTrue(results.isEmpty());
    }
}
