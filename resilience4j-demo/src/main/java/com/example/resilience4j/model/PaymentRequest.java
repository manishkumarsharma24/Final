package com.example.resilience4j.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a payment request sent to the Payment Service.
 * Contains all information needed to process a payment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    private String orderId;       // unique order identifier
    private String userId;        // who is paying
    private double amount;        // amount in INR/USD
    private String currency;      // "INR", "USD" etc.

    // Simulate failure scenarios for demo purposes
    // In real life this would not be here
    private boolean simulateTimeout;   // force a slow response
    private boolean simulateFailure;   // force a hard failure
    private boolean simulateDecline;   // force payment declined
}
