package com.shopverse.domain.exception;

import com.shopverse.domain.model.ErrorCode;

/** Base class for all domain-layer rule violations. */
public class DomainException extends ShopVerseException {
    public DomainException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
