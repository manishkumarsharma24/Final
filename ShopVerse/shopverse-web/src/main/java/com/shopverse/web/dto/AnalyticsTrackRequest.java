package com.shopverse.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AnalyticsTrackRequest(
        @NotBlank String eventType,    // PRODUCT_VIEW | SEARCH | ADD_TO_CART | CHECKOUT_STARTED | ORDER_CONVERTED
        @NotBlank String sessionId,
        Long productId,
        String productName,
        String category,
        Long customerId,
        String query,
        Integer resultsCount,
        Integer quantity,
        BigDecimal cartTotal,
        Integer itemCount,
        Long orderId,
        BigDecimal total) {
}
