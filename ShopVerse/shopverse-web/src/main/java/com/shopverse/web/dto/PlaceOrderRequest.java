package com.shopverse.web.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public record PlaceOrderRequest(
        @NotNull Long customerId,
        @NotBlank String street,
        @NotBlank String city,
        String state,
        @NotBlank String postalCode,
        @NotBlank @Size(min=2, max=2) String country,
        @NotEmpty List<ItemRequest> items
) {
    public record ItemRequest(
            @NotNull Long productId,
            @Min(1) int quantity) {}
}
