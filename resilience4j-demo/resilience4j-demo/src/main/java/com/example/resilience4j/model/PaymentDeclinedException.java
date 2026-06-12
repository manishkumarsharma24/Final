package com.example.resilience4j.model;

/**
 * Thrown when payment is explicitly declined (wrong card, insufficient funds).
 *
 * IMPORTANT: This exception is in the "ignore-exceptions" list in application.yml
 * meaning Resilience4j will NOT retry on this exception.
 *
 * Why? Because retrying a declined payment is pointless — it will always decline.
 * Retrying is only useful for transient failures (timeout, network blip).
 */
public class PaymentDeclinedException extends RuntimeException {

    public PaymentDeclinedException(String message) {
        super(message);
    }
}
