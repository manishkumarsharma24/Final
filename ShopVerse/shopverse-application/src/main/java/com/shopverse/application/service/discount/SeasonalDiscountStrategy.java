package com.shopverse.application.service.discount;

import com.shopverse.domain.model.Customer;
import com.shopverse.domain.vo.Money;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.MonthDay;

/** Ch02-06: Seasonal sale discount (e.g. Black Friday 20% off). */
@Component
public class SeasonalDiscountStrategy implements DiscountStrategy {

    private static final MonthDay SALE_START = MonthDay.of(11, 25);
    private static final MonthDay SALE_END   = MonthDay.of(11, 30);
    private static final BigDecimal RATE     = new BigDecimal("0.20");

    @Override
    public Money apply(Money price, Customer customer) {
        MonthDay today = MonthDay.now();
        if (!today.isBefore(SALE_START) && !today.isAfter(SALE_END)) {
            return new Money(price.amount().multiply(BigDecimal.ONE.subtract(RATE))
                                           .setScale(2, RoundingMode.HALF_UP),
                             price.currency());
        }
        return price;
    }

    @Override
    public String name() { return "SEASONAL"; }
}
