package com.shopverse.infrastructure.adapter;

import com.shopverse.domain.model.OrderActivity;
import com.shopverse.domain.port.OrderActivityRepository;
import com.shopverse.infrastructure.cassandra.OrderActivityCassandraRepository;
import com.shopverse.infrastructure.cassandra.OrderActivityEntity;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Hexagonal adapter — OrderActivityRepository domain port → Cassandra.
 * Maps between the domain OrderActivity model and the OrderActivityEntity Spring Data entity.
 */
@Repository
public class CassandraOrderActivityAdapter implements OrderActivityRepository {

    private final OrderActivityCassandraRepository cassandraRepo;

    public CassandraOrderActivityAdapter(OrderActivityCassandraRepository cassandraRepo) {
        this.cassandraRepo = cassandraRepo;
    }

    @Override
    public void save(OrderActivity activity) {
        cassandraRepo.save(toEntity(activity));
    }

    @Override
    public List<OrderActivity> findByCustomerId(Long customerId) {
        return cassandraRepo.findByCustomerIdOrderByEventTimeDesc(customerId)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<OrderActivity> findRecentByCustomerId(Long customerId, Instant since) {
        return cassandraRepo.findRecentActivity(customerId, since)
                .stream().map(this::toDomain).toList();
    }

    // ── Mapping helpers ──────────────────────────────────────────────────────

    private OrderActivityEntity toEntity(OrderActivity a) {
        OrderActivityEntity e = new OrderActivityEntity(
                a.getCustomerId(), a.getOrderId(), a.getEventType(), a.getDetails());
        // Preserve eventTime and eventId if already set (e.g. re-save scenario)
        if (a.getEventTime() != null) e = new OrderActivityEntity(
                a.getCustomerId(), a.getOrderId(), a.getEventType(), a.getDetails());
        return e;
    }

    private OrderActivity toDomain(OrderActivityEntity e) {
        OrderActivity a = new OrderActivity();
        a.setCustomerId(e.getCustomerId());
        a.setEventTime(e.getEventTime());
        a.setEventId(e.getEventId());
        a.setOrderId(e.getOrderId());
        a.setEventType(e.getEventType());
        a.setDetails(e.getDetails());
        return a;
    }
}
