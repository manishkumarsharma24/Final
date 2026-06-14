package com.shopverse.application.usecase.order;

import com.shopverse.domain.exception.OrderNotFoundException;
import com.shopverse.domain.model.Order;
import com.shopverse.domain.port.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Returns all orders for a given customer, newest first.
 * Used by GET /api/orders/customer/{customerId}.
 */
@Service
public class GetOrdersByCustomerUseCase {

    private final OrderRepository orderRepository;

    public GetOrdersByCustomerUseCase(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public List<Order> execute(Long customerId) {
        return orderRepository.findByCustomerId(customerId);
    }

    /**
     * Resolves the customerId for a given orderId.
     * Used by activity endpoints that need the Cassandra partition key (customerId)
     * but only have the orderId.
     */
    @Transactional(readOnly = true)
    public Long resolveCustomerIdForOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .map(Order::getCustomerId)
                .orElseThrow(() -> new com.shopverse.domain.exception.OrderNotFoundException(orderId));
    }
}
