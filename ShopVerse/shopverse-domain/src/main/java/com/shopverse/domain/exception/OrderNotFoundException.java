package com.shopverse.domain.exception;

import com.shopverse.domain.model.ErrorCode;

public class OrderNotFoundException extends DomainException {
    public OrderNotFoundException(Long orderId) {
        super(ErrorCode.ORDER_NOT_FOUND, "Order not found with id: " + orderId);
    }
}
