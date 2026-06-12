package com.shopverse.application.service.payment;

import com.shopverse.domain.vo.Money;

/** Ch02-06: Product interface for Factory pattern. */
public interface PaymentProcessor {
    String process(Long orderId, Money amount, String token);
    String providerName();
}
