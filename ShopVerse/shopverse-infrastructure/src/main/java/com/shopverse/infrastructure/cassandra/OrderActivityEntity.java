package com.shopverse.infrastructure.cassandra;

import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Ch06-03: Cassandra wide-column entity — order_activity table.
 * Partition key: customer_id — all activity for a customer on one node.
 * Clustering: event_time DESC — newest events first.
 */
@Table("order_activity")
public class OrderActivityEntity {

    @PrimaryKeyColumn(name = "customer_id", type = PrimaryKeyType.PARTITIONED)
    private Long customerId;

    @PrimaryKeyColumn(name = "event_time", ordering = Ordering.DESCENDING,
                      type = PrimaryKeyType.CLUSTERED)
    private Instant eventTime;

    @PrimaryKeyColumn(name = "event_id", type = PrimaryKeyType.CLUSTERED)
    private UUID eventId;

    @Column("order_id")
    private Long orderId;

    @Column("event_type")
    private String eventType;

    @Column("details")
    private String details;

    public OrderActivityEntity() {}

    public OrderActivityEntity(Long customerId, Long orderId, String eventType, String details) {
        this.customerId = customerId;
        this.orderId    = orderId;
        this.eventType  = eventType;
        this.details    = details;
        this.eventTime  = Instant.now();
        this.eventId    = UUID.randomUUID();
    }

    // Getters & setters
    public Long getCustomerId()             { return customerId; }
    public Instant getEventTime()           { return eventTime; }
    public UUID getEventId()                { return eventId; }
    public Long getOrderId()                { return orderId; }
    public String getEventType()            { return eventType; }
    public String getDetails()              { return details; }
}
