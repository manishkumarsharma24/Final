package com.shopverse.application.usecase.order;

import com.shopverse.domain.model.OrderActivity;
import com.shopverse.domain.port.OrderActivityRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Ch06-03: Fetch order activity events for a customer from Cassandra.
 */
@Service
public class GetOrderActivityUseCase {

    private final OrderActivityRepository orderActivityRepository;

    public GetOrderActivityUseCase(OrderActivityRepository orderActivityRepository) {
        this.orderActivityRepository = orderActivityRepository;
    }

    /** All activity for a customer, newest first. */
    public List<OrderActivity> getByCustomer(Long customerId) {
        return orderActivityRepository.findByCustomerId(customerId);
    }

    /** Last 50 events since a given point in time. */
    public List<OrderActivity> getRecentByCustomer(Long customerId, Instant since) {
        return orderActivityRepository.findRecentByCustomerId(customerId, since);
    }

    /**
     * Activity for a specific order.
     * Cassandra is partitioned by customerId, so we fetch all customer events
     * and filter by orderId in memory — efficient because a customer's events
     * all live on one Cassandra node.
     */
    public List<OrderActivity> getByOrderId(Long customerId, Long orderId) {
        return orderActivityRepository.findByCustomerId(customerId)
                .stream()
                .filter(a -> orderId.equals(a.getOrderId()))
                .collect(Collectors.toList());
    }
}
