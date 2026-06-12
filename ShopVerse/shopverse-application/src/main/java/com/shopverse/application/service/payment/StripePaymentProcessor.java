package com.shopverse.application.service.payment;

import com.shopverse.domain.vo.Money;
import org.springframework.stereotype.Component;

/** Ch02-06: Concrete payment processor — Stripe. */
@Component("stripe")
public class StripePaymentProcessor implements PaymentProcessor {
    @Override
    public String process(Long orderId, Money amount, String token) {
        // In real code: call Stripe SDK
        return "stripe_txn_" + orderId + "_" + System.currentTimeMillis();
    }
    @Override
    public String providerName() { return "stripe"; }
}
