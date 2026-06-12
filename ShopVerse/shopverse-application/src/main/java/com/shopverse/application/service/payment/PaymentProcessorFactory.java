package com.shopverse.application.service.payment;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Ch02-06: Factory that resolves the correct PaymentProcessor by provider name.
 * Spring injects all PaymentProcessor beans by name into the Map.
 */
@Component
public class PaymentProcessorFactory {

    private final Map<String, PaymentProcessor> processors;

    public PaymentProcessorFactory(Map<String, PaymentProcessor> processors) {
        this.processors = processors;
    }

    public PaymentProcessor getProcessor(String provider) {
        PaymentProcessor processor = processors.get(provider.toLowerCase());
        if (processor == null) {
            throw new IllegalArgumentException("Unknown payment provider: " + provider);
        }
        return processor;
    }
}
