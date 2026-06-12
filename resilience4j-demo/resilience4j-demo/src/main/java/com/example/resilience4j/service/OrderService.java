package com.example.resilience4j.service;

import com.example.resilience4j.client.PaymentServiceClient;
import com.example.resilience4j.model.PaymentDeclinedException;
import com.example.resilience4j.model.PaymentRequest;
import com.example.resilience4j.model.PaymentResponse;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.core.functions.CheckedSupplier;
import io.github.resilience4j.decorators.Decorators;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * Order Service — orchestrates payment processing with full Resilience4j stack.
 *
 * All 5 layers are applied here in the correct order:
 *
 *   Request → [RateLimiter] → [CircuitBreaker] → [Retry] → [Bulkhead] → [TimeLimiter] → PaymentService
 *
 * The order matters:
 * - RateLimiter outermost: cheapest check, stops floods first
 * - CircuitBreaker before Retry: if circuit OPEN, don't waste retries
 * - Retry before Bulkhead: each retry consumes a Bulkhead slot
 * - TimeLimiter innermost: cancels actual HTTP call, frees Bulkhead slot fast
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final PaymentServiceClient paymentServiceClient;
    private final RateLimiter rateLimiter;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final Bulkhead bulkhead;
    private final TimeLimiter timeLimiter;

    // Dedicated thread pool for async payment calls
    // TimeLimiter requires CompletableFuture — needs its own executor
    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(20);

    // =========================================================================
    // APPROACH 1: ANNOTATION-BASED (simpler, less control)
    // Use this for most use cases — Spring AOP handles the wrapping
    // =========================================================================

    /**
     * Annotation-based approach.
     * Spring AOP wraps this method with all 4 patterns automatically.
     * Order of annotation application (innermost first in Spring):
     * TimeLimiter → Bulkhead → Retry → CircuitBreaker → RateLimiter
     *
     * NOTE: Annotation order in Spring Boot is:
     * RateLimiter → CircuitBreaker → Retry → Bulkhead → TimeLimiter
     * (outermost to innermost, matching our desired layering)
     */
    @io.github.resilience4j.ratelimiter.annotation.RateLimiter(
            name = "paymentService",
            fallbackMethod = "rateLimitFallback")
    @io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker(
            name = "paymentService",
            fallbackMethod = "circuitBreakerFallback")
    @io.github.resilience4j.retry.annotation.Retry(
            name = "paymentService",
            fallbackMethod = "retryFallback")
    @io.github.resilience4j.bulkhead.annotation.Bulkhead(
            name = "paymentService",
            fallbackMethod = "bulkheadFallback")
    @io.github.resilience4j.timelimiter.annotation.TimeLimiter(
            name = "paymentService",
            fallbackMethod = "timeLimiterFallback")
    public CompletableFuture<PaymentResponse> processPaymentAnnotated(PaymentRequest request) {
        log.info("[OrderService] processPaymentAnnotated() called for orderId: {}", request.getOrderId());

        // Wrap in CompletableFuture — required by @TimeLimiter
        return CompletableFuture.supplyAsync(
                () -> paymentServiceClient.charge(request),
                scheduler
        );
    }

    // =========================================================================
    // APPROACH 2: PROGRAMMATIC (full control, explicit layering)
    // Use this when you need fine-grained control or dynamic configuration
    // =========================================================================

    /**
     * Programmatic approach using Decorators builder.
     * Explicitly wraps the payment call with all 5 layers.
     * You can see exactly what order they're applied.
     */
    public PaymentResponse processPaymentProgrammatic(PaymentRequest request) {
        log.info("[OrderService] processPaymentProgrammatic() called for orderId: {}", request.getOrderId());

        // Step 1: Define the actual payment call as a Supplier
        // This is what all the layers are protecting
        Supplier<CompletableFuture<PaymentResponse>> futureSupplier = () ->
                CompletableFuture.supplyAsync(
                        () -> paymentServiceClient.charge(request),
                        scheduler
                );

        // Step 2: Wrap with all layers using Decorators builder
        // Read from BOTTOM to TOP = innermost to outermost:
        // TimeLimiter (innermost) → Bulkhead → Retry → CircuitBreaker → RateLimiter (outermost)
        Supplier<CompletableFuture<PaymentResponse>> decorated =
                Decorators.ofSupplier(futureSupplier)
                        // Layer 5 (innermost): Time Limiter
                        // Wraps the CompletableFuture — cancels if takes > 2s
                        .withTimeLimiter(timeLimiter, scheduler)

                        // Layer 4: Bulkhead
                        // Limits concurrent executions to 10
                        .withBulkhead(bulkhead)

                        // Layer 3: Retry
                        // Retries up to 3 times on transient failures
                        .withRetry(retry, scheduler)

                        // Layer 2: Circuit Breaker
                        // Monitors failure rate — opens at 50%
                        .withCircuitBreaker(circuitBreaker)

                        // Layer 1 (outermost): Rate Limiter
                        // Limits to 100 requests/sec
                        .withRateLimiter(rateLimiter)

                        // Fallback: called when ALL layers fail
                        .withFallback(Exception.class,
                                ex -> CompletableFuture.completedFuture(
                                        determineFallback(request, ex)))
                        .decorate();

        // Step 3: Execute and get result
        try {
            return decorated.get().get(3, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("[OrderService] Unexpected error: {}", e.getMessage());
            return PaymentResponse.error(request.getOrderId(), "Unexpected error: " + e.getMessage());
        }
    }

    // =========================================================================
    // FALLBACK METHODS (Annotation-based approach)
    // Spring calls these when each layer rejects/fails
    // Method signature must match original + Exception parameter
    // =========================================================================

    /**
     * Called when Rate Limiter rejects (too many requests/sec).
     * User is sending requests too fast.
     */
    public CompletableFuture<PaymentResponse> rateLimitFallback(
            PaymentRequest request, RequestNotPermitted ex) {
        log.warn("[Fallback][RateLimit] Order {} rejected — too many requests", request.getOrderId());
        return CompletableFuture.completedFuture(
                PaymentResponse.error(request.getOrderId(),
                        "System busy — too many requests. Please retry in 1 second."));
    }

    /**
     * Called when Circuit Breaker is OPEN (service is broken).
     * Don't even attempt — return immediately.
     */
    public CompletableFuture<PaymentResponse> circuitBreakerFallback(
            PaymentRequest request, CallNotPermittedException ex) {
        log.warn("[Fallback][CircuitBreaker] Order {} blocked — circuit is OPEN. " +
                "Payment Service is DOWN.", request.getOrderId());
        return CompletableFuture.completedFuture(
                PaymentResponse.pending(request.getOrderId(),
                        "Payment Service temporarily unavailable. " +
                        "Your order is saved and payment will be retried automatically."));
    }

    /**
     * Called when ALL retry attempts are exhausted.
     * Service failed 3 times in a row.
     */
    public CompletableFuture<PaymentResponse> retryFallback(
            PaymentRequest request, Exception ex) {
        log.error("[Fallback][Retry] Order {} failed after all retry attempts. " +
                "Last error: {}", request.getOrderId(), ex.getMessage());
        return CompletableFuture.completedFuture(
                PaymentResponse.pending(request.getOrderId(),
                        "Payment processing delayed. " +
                        "Your order is safe and will be processed shortly."));
    }

    /**
     * Called when Bulkhead is full (too many concurrent calls).
     * All 10 slots are busy.
     */
    public CompletableFuture<PaymentResponse> bulkheadFallback(
            PaymentRequest request, BulkheadFullException ex) {
        log.warn("[Fallback][Bulkhead] Order {} rejected — all payment slots busy",
                request.getOrderId());
        return CompletableFuture.completedFuture(
                PaymentResponse.error(request.getOrderId(),
                        "Payment system at capacity. Please retry in a moment."));
    }

    /**
     * Called when TimeLimiter fires (call took > 2 seconds).
     * Payment Service is too slow.
     */
    public CompletableFuture<PaymentResponse> timeLimiterFallback(
            PaymentRequest request, TimeoutException ex) {
        log.warn("[Fallback][TimeLimiter] Order {} timed out after 2s",
                request.getOrderId());
        return CompletableFuture.completedFuture(
                PaymentResponse.pending(request.getOrderId(),
                        "Payment verification taking longer than expected. " +
                        "Check your order status in a few minutes."));
    }

    // =========================================================================
    // HELPER: Smart fallback for programmatic approach
    // Determines the right message based on which exception was thrown
    // =========================================================================
    private PaymentResponse determineFallback(PaymentRequest request, Exception ex) {
        log.error("[Fallback][Programmatic] Order {} failed. Exception: {} — {}",
                request.getOrderId(), ex.getClass().getSimpleName(), ex.getMessage());

        if (ex instanceof RequestNotPermitted) {
            // Rate limiter fired
            return PaymentResponse.error(request.getOrderId(),
                    "Too many requests — rate limit exceeded");

        } else if (ex instanceof CallNotPermittedException) {
            // Circuit breaker is open
            return PaymentResponse.pending(request.getOrderId(),
                    "Payment Service is DOWN — order queued for retry");

        } else if (ex instanceof BulkheadFullException) {
            // Too many concurrent calls
            return PaymentResponse.error(request.getOrderId(),
                    "System at capacity — please retry");

        } else if (ex instanceof TimeoutException) {
            // TimeLimiter fired
            return PaymentResponse.pending(request.getOrderId(),
                    "Payment timed out — will be verified shortly");

        } else if (ex instanceof PaymentDeclinedException) {
            // Payment explicitly declined — not a system issue
            return PaymentResponse.error(request.getOrderId(),
                    "Payment declined: " + ex.getMessage());

        } else {
            // All retries exhausted
            return PaymentResponse.pending(request.getOrderId(),
                    "Payment processing delayed — order is safe");
        }
    }
}
