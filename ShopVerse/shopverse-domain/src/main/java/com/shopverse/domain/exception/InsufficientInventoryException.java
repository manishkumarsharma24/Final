package com.shopverse.domain.exception;

import com.shopverse.domain.model.ErrorCode;

public class InsufficientInventoryException extends DomainException {
    public InsufficientInventoryException(Long productId, int requested, int available) {
        super(ErrorCode.INSUFFICIENT_INVENTORY,
              String.format("Product %d: requested %d but only %d available", productId, requested, available));
    }
}
