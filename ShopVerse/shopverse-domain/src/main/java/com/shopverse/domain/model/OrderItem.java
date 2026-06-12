package com.shopverse.domain.model;

import com.shopverse.domain.vo.Money;

import java.util.Objects;

/**
 * Ch02-02: Order line-item value entity.
 */
public class OrderItem {

    private final Long productId;
    private final String productName;
    private final int quantity;
    private final Money unitPrice;

    public OrderItem(Long productId, String productName, int quantity, Money unitPrice) {
        this.productId   = Objects.requireNonNull(productId);
        this.productName = Objects.requireNonNull(productName);
        this.unitPrice   = Objects.requireNonNull(unitPrice);
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
        this.quantity    = quantity;
    }

    public Money subtotal() {
        return unitPrice.multiply(quantity);
    }

    public Long getProductId()      { return productId; }
    public String getProductName()  { return productName; }
    public int getQuantity()        { return quantity; }
    public Money getUnitPrice()     { return unitPrice; }
}
