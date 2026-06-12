package com.shopverse.application.command;

import java.util.List;

/**
 * Ch07-05: CQRS command — immutable record carrying write intent.
 */
public record PlaceOrderCommand(
        Long customerId,
        String street,
        String city,
        String state,
        String postalCode,
        String country,
        List<ItemRequest> items
) {
    public record ItemRequest(Long productId, int quantity) {}
}
