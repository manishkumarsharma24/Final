package com.example.resilience4j.controller;

import com.example.resilience4j.model.PaymentRequest;
import com.example.resilience4j.model.PaymentResponse;
import com.example.resilience4j.service.OrderService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.bulkhead.Bulkhead;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * REST Controller — exposes endpoints to test all Resilience4j patterns.
 *
 * Test endpoints:
 *
 * 1. POST /api/orders/pay               — normal payment (happy path)
 * 2. POST /api/orders/pay?timeout=true  — simulate timeout
 * 3. POST /api/orders/pay?fail=true     — simulate failure (triggers retry)
 * 4. POST /api/orders/pay?decline=true  — simulate decline (no retry)
 * 5. GET  /api/orders/status            — see current state of all patterns
 */
@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final CircuitBreaker circuitBreaker;
    private final RateLimiter rateLimiter;
    private final Retry retry;
    private final Bulkhead bulkhead;

    // =========================================================================
    // ENDPOINT 1: Normal Payment (Happy Path)
    // curl -X POST http://localhost:8080/api/orders/pay
    // =========================================================================
    @PostMapping("/pay")
    public CompletableFuture<ResponseEntity<PaymentResponse>> placeOrder(
            @RequestParam(defaultValue = "false") boolean timeout,
            @RequestParam(defaultValue = "false") boolean fail,
            @RequestParam(defaultValue = "false") boolean decline) {

        PaymentRequest request = PaymentRequest.builder()
                .orderId("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .userId("USER-123")
                .amount(999.99)
                .currency("INR")
                .simulateTimeout(timeout)    // ?timeout=true → triggers TimeLimiter
                .simulateFailure(fail)       // ?fail=true → triggers Retry + CircuitBreaker
                .simulateDecline(decline)    // ?decline=true → PaymentDeclined, no retry
                .build();

        log.info("[Controller] Received order request — orderId: {}, timeout: {}, fail: {}, decline: {}",
                request.getOrderId(), timeout, fail, decline);

        // Using annotation-based approach
        return orderService.processPaymentAnnotated(request)
                .thenApply(response -> {
                    if ("SUCCESS".equals(response.getStatus())) {
                        return ResponseEntity.ok(response);
                    } else if ("PENDING".equals(response.getStatus())) {
                        return ResponseEntity.accepted().body(response);  // 202 Accepted
                    } else {
                        return ResponseEntity.status(503).body(response); // 503 Service Unavailable
                    }
                });
    }

    // =========================================================================
    // ENDPOINT 2: Programmatic approach (same logic, different implementation)
    // curl -X POST http://localhost:8080/api/orders/pay-programmatic
    // =========================================================================
    @PostMapping("/pay-programmatic")
    public ResponseEntity<PaymentResponse> placeOrderProgrammatic(
            @RequestParam(defaultValue = "false") boolean timeout,
            @RequestParam(defaultValue = "false") boolean fail,
            @RequestParam(defaultValue = "false") boolean decline) {

        PaymentRequest request = PaymentRequest.builder()
                .orderId("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .userId("USER-456")
                .amount(1499.99)
                .currency("INR")
                .simulateTimeout(timeout)
                .simulateFailure(fail)
                .simulateDecline(decline)
                .build();

        PaymentResponse response = orderService.processPaymentProgrammatic(request);

        if ("SUCCESS".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else if ("PENDING".equals(response.getStatus())) {
            return ResponseEntity.accepted().body(response);
        } else {
            return ResponseEntity.status(503).body(response);
        }
    }

    // =========================================================================
    // ENDPOINT 3: Status Dashboard
    // See the real-time state of all Resilience4j patterns
    // curl http://localhost:8080/api/orders/status
    // =========================================================================
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();

        // Circuit Breaker state
        Map<String, Object> cbStatus = new HashMap<>();
        cbStatus.put("state", circuitBreaker.getState().name());
        cbStatus.put("failureRate", circuitBreaker.getMetrics().getFailureRate() + "%");
        cbStatus.put("successfulCalls", circuitBreaker.getMetrics().getNumberOfSuccessfulCalls());
        cbStatus.put("failedCalls", circuitBreaker.getMetrics().getNumberOfFailedCalls());
        cbStatus.put("notPermittedCalls", circuitBreaker.getMetrics().getNumberOfNotPermittedCalls());
        cbStatus.put("bufferedCalls", circuitBreaker.getMetrics().getNumberOfBufferedCalls());
        status.put("circuitBreaker", cbStatus);

        // Rate Limiter state
        Map<String, Object> rlStatus = new HashMap<>();
        rlStatus.put("availablePermissions", rateLimiter.getMetrics().getAvailablePermissions());
        rlStatus.put("numberOfWaitingThreads", rateLimiter.getMetrics().getNumberOfWaitingThreads());
        status.put("rateLimiter", rlStatus);

        // Bulkhead state
        Map<String, Object> bhStatus = new HashMap<>();
        bhStatus.put("availableConcurrentCalls", bulkhead.getMetrics().getAvailableConcurrentCalls());
        bhStatus.put("maxAllowedConcurrentCalls", bulkhead.getMetrics().getMaxAllowedConcurrentCalls());
        status.put("bulkhead", bhStatus);

        // Summary
        status.put("summary", Map.of(
                "circuitBreakerOpen", circuitBreaker.getState() == CircuitBreaker.State.OPEN,
                "rateLimitAvailable", rateLimiter.getMetrics().getAvailablePermissions() > 0,
                "bulkheadAvailable", bulkhead.getMetrics().getAvailableConcurrentCalls() > 0
        ));

        return ResponseEntity.ok(status);
    }
}
