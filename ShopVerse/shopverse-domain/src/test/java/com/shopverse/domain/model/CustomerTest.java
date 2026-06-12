package com.shopverse.domain.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Customer domain model")
class CustomerTest {

    private Customer customer;

    @BeforeEach
    void setUp() {
        customer = new Customer(1L, "John", "Doe", "john@example.com");
    }

    @Test
    @DisplayName("new customer starts as STANDARD tier with 0 loyalty points")
    void new_customer_defaults() {
        assertEquals(CustomerTier.STANDARD, customer.getTier());
        assertEquals(0, customer.getLoyaltyPoints());
        assertTrue(customer.isActive());
    }

    @Test
    @DisplayName("getFullName returns firstName + lastName")
    void full_name() {
        assertEquals("John Doe", customer.getFullName());
    }

    @Test
    @DisplayName("addLoyaltyPoints accumulates correctly")
    void loyalty_points_accumulate() {
        customer.addLoyaltyPoints(100);
        customer.addLoyaltyPoints(200);
        assertEquals(300, customer.getLoyaltyPoints());
    }

    @Test
    @DisplayName("tier upgrades as loyalty points grow")
    void tier_upgrades() {
        // STANDARD → SILVER → GOLD → PLATINUM (thresholds defined in CustomerTier)
        customer.addLoyaltyPoints(1000);
        assertNotEquals(CustomerTier.STANDARD, customer.getTier()); // should have upgraded
    }

    @Test
    @DisplayName("deactivate sets active to false")
    void deactivate() {
        customer.deactivate();
        assertFalse(customer.isActive());
    }

    @Test
    @DisplayName("email setter updates email")
    void set_email() {
        customer.setEmail("new@example.com");
        assertEquals("new@example.com", customer.getEmail());
    }

    @Test
    @DisplayName("setEmail with null throws NullPointerException")
    void set_email_null() {
        assertThrows(NullPointerException.class, () -> customer.setEmail(null));
    }
}
