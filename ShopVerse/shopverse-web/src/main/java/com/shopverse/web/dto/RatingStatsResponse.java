package com.shopverse.web.dto;

/** Response DTO for aggregated rating stats. */
public record RatingStatsResponse(
        Long productId,
        Double averageRating,
        Long reviewCount
) {}
