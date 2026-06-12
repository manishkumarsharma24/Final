package com.shopverse.infrastructure.jpa.projection;

import java.math.BigDecimal;

/**
 * Ch04-08: Spring Data Projection — returns only id/name/price (avoids SELECT *).
 * Improves query performance for list endpoints.
 */
public interface ProductSummary {
    Long getId();
    String getName();
    BigDecimal getPrice();
    String getCategory();
    int getStockQuantity();
}
