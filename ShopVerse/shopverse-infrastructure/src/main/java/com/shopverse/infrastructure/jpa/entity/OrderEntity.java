package com.shopverse.infrastructure.jpa.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Ch04-01: Order JPA entity.
 * Ch04-04: @OneToMany with CascadeType.ALL, orphanRemoval.
 * Ch04-05: @NamedQuery for JPQL.
 */
@Entity
@Table(
    name = "orders",
    indexes = {
        @Index(name = "idx_orders_customer_id", columnList = "customer_id"),
        @Index(name = "idx_orders_status", columnList = "status")
    }
)
@NamedQueries({
    @NamedQuery(
        name  = "OrderEntity.findByCustomerId",
        query = "SELECT o FROM OrderEntity o WHERE o.customerId = :customerId ORDER BY o.createdAt DESC"
    ),
    @NamedQuery(
        name  = "OrderEntity.findByStatus",
        query = "SELECT o FROM OrderEntity o WHERE o.status = :status"
    )
})
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "orders_seq")
    @SequenceGenerator(name = "orders_seq", sequenceName = "orders_id_seq", allocationSize = 50)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    // mappedBy = "order" — the child (OrderItemEntity.order) owns the FK column.
    // This prevents Hibernate's INSERT-with-NULL then UPDATE pattern that violated NOT NULL.
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItemEntity> items = new ArrayList<>();

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "street",     column = @Column(name = "ship_street")),
        @AttributeOverride(name = "city",       column = @Column(name = "ship_city")),
        @AttributeOverride(name = "state",      column = @Column(name = "ship_state")),
        @AttributeOverride(name = "postalCode", column = @Column(name = "ship_postal_code")),
        @AttributeOverride(name = "country",    column = @Column(name = "ship_country"))
    })
    private AddressEmbeddable shippingAddress;

    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    // Getters & setters
    public Long getId()                                     { return id; }
    public void setId(Long id)                              { this.id = id; }
    public Long getCustomerId()                             { return customerId; }
    public void setCustomerId(Long cid)                     { this.customerId = cid; }
    public String getStatus()                               { return status; }
    public void setStatus(String status)                    { this.status = status; }
    public List<OrderItemEntity> getItems()                 { return items; }
    public void setItems(List<OrderItemEntity> items)       { this.items = items; }

    /** Maintains both sides of the bidirectional relationship. */
    public void addItem(OrderItemEntity item) {
        items.add(item);
        item.setOrder(this);
    }
    public AddressEmbeddable getShippingAddress()           { return shippingAddress; }
    public void setShippingAddress(AddressEmbeddable addr)  { this.shippingAddress = addr; }
    public String getTrackingNumber()                       { return trackingNumber; }
    public void setTrackingNumber(String tn)                { this.trackingNumber = tn; }
    public Long getVersion()                                { return version; }
    public Instant getCreatedAt()                           { return createdAt; }
    public Instant getUpdatedAt()                           { return updatedAt; }
}
