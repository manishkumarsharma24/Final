package com.shopverse.web.dto;

import com.shopverse.domain.model.Customer;
import com.shopverse.domain.model.CustomerTier;

public record CustomerResponse(
        Long id, String firstName, String lastName,
        String email, CustomerTier tier, int loyaltyPoints) {
    public static CustomerResponse from(Customer c) {
        return new CustomerResponse(c.getId(), c.getFirstName(), c.getLastName(),
                c.getEmail(), c.getTier(), c.getLoyaltyPoints());
    }
}
