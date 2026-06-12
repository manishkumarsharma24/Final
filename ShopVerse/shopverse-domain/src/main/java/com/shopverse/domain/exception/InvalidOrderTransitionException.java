package com.shopverse.domain.exception;

import com.shopverse.domain.model.ErrorCode;
import com.shopverse.domain.model.OrderStatus;

public class InvalidOrderTransitionException extends DomainException {
    public InvalidOrderTransitionException(OrderStatus from, OrderStatus to) {
        super(ErrorCode.ORDER_INVALID_TRANSITION,
              String.format("Cannot transition order from %s to %s", from, to));
    }
}
