package com.shopverse.application.service.discount;

import com.shopverse.domain.model.Customer;
import com.shopverse.domain.vo.Money;
import org.springframework.stereotype.Component;

@Component
public class NoDiscountStrategy implements DiscountStrategy {
    @Override
    public Money apply(Money price, Customer customer) { return price; }
    @Override
    public String name() { return "NONE"; }
}
