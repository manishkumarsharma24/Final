package com.shopverse.application.service.saga;

import com.shopverse.application.command.PlaceOrderCommand;
import com.shopverse.application.service.concurrency.DistributedLockService;
import com.shopverse.application.service.idempotency.IdempotencyService;
import com.shopverse.application.service.payment.PaymentProcessorFactory;
import com.shopverse.application.usecase.order.PlaceOrderUseCase;
import com.shopverse.domain.model.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Ch09-04: Saga orchestrator — coordinates multi-step order flow with compensation.
 *
 * Steps:
 *   1. Check idempotency
 *   2. Acquire distributed lock on customer
 *   3. Place order (reserve stock)
 *   4. Process payment
 *   5. Confirm order / compensate on failure
 */
@Service
public class OrderSaga {

    private static final Logger log = LoggerFactory.getLogger(OrderSaga.class);

    private final PlaceOrderUseCase placeOrderUseCase;
    private final PaymentProcessorFactory paymentFactory;
    private final DistributedLockService lockService;
    private final IdempotencyService idempotencyService;

    public OrderSaga(PlaceOrderUseCase placeOrderUseCase,
                     PaymentProcessorFactory paymentFactory,
                     DistributedLockService lockService,
                     IdempotencyService idempotencyService) {
        this.placeOrderUseCase  = placeOrderUseCase;
        this.paymentFactory     = paymentFactory;
        this.lockService        = lockService;
        this.idempotencyService = idempotencyService;
    }

    public Order execute(PlaceOrderCommand cmd, String idempotencyKey, String paymentProvider, String paymentToken) {
        // Step 1: Idempotency check
        if (idempotencyService.isProcessed(idempotencyKey)) {
            return idempotencyService.<Order>getResult(idempotencyKey)
                    .orElseThrow(() -> new IllegalStateException("Idempotency result missing"));
        }

        String lockKey = "lock:customer:" + cmd.customerId();

        return lockService.withLock(lockKey, 5, 30, () -> {
            Order order = null;
            try {
                // Step 2: Place order (reserves stock, applies discount)
                order = placeOrderUseCase.execute(cmd);

                // Step 3: Process payment
                paymentFactory.getProcessor(paymentProvider)
                              .process(order.getId(), order.total(), paymentToken);

                // Step 4: Mark idempotent
                idempotencyService.markProcessed(idempotencyKey, order);
                log.info("Saga completed: orderId={}", order.getId());
                return order;

            } catch (Exception ex) {
                log.error("Saga failed, compensating: {}", ex.getMessage());
                // Compensation: cancel order if created
                if (order != null) {
                    order.cancel();
                    // TODO: save cancelled order, restore stock
                }
                throw ex;
            }
        });
    }
}
