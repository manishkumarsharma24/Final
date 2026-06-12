package com.shopverse.domain.port;

import com.shopverse.domain.model.OrderActivity;

import java.time.Instant;
import java.util.List;

/**
 * Output port for order activity events.
 * Implemented in the infrastructure layer by CassandraOrderActivityAdapter.
 */
public interface OrderActivityRepository {

    void save(OrderActivity activity);

    List<OrderActivity> findByCustomerId(Long customerId);

    List<OrderActivity> findRecentByCustomerId(Long customerId, Instant since);
}
