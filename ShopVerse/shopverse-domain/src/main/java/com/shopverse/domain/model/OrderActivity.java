package com.shopverse.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain model for an order activity event — pure Java, no framework dependencies.
 * Stored in Cassandra via the infrastructure layer.
 *
 * Partition key : customerId  — all activity for a customer lives on one node
 * Clustering    : eventTime DESC, eventId — newest events first; UUID breaks ties
 */
public class OrderActivity {

    private Long   customerId;
    private Instant eventTime;
    private UUID   eventId;
    private Long   orderId;
    private String eventType;
    private String details;

    public OrderActivity() {}

    public OrderActivity(Long customerId, Long orderId, String eventType, String details) {
        this.customerId = customerId;
        this.orderId    = orderId;
        this.eventType  = eventType;
        this.details    = details;
        this.eventTime  = Instant.now();
        this.eventId    = UUID.randomUUID();
    }

    public Long    getCustomerId()            { return customerId; }
    public void    setCustomerId(Long v)      { this.customerId = v; }
    public Instant getEventTime()             { return eventTime; }
    public void    setEventTime(Instant v)    { this.eventTime = v; }
    public UUID    getEventId()               { return eventId; }
    public void    setEventId(UUID v)         { this.eventId = v; }
    public Long    getOrderId()               { return orderId; }
    public void    setOrderId(Long v)         { this.orderId = v; }
    public String  getEventType()             { return eventType; }
    public void    setEventType(String v)     { this.eventType = v; }
    public String  getDetails()               { return details; }
    public void    setDetails(String v)       { this.details = v; }
}
