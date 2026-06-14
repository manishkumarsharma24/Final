package com.shopverse.infrastructure.adapter;

import com.shopverse.domain.model.OrderActivity;
import com.shopverse.domain.port.OrderActivityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * No-op implementation of OrderActivityRepository used when cassandra.enabled=false.
 *
 * Cassandra stores the order activity audit log (who did what, when).
 * When Cassandra is not running (e.g. local dev without Docker), this adapter
 * silently discards writes and returns empty lists so the app starts normally.
 *
 * To enable real Cassandra: set cassandra.enabled=true (or remove the property,
 * since matchIfMissing=true on CassandraOrderActivityAdapter defaults to Cassandra on).
 */
@Repository
@ConditionalOnProperty(name = "cassandra.enabled", havingValue = "false")
public class NoOpOrderActivityAdapter implements OrderActivityRepository {

    private static final Logger log = LoggerFactory.getLogger(NoOpOrderActivityAdapter.class);

    @Override
    public void save(OrderActivity activity) {
        log.debug("Cassandra disabled — order activity not persisted: orderId={} type={}",
                activity.getOrderId(), activity.getEventType());
    }

    @Override
    public List<OrderActivity> findByCustomerId(Long customerId) {
        log.debug("Cassandra disabled — returning empty order activity for customerId={}", customerId);
        return Collections.emptyList();
    }

    @Override
    public List<OrderActivity> findRecentByCustomerId(Long customerId, Instant since) {
        log.debug("Cassandra disabled — returning empty recent activity for customerId={}", customerId);
        return Collections.emptyList();
    }
}
