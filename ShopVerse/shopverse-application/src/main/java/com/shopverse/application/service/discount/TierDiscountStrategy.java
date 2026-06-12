package com.shopverse.application.service.discount;

import com.shopverse.domain.model.Customer;
import com.shopverse.domain.vo.Money;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Ch02-06: Concrete strategy — applies tier-based percentage discount.
 */
@Component
public class TierDiscountStrategy implements DiscountStrategy {

    @Override
    public Money apply(Money price, Customer customer) {
        double rate = customer.getTier().getDiscountRate();
        if (rate == 0.0) return price;
        BigDecimal factor = BigDecimal.ONE.subtract(BigDecimal.valueOf(rate));
        return new Money(price.amount().multiply(factor).setScale(2, RoundingMode.HALF_UP),
                         price.currency());
    }

    @Override
    public String name() { return "TIER"; }
}
