package com.shopverse.web.controller;

import com.shopverse.application.command.PlaceOrderCommand;
import com.shopverse.application.usecase.order.GetAllOrdersUseCase;
import com.shopverse.application.usecase.order.GetOrderActivityUseCase;
import com.shopverse.application.usecase.order.GetOrdersByCustomerUseCase;
import com.shopverse.application.usecase.order.PlaceOrderUseCase;
import com.shopverse.application.usecase.order.UpdateOrderStatusUseCase;
import com.shopverse.domain.model.Order;
import com.shopverse.shared.ApiResponse;
import com.shopverse.web.dto.OrderActivityResponse;
import com.shopverse.web.dto.OrderResponse;
import com.shopverse.web.dto.PlaceOrderRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Ch07-01: Order REST controller.
 * Ch07-06: Requires authentication to place an order.
 * Ch06-03: GET /api/orders/activity/{customerId} — reads from Cassandra.
 * Ch06-04: PATCH /api/orders/{id}/confirm|process|ship|deliver|cancel — status transitions + Cassandra log.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final PlaceOrderUseCase          placeOrderUseCase;
    private final GetOrdersByCustomerUseCase getOrdersByCustomerUseCase;
    private final GetAllOrdersUseCase        getAllOrdersUseCase;
    private final GetOrderActivityUseCase    getOrderActivityUseCase;
    private final UpdateOrderStatusUseCase   updateOrderStatusUseCase;

    public OrderController(PlaceOrderUseCase placeOrderUseCase,
                           GetOrdersByCustomerUseCase getOrdersByCustomerUseCase,
                           GetAllOrdersUseCase getAllOrdersUseCase,
                           GetOrderActivityUseCase getOrderActivityUseCase,
                           UpdateOrderStatusUseCase updateOrderStatusUseCase) {
        this.placeOrderUseCase          = placeOrderUseCase;
        this.getOrdersByCustomerUseCase = getOrdersByCustomerUseCase;
        this.getAllOrdersUseCase         = getAllOrdersUseCase;
        this.getOrderActivityUseCase    = getOrderActivityUseCase;
        this.updateOrderStatusUseCase   = updateOrderStatusUseCase;
    }

    // ── Place order ───────────────────────────────────────────────────────────

    /**
     * Place an order — writes to PostgreSQL, logs ORDER_PLACED event to Cassandra.
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<OrderResponse>> placeOrder(
            @Valid @RequestBody PlaceOrderRequest req) {
        PlaceOrderCommand cmd = new PlaceOrderCommand(
                req.customerId(),
                req.street(), req.city(), req.state(), req.postalCode(), req.country(),
                req.items().stream()
                   .map(i -> new PlaceOrderCommand.ItemRequest(i.productId(), i.quantity()))
                   .toList()
        );
        Order order = placeOrderUseCase.execute(cmd);
        return ResponseEntity.status(201)
                .body(ApiResponse.ok(OrderResponse.from(order), "Order placed successfully"));
    }

    // ── Fetch orders ─────────────────────────────────────────────────────────

    /**
     * GET /api/orders/customer/{customerId}
     * Returns all orders for the given customer, newest first.
     */
    @GetMapping("/customer/{customerId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getByCustomer(
            @PathVariable Long customerId) {
        List<OrderResponse> orders = getOrdersByCustomerUseCase.execute(customerId)
                .stream().map(OrderResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(orders));
    }

    /**
     * GET /api/orders
     * Admin: returns all orders, optionally filtered by ?status=CONFIRMED etc.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getAll(
            @RequestParam(required = false) String status) {
        List<OrderResponse> orders = getAllOrdersUseCase.execute(status)
                .stream().map(OrderResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(orders));
    }

    // ── Cassandra activity log ────────────────────────────────────────────────

    /**
     * GET /api/orders/{orderId}/activity
     * Returns activity events for a specific order from Cassandra.
     * Cassandra is partitioned by customerId, so we load the order first to
     * get the customerId, then filter events by orderId in memory.
     */
    @GetMapping("/{orderId}/activity")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<OrderActivityResponse>>> getActivityByOrder(
            @PathVariable Long orderId) {
        // Resolve customerId from PostgreSQL (Cassandra partition key), then filter by orderId in memory
        var activities = getOrderActivityUseCase.getByOrderId(
                resolveCustomerId(orderId), orderId);
        List<OrderActivityResponse> response = activities.stream()
                .map(OrderActivityResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * GET /api/orders/activity/{customerId}
     * Returns all activity events for a customer from Cassandra (newest first).
     * Optional ?since=2026-01-01T00:00:00Z filters by timestamp.
     */
    @GetMapping("/activity/{customerId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<OrderActivityResponse>>> getActivityByCustomer(
            @PathVariable Long customerId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant since) {
        var activities = since != null
                ? getOrderActivityUseCase.getRecentByCustomer(customerId, since)
                : getOrderActivityUseCase.getByCustomer(customerId);
        List<OrderActivityResponse> response = activities.stream()
                .map(OrderActivityResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /** Helper: load order from PostgreSQL to resolve its customerId. */
    private Long resolveCustomerId(Long orderId) {
        return getOrdersByCustomerUseCase.resolveCustomerIdForOrder(orderId);
    }

    // ── Status transitions (admin only) ──────────────────────────────────────

    /**
     * PATCH /api/orders/{id}/confirm — PENDING → CONFIRMED
     * Each transition logs an activity event to Cassandra.
     */
    @PatchMapping("/{id}/confirm")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> confirm(@PathVariable Long id) {
        Order order = updateOrderStatusUseCase.confirm(id);
        return ResponseEntity.ok(ApiResponse.ok(OrderResponse.from(order), "Order confirmed"));
    }

    /**
     * PATCH /api/orders/{id}/process — CONFIRMED → PROCESSING
     */
    @PatchMapping("/{id}/process")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> process(@PathVariable Long id) {
        Order order = updateOrderStatusUseCase.startProcessing(id);
        return ResponseEntity.ok(ApiResponse.ok(OrderResponse.from(order), "Order moved to processing"));
    }

    /**
     * PATCH /api/orders/{id}/ship — PROCESSING → SHIPPED
     * Body: { "trackingNumber": "TRK-123456" }
     */
    @PatchMapping("/{id}/ship")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> ship(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String trackingNumber = body != null ? body.getOrDefault("trackingNumber", "") : "";
        Order order = updateOrderStatusUseCase.ship(id, trackingNumber);
        return ResponseEntity.ok(ApiResponse.ok(OrderResponse.from(order), "Order shipped"));
    }

    /**
     * PATCH /api/orders/{id}/deliver — SHIPPED → DELIVERED
     */
    @PatchMapping("/{id}/deliver")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> deliver(@PathVariable Long id) {
        Order order = updateOrderStatusUseCase.deliver(id);
        return ResponseEntity.ok(ApiResponse.ok(OrderResponse.from(order), "Order delivered"));
    }

    /**
     * PATCH /api/orders/{id}/cancel — PENDING / CONFIRMED / PROCESSING → CANCELLED
     * Accessible by authenticated users (customer can cancel their own order, admin can cancel any).
     */
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<OrderResponse>> cancel(@PathVariable Long id) {
        Order order = updateOrderStatusUseCase.cancel(id);
        return ResponseEntity.ok(ApiResponse.ok(OrderResponse.from(order), "Order cancelled"));
    }

    /**
     * PATCH /api/orders/{id}/refund — DELIVERED → REFUNDED
     * Admin only. Publishes OrderRefunded to Kafka, which:
     *   - releases inventory (StockReleased on inventory topic)
     *   - sends a refund confirmation email via RabbitMQ (PaymentSuccessNotification)
     */
    @PatchMapping("/{id}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> refund(@PathVariable Long id) {
        Order order = updateOrderStatusUseCase.refund(id);
        return ResponseEntity.ok(ApiResponse.ok(OrderResponse.from(order), "Order refunded"));
    }

}
