package com.shopverse.domain.exception;

import com.shopverse.domain.model.ErrorCode;

public class CustomerNotFoundException extends DomainException {
    public CustomerNotFoundException(Long customerId) {
        super(ErrorCode.CUSTOMER_NOT_FOUND, "Customer not found with id: " + customerId);
    }
}
