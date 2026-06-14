package com.shopverse.domain.port;

import com.shopverse.domain.model.Order;
import com.shopverse.domain.model.OrderStatus;

import java.util.List;
import java.util.Optional;

/**
 * Ch03-04: Output port (repository interface) — domain does NOT depend on Spring Data.
 * Hexagonal architecture: infrastructure implements this interface.
 */
public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(Long id);
    List<Order> findByCustomerId(Long customerId);
    List<Order> findByStatus(OrderStatus status);
    List<Order> findAll();
    void deleteById(Long id);
}
