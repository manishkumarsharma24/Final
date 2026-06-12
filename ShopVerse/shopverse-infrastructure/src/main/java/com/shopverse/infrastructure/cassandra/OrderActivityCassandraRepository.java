package com.shopverse.infrastructure.cassandra;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;

import java.time.Instant;
import java.util.List;

/**
 * Ch06-03: Spring Data Cassandra repository for OrderActivityEntity.
 * Named OrderActivityCassandraRepository (not OrderActivityRepository) to avoid
 * naming conflict with the domain port com.shopverse.domain.port.OrderActivityRepository.
 */
public interface OrderActivityCassandraRepository
        extends CassandraRepository<OrderActivityEntity, Long> {

    List<OrderActivityEntity> findByCustomerIdOrderByEventTimeDesc(Long customerId);

    @Query("SELECT * FROM order_activity WHERE customer_id = ?0 AND event_time > ?1 LIMIT 50")
    List<OrderActivityEntity> findRecentActivity(Long customerId, Instant since);
}
