package com.shopverse.application.query;

public record SearchProductsQuery(
        String keyword,
        String category,
        int page,
        int size
) {}
