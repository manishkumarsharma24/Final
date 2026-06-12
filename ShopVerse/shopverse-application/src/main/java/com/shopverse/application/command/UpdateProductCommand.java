package com.shopverse.application.command;

import java.math.BigDecimal;

public record UpdateProductCommand(
        Long productId,
        String name,
        String description,
        BigDecimal price,
        String currency,
        String category,
        int stockQuantity
) {}
