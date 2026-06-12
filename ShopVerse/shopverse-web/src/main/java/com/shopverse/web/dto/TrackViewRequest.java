package com.shopverse.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TrackViewRequest(
        @NotNull Long productId,
        @NotBlank String productName,
        @NotBlank String category,
        Long customerId,
        @NotBlank String sessionId,
        Long previousProductId) {
}
