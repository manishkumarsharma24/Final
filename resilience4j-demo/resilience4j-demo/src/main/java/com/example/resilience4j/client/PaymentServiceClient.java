package com.example.resilience4j.client;

import com.example.resilience4j.model.PaymentDeclinedException;
import com.example.resilience4j.model.PaymentRequest;
import com.example.resilience4j.model.PaymentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simulates an external Payment Service HTTP client.
 *
 * In a real app this would use RestTemplate or WebClient to call
 * an actual external service URL. Here we simulate failures
 * so you can see all 5 Resilience4j patterns in action.
 *
 * Simulated scenarios:
 * 1. simulateTimeout = true  → sleeps 5 seconds → TimeLimiter fires at 2s
 * 2. simulateFailure = true  → throws RuntimeException → Retry kicks in
 * 3. simulateDecline = true  → throws PaymentDeclinedException → NOT retried
 * 4. Normal call             → succeeds immediately
 */
@Slf4j
@Component
public class PaymentServiceClient {

    // Tracks total calls made — useful for seeing retry attempts in logs
    private final AtomicInteger callCount = new AtomicInteger(0);

    /**
     * The actual (simulated) HTTP call to Payment Service.
     * This is the method that all 5 Resilience4j layers protect.
     */
    public PaymentResponse charge(PaymentRequest request) {

        int attempt = callCount.incrementAndGet();
        log.info(">>> PaymentServiceClient.charge() called — attempt #{} for order {}",
                attempt, request.getOrderId());

        // -------------------------------------------------------------------
        // Simulate: TIMEOUT
        // TimeLimiter (Layer 5) will cancel this after 2 seconds
        // Retry (Layer 3) will retry up to 3 times
        // -------------------------------------------------------------------
        if (request.isSimulateTimeout()) {
            log.warn("Simulating slow Payment Service response (5s sleep)...");
            try {
                Thread.sleep(5000); // Sleep 5s — TimeLimiter cancels at 2s
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Thread interrupted (TimeLimiter cancelled the call)");
                throw new RuntimeException("Payment Service timed out");
            }
        }

        // -------------------------------------------------------------------
        // Simulate: HARD FAILURE
        // This is a transient error — Retry will attempt 3 times
        // Circuit Breaker counts this as a failure
        // -------------------------------------------------------------------
        if (request.isSimulateFailure()) {
            log.error("Simulating Payment Service failure (connection refused)...");
            throw new RuntimeException("Payment Service connection refused — attempt #" + attempt);
        }

        // -------------------------------------------------------------------
        // Simulate: PAYMENT DECLINED
        // This is NOT a transient error — Retry will NOT attempt again
        // (PaymentDeclinedException is in ignore-exceptions list)
        // Circuit Breaker DOES count this as a failure
        // -------------------------------------------------------------------
        if (request.isSimulateDecline()) {
            log.error("Simulating payment declined by bank...");
            throw new PaymentDeclinedException("Payment declined: Insufficient funds for order " + request.getOrderId());
        }

        // -------------------------------------------------------------------
        // Happy path — payment succeeds
        // -------------------------------------------------------------------
        String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("<<< Payment SUCCESS — txnId: {}, orderId: {}", transactionId, request.getOrderId());

        return PaymentResponse.success(request.getOrderId(), transactionId);
    }
}
