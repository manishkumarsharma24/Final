package com.shopverse.application.service.payment;

import com.shopverse.domain.vo.Money;
import org.springframework.stereotype.Component;

/** Ch02-06: Concrete payment processor — PayPal. */
@Component("paypal")
public class PayPalPaymentProcessor implements PaymentProcessor {
    @Override
    public String process(Long orderId, Money amount, String token) {
        return "paypal_txn_" + orderId + "_" + System.currentTimeMillis();
    }
    @Override
    public String providerName() { return "paypal"; }
}
