package com.shopverse.domain.exception;

import com.shopverse.domain.model.ErrorCode;

public class ProductNotFoundException extends DomainException {
    public ProductNotFoundException(Long productId) {
        super(ErrorCode.PRODUCT_NOT_FOUND, "Product not found with id: " + productId);
    }
}
