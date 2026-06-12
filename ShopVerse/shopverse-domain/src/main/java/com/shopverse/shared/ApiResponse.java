package com.shopverse.shared;

import com.shopverse.domain.model.ErrorCode;

import java.time.Instant;

/**
 * Ch07-01: Generic REST response wrapper — uniform API contract.
 * Ch02-01: Demonstrates bounded generics + factory methods.
 */
public record ApiResponse<T>(
        boolean success,
        T data,
        String message,
        String errorCode,
        Instant timestamp
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, null, Instant.now());
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, data, message, null, Instant.now());
    }

    public static <T> ApiResponse<T> error(ErrorCode code, String message) {
        return new ApiResponse<>(false, null, message, code.getCode(), Instant.now());
    }

    public static <T> ApiResponse<T> error(String errorCode, String message) {
        return new ApiResponse<>(false, null, message, errorCode, Instant.now());
    }
}
