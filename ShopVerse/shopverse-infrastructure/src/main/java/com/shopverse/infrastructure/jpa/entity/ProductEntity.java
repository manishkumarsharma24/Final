package com.shopverse.infrastructure.jpa.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Ch04-01: JPA entity — Product table mapping.
 * Ch04-03: @Column constraints, @Index via @Table.
 * Ch05-03: Index strategy reflected in DDL (see Flyway V1).
 */
@Entity
@Table(
    name = "products",
    indexes = {
        @Index(name = "idx_products_category", columnList = "category"),
        @Index(name = "idx_products_name_trgm", columnList = "name")  // GIN trigram via Flyway
    }
)
public class ProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "products_seq")
    @SequenceGenerator(name = "products_seq", sequenceName = "products_id_seq", allocationSize = 50)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity;

    @Column(length = 100)
    private String category;

    @Column(nullable = false)
    private boolean active = true;

    @Version
    private Long version;   // Ch04-06: Optimistic locking

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    // Getters & setters
    public Long getId()                         { return id; }
    public void setId(Long id)                  { this.id = id; }
    public String getName()                     { return name; }
    public void setName(String name)            { this.name = name; }
    public String getDescription()              { return description; }
    public void setDescription(String d)        { this.description = d; }
    public BigDecimal getPrice()                { return price; }
    public void setPrice(BigDecimal price)      { this.price = price; }
    public String getCurrency()                 { return currency; }
    public void setCurrency(String currency)    { this.currency = currency; }
    public int getStockQuantity()               { return stockQuantity; }
    public void setStockQuantity(int qty)       { this.stockQuantity = qty; }
    public String getCategory()                 { return category; }
    public void setCategory(String category)    { this.category = category; }
    public boolean isActive()                   { return active; }
    public void setActive(boolean active)       { this.active = active; }
    public Long getVersion()                    { return version; }
    public Instant getCreatedAt()               { return createdAt; }
    public Instant getUpdatedAt()               { return updatedAt; }
}
