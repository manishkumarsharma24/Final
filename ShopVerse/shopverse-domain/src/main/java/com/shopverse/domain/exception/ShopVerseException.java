package com.shopverse.domain.exception;

import com.shopverse.domain.model.ErrorCode;

/**
 * Ch03-06: Root exception for all ShopVerse domain errors.
 * Ch02-07: Exception chaining via cause constructor.
 */
public class ShopVerseException extends RuntimeException {

    private final ErrorCode errorCode;

    public ShopVerseException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ShopVerseException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() { return errorCode; }
}
