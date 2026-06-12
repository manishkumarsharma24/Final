package com.shopverse.infrastructure.jpa.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Ch04-04: Child entity in @OneToMany relationship.
 *
 * Bidirectional @ManyToOne owns the order_id FK column.
 * The explicit orderId field is kept as insertable=false/updatable=false
 * so toDomain() can still read the value without triggering a join.
 */
@Entity
@Table(name = "order_items")
public class OrderItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "order_items_seq")
    @SequenceGenerator(name = "order_items_seq", sequenceName = "order_items_id_seq", allocationSize = 100)
    private Long id;

    // Owning side of the FK — set via OrderEntity.addItem() so order_id is non-null on INSERT
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    // Read-only convenience field — same column, never written by Hibernate
    @Column(name = "order_id", nullable = false, insertable = false, updatable = false)
    private Long orderId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, length = 3)
    private String currency;

    // Getters & setters
    public Long getId()                         { return id; }
    public void setId(Long id)                  { this.id = id; }
    public OrderEntity getOrder()               { return order; }
    public void setOrder(OrderEntity order)     { this.order = order; }
    public Long getOrderId()                    { return orderId; }
    public Long getProductId()                  { return productId; }
    public void setProductId(Long pid)          { this.productId = pid; }
    public String getProductName()              { return productName; }
    public void setProductName(String name)     { this.productName = name; }
    public int getQuantity()                    { return quantity; }
    public void setQuantity(int qty)            { this.quantity = qty; }
    public BigDecimal getUnitPrice()            { return unitPrice; }
    public void setUnitPrice(BigDecimal price)  { this.unitPrice = price; }
    public String getCurrency()                 { return currency; }
    public void setCurrency(String currency)    { this.currency = currency; }
}
