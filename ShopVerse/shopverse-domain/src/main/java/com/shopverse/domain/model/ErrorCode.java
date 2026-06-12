package com.shopverse.domain.model;

/**
 * Ch02-05: Typed error codes used across exception hierarchy and API responses.
 */
public enum ErrorCode {
    // Order domain
    ORDER_NOT_FOUND("ORD-001"),
    ORDER_INVALID_TRANSITION("ORD-002"),
    ORDER_EMPTY("ORD-003"),

    // Product domain
    PRODUCT_NOT_FOUND("PRD-001"),
    PRODUCT_OUT_OF_STOCK("PRD-002"),
    INSUFFICIENT_INVENTORY("PRD-003"),

    // Customer domain
    CUSTOMER_NOT_FOUND("CST-001"),
    CUSTOMER_ALREADY_EXISTS("CST-002"),

    // Payment domain
    PAYMENT_FAILED("PAY-001"),
    PAYMENT_ALREADY_PROCESSED("PAY-002"),

    // Generic
    VALIDATION_ERROR("GEN-001"),
    INTERNAL_ERROR("GEN-002"),
    UNAUTHORIZED("SEC-001"),
    FORBIDDEN("SEC-002");

    private final String code;

    ErrorCode(String code) { this.code = code; }

    public String getCode() { return code; }
}
