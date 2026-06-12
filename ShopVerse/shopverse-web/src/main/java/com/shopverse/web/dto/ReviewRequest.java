package com.shopverse.web.dto;

import jakarta.validation.constraints.*;
import java.util.List;

/** Request body for submitting a product review. */
public record ReviewRequest(
        @NotNull Long customerId,
        @Min(1) @Max(5) int rating,
        @NotBlank @Size(max = 120) String title,
        @NotBlank @Size(max = 2000) String body,
        List<String> tags,
        boolean verified
) {}
