package com.shopverse.domain.model;

import com.shopverse.domain.exception.InsufficientInventoryException;
import com.shopverse.domain.vo.Money;

import java.util.Objects;

/**
 * Ch02-02: Product domain entity with encapsulation and invariant enforcement.
 * Ch03-02: Demonstrates builder pattern.
 */
public class Product {

    private final Long id;
    private String name;
    private String description;
    private Money price;
    private int stockQuantity;
    private String category;
    private boolean active;

    private Product(Builder builder) {
        this.id            = builder.id;
        this.name          = Objects.requireNonNull(builder.name, "name");
        this.description   = builder.description;
        this.price         = Objects.requireNonNull(builder.price, "price");
        this.stockQuantity = builder.stockQuantity;
        this.category      = builder.category;
        this.active        = builder.active;
    }

    /** Ch03-02: Static factory method — required for MapStruct builder auto-detection. */
    public static Builder builder() { return new Builder(); }

    /** Ch03-02: Static nested Builder. */
    public static class Builder {
        private Long id;
        private String name;
        private String description;
        private Money price;
        private int stockQuantity;
        private String category;
        private boolean active = true;

        public Builder id(Long id)                      { this.id = id; return this; }
        public Builder name(String name)                { this.name = name; return this; }
        public Builder description(String desc)         { this.description = desc; return this; }
        public Builder price(Money price)               { this.price = price; return this; }
        public Builder stockQuantity(int qty)           { this.stockQuantity = qty; return this; }
        public Builder category(String cat)             { this.category = cat; return this; }
        public Builder active(boolean active)           { this.active = active; return this; }
        public Product build()                          { return new Product(this); }
    }

    public void reduceStock(int quantity) {
        if (quantity > stockQuantity) {
            throw new InsufficientInventoryException(id, quantity, stockQuantity);
        }
        this.stockQuantity -= quantity;
    }

    public void replenishStock(int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("Replenish quantity must be positive");
        this.stockQuantity += quantity;
    }

    public boolean isInStock()      { return stockQuantity > 0; }

    // Getters
    public Long getId()             { return id; }
    public String getName()         { return name; }
    public String getDescription()  { return description; }
    public Money getPrice()         { return price; }
    public int getStockQuantity()   { return stockQuantity; }
    public String getCategory()     { return category; }
    public boolean isActive()       { return active; }

    // Setters (controlled mutations)
    public void setName(String name)            { this.name = Objects.requireNonNull(name); }
    public void setPrice(Money price)           { this.price = Objects.requireNonNull(price); }
    public void setDescription(String desc)     { this.description = desc; }
    public void setCategory(String category)    { this.category = category; }
    public void deactivate()                    { this.active = false; }
}
