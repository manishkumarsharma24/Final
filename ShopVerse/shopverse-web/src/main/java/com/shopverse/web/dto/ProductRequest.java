package com.shopverse.web.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import jakarta.validation.constraints.Min;

/** Ch07-01: Request DTO with Bean Validation (Ch03-08). */
public record ProductRequest(
        @NotBlank String name,
        String description,
        @NotNull @DecimalMin("0.01") BigDecimal price,
        @NotBlank @Size(min = 3, max = 3) String currency,
        String category,
        @Min(0) int stockQuantity
) {}
