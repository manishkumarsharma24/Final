package com.shopverse.application.usecase.order;

import com.shopverse.domain.model.Order;
import com.shopverse.domain.model.OrderStatus;
import com.shopverse.domain.port.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Returns all orders, optionally filtered by status.
 * Used by GET /api/orders (admin).
 */
@Service
public class GetAllOrdersUseCase {

    private final OrderRepository orderRepository;

    public GetAllOrdersUseCase(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public List<Order> execute(String statusFilter) {
        if (statusFilter != null && !statusFilter.isBlank()) {
            return orderRepository.findByStatus(OrderStatus.valueOf(statusFilter.toUpperCase()));
        }
        // findAll via findByStatus with all statuses, or add findAll to port
        return orderRepository.findAll();
    }
}
