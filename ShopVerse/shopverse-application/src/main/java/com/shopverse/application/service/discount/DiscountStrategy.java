package com.shopverse.application.service.discount;

import com.shopverse.domain.model.Customer;
import com.shopverse.domain.vo.Money;

/**
 * Ch02-06: Strategy pattern — pluggable discount algorithms.
 * Each CustomerTier gets its own concrete strategy.
 */
public interface DiscountStrategy {
    Money apply(Money originalPrice, Customer customer);
    String name();
}
