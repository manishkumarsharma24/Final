package com.shopverse.web.dto;

import com.shopverse.domain.model.Product;
import java.math.BigDecimal;

/** Ch07-01: Response DTO — shields domain model from API layer. */
public record ProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        String currency,
        int stockQuantity,
        String category,
        boolean active
) {
    public static ProductResponse from(Product p) {
        return new ProductResponse(
                p.getId(), p.getName(), p.getDescription(),
                p.getPrice().amount(), p.getPrice().currency(),
                p.getStockQuantity(), p.getCategory(), p.isActive());
    }
}
