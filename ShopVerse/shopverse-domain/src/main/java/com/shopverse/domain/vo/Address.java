package com.shopverse.domain.vo;

import java.util.Objects;

/**
 * Ch02-03: Immutable Value Object — postal address.
 */
public record Address(
        String street,
        String city,
        String state,
        String postalCode,
        String country
) {
    public Address {
        Objects.requireNonNull(street, "street");
        Objects.requireNonNull(city, "city");
        Objects.requireNonNull(postalCode, "postalCode");
        Objects.requireNonNull(country, "country");
        if (country.length() != 2) {
            throw new IllegalArgumentException("country must be ISO-3166-1 alpha-2 (2 chars)");
        }
    }

    public String formatted() {
        return String.join(", ", street, city,
                state != null ? state : "", postalCode, country);
    }
}
