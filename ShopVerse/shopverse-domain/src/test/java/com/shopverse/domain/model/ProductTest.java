package com.shopverse.domain.model;

import com.shopverse.domain.exception.InsufficientInventoryException;
import com.shopverse.domain.vo.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Product domain model")
class ProductTest {

    private Product product;

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .id(1L)
                .name("Test Widget")
                .description("A fine widget")
                .price(new Money(new BigDecimal("29.99"), "USD"))
                .category("Electronics")
                .stockQuantity(50)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("is in stock when stockQuantity > 0")
    void is_in_stock() {
        assertTrue(product.isInStock());
    }

    @Test
    @DisplayName("not in stock when stockQuantity == 0")
    void not_in_stock() {
        Product empty = Product.builder()
                .name("Empty").price(new Money(BigDecimal.ONE, "USD")).stockQuantity(0).build();
        assertFalse(empty.isInStock());
    }

    @Test
    @DisplayName("reduceStock decrements quantity")
    void reduce_stock_success() {
        product.reduceStock(10);
        assertEquals(40, product.getStockQuantity());
    }

    @Test
    @DisplayName("reduceStock to zero is valid")
    void reduce_stock_to_zero() {
        product.reduceStock(50);
        assertEquals(0, product.getStockQuantity());
        assertFalse(product.isInStock());
    }

    @Test
    @DisplayName("reduceStock over available quantity throws InsufficientInventoryException")
    void reduce_stock_insufficient() {
        assertThrows(InsufficientInventoryException.class, () -> product.reduceStock(51));
    }

    @Test
    @DisplayName("replenishStock increments quantity")
    void replenish_stock() {
        product.replenishStock(100);
        assertEquals(150, product.getStockQuantity());
    }

    @Test
    @DisplayName("replenishStock with zero or negative throws IllegalArgumentException")
    void replenish_stock_invalid() {
        assertThrows(IllegalArgumentException.class, () -> product.replenishStock(0));
        assertThrows(IllegalArgumentException.class, () -> product.replenishStock(-5));
    }

    @Test
    @DisplayName("deactivate sets active to false")
    void deactivate() {
        product.deactivate();
        assertFalse(product.isActive());
    }
}
