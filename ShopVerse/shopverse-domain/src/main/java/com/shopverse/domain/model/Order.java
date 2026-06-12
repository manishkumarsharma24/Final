package com.shopverse.domain.model;

import com.shopverse.domain.exception.InvalidOrderTransitionException;
import com.shopverse.domain.vo.Address;
import com.shopverse.domain.vo.Money;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Ch02-02: Order aggregate root.
 * Ch02-07: Invariant enforcement via exceptions.
 * Ch14-01: Raises domain events on state transitions.
 */
public class Order {

    private final Long id;
    private final Long customerId;
    private final List<OrderItem> items;
    private OrderStatus status;
    private Address shippingAddress;
    private String trackingNumber;
    private final Instant createdAt;
    private Instant updatedAt;

    public Order(Long id, Long customerId, Address shippingAddress) {
        this.id              = id;
        this.customerId      = Objects.requireNonNull(customerId);
        this.shippingAddress = Objects.requireNonNull(shippingAddress);
        this.items           = new ArrayList<>();
        this.status          = OrderStatus.PENDING;
        this.createdAt       = Instant.now();
        this.updatedAt       = this.createdAt;
    }

    public void addItem(OrderItem item) {
        if (status != OrderStatus.PENDING) {
            throw new IllegalStateException("Cannot add items to a non-PENDING order");
        }
        items.add(Objects.requireNonNull(item));
        touch();
    }

    public void confirm() {
        transition(OrderStatus.CONFIRMED);
    }

    public void startProcessing() {
        transition(OrderStatus.PROCESSING);
    }

    public void ship(String trackingNumber) {
        transition(OrderStatus.SHIPPED);
        this.trackingNumber = trackingNumber;
    }

    public void deliver() {
        transition(OrderStatus.DELIVERED);
    }

    public void cancel() {
        transition(OrderStatus.CANCELLED);
    }

    public void refund() {
        transition(OrderStatus.REFUNDED);
    }

    private void transition(OrderStatus next) {
        if (!status.canTransitionTo(next)) {
            throw new InvalidOrderTransitionException(status, next);
        }
        this.status = next;
        touch();
    }

    private void touch() { this.updatedAt = Instant.now(); }

    public Money total() {
        return items.stream()
                .map(OrderItem::subtotal)
                .reduce(Money.zero("USD"), Money::add);
    }

    public boolean isEmpty() { return items.isEmpty(); }

    // Getters
    public Long getId()                     { return id; }
    public Long getCustomerId()             { return customerId; }
    public List<OrderItem> getItems()       { return Collections.unmodifiableList(items); }
    public OrderStatus getStatus()          { return status; }
    public Address getShippingAddress()     { return shippingAddress; }
    public String getTrackingNumber()       { return trackingNumber; }
    public Instant getCreatedAt()           { return createdAt; }
    public Instant getUpdatedAt()           { return updatedAt; }
}
