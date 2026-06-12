package com.example.resilience4j.config;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 * Resilience4j Java Configuration.
 *
 * NOTE: You can also configure everything in application.yml (which we also have).
 * This class shows the programmatic approach — useful when you need dynamic config
 * or want to add event listeners to see exactly what's happening.
 *
 * The beans defined here OVERRIDE the application.yml config for "paymentService".
 * Comment out this class to use application.yml config instead.
 */
@Slf4j
@Configuration
public class Resilience4jConfig {

    private static final String SERVICE_NAME = "paymentService";

    // =====================================================================
    // LAYER 1: RATE LIMITER
    // First gate — limits how many requests per second enter the system
    // =====================================================================
    @Bean
    public RateLimiter paymentRateLimiter(RateLimiterRegistry registry) {

        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(100)                          // 100 requests allowed
                .limitRefreshPeriod(Duration.ofSeconds(1))    // refreshed every second
                .timeoutDuration(Duration.ofMillis(0))        // 0 = reject immediately, don't queue
                .build();

        RateLimiter rateLimiter = registry.rateLimiter(SERVICE_NAME, config);

        // EVENT LISTENER — log every rate limit event
        rateLimiter.getEventPublisher()
                .onSuccess(event ->
                        log.debug("[RateLimiter] Request PERMITTED — available permits: {}",
                                rateLimiter.getMetrics().getAvailablePermissions()))
                .onFailure(event ->
                        log.warn("[RateLimiter] Request REJECTED — rate limit exceeded"));

        return rateLimiter;
    }

    // =====================================================================
    // LAYER 2: CIRCUIT BREAKER
    // Monitors failure rate — trips open when service is clearly broken
    // Prevents hammering a failing service
    // =====================================================================
    @Bean
    public CircuitBreaker paymentCircuitBreaker(CircuitBreakerRegistry registry) {

        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)                        // evaluate last 10 calls
                .failureRateThreshold(50)                     // open if 50%+ fail
                .waitDurationInOpenState(Duration.ofSeconds(30)) // stay open 30s
                .permittedNumberOfCallsInHalfOpenState(3)     // probe with 3 calls
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                // What counts as a failure? (TimeoutException from TimeLimiter counts too)
                .recordExceptions(Exception.class)
                .build();

        CircuitBreaker circuitBreaker = registry.circuitBreaker(SERVICE_NAME, config);

        // EVENT LISTENERS — log every state transition
        circuitBreaker.getEventPublisher()
                .onStateTransition(event ->
                        log.warn("[CircuitBreaker] STATE CHANGE: {} → {}",
                                event.getStateTransition().getFromState(),
                                event.getStateTransition().getToState()))
                .onSuccess(event ->
                        log.debug("[CircuitBreaker] Call succeeded in {}ms",
                                event.getElapsedDuration().toMillis()))
                .onError(event ->
                        log.error("[CircuitBreaker] Call FAILED — failure rate: {}%",
                                circuitBreaker.getMetrics().getFailureRate()))
                .onCallNotPermitted(event ->
                        log.warn("[CircuitBreaker] Call BLOCKED — circuit is OPEN"));

        return circuitBreaker;
    }

    // =====================================================================
    // LAYER 3: RETRY
    // Retries on transient failures — inside Circuit Breaker so each
    // retry attempt counts toward the Circuit Breaker's failure rate
    // =====================================================================
    @Bean
    public Retry paymentRetry(RetryRegistry registry) {

        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)                               // try up to 3 times
                .waitDuration(Duration.ofMillis(500))         // 500ms between retries
                // Only retry on these — transient errors worth retrying
                .retryExceptions(
                        TimeoutException.class,               // from TimeLimiter
                        RuntimeException.class)               // connection issues
                // Never retry on these — permanent errors, retrying is pointless
                .ignoreExceptions(
                        com.example.resilience4j.model.PaymentDeclinedException.class)
                .build();

        Retry retry = registry.retry(SERVICE_NAME, config);

        // EVENT LISTENERS — log every retry attempt
        retry.getEventPublisher()
                .onRetry(event ->
                        log.warn("[Retry] Attempt #{} failed — retrying in 500ms. Cause: {}",
                                event.getNumberOfRetryAttempts(),
                                event.getLastThrowable().getMessage()))
                .onSuccess(event ->
                        log.info("[Retry] Succeeded after {} attempt(s)",
                                event.getNumberOfRetryAttempts()))
                .onError(event ->
                        log.error("[Retry] ALL {} attempts failed — giving up",
                                event.getNumberOfRetryAttempts()));

        return retry;
    }

    // =====================================================================
    // LAYER 4: BULKHEAD
    // Limits concurrent calls — thread pool isolation
    // Prevents Payment Service slowness from consuming all threads
    // =====================================================================
    @Bean
    public Bulkhead paymentBulkhead(BulkheadRegistry registry) {

        BulkheadConfig config = BulkheadConfig.custom()
                .maxConcurrentCalls(10)                       // max 10 simultaneous calls
                .maxWaitDuration(Duration.ofMillis(100))      // wait 100ms for a free slot
                .build();

        Bulkhead bulkhead = registry.bulkhead(SERVICE_NAME, config);

        // EVENT LISTENERS
        bulkhead.getEventPublisher()
                .onCallPermitted(event ->
                        log.debug("[Bulkhead] Call permitted — available slots: {}",
                                bulkhead.getMetrics().getAvailableConcurrentCalls()))
                .onCallRejected(event ->
                        log.warn("[Bulkhead] Call REJECTED — all {} slots busy",
                                bulkhead.getMetrics().getMaxAllowedConcurrentCalls()))
                .onCallFinished(event ->
                        log.debug("[Bulkhead] Call finished — available slots: {}",
                                bulkhead.getMetrics().getAvailableConcurrentCalls()));

        return bulkhead;
    }

    // =====================================================================
    // LAYER 5: TIME LIMITER (innermost)
    // Cancels calls that take longer than 2 seconds
    // Closest to the actual HTTP call
    // =====================================================================
    @Bean
    public TimeLimiter paymentTimeLimiter(TimeLimiterRegistry registry) {

        TimeLimiterConfig config = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(2))       // 2 second max
                .cancelRunningFuture(true)                    // actually cancel HTTP call
                .build();

        return registry.timeLimiter(SERVICE_NAME, config);
    }
}
