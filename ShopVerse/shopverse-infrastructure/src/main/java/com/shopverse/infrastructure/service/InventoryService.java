package com.shopverse.infrastructure.service;

import com.shopverse.infrastructure.jpa.repository.JpaProductRepository;
import jakarta.persistence.OptimisticLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ch04-06: Optimistic locking — @Version on ProductEntity.
 * Ch09-04: @Retryable — auto-retry on OptimisticLockException.
 * Ch05-01: Uses native UPDATE with stock guard to prevent oversell.
 */
@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);
    private final JpaProductRepository productRepo;

    public InventoryService(JpaProductRepository productRepo) {
        this.productRepo = productRepo;
    }

    /**
     * Ch04-06: Retryable decrement — retries up to 3x on lock conflicts.
     */
    @Transactional
    @Retryable(retryFor = OptimisticLockException.class,
               maxAttempts = 3,
               backoff = @Backoff(delay = 100, multiplier = 2))
    public boolean decrementStock(Long productId, int quantity) {
        int updated = productRepo.decrementStock(productId, quantity);
        if (updated == 0) {
            log.warn("Insufficient stock: productId={}, requested={}", productId, quantity);
            return false;
        }
        return true;
    }

    /**
     * Ch05-01: Batch replenish — uses JDBC batch insert via JPA batch properties.
     */
    @Transactional
    public void batchReplenish(java.util.Map<Long, Integer> replenishments) {
        replenishments.forEach((productId, qty) -> {
            productRepo.findById(productId).ifPresent(p -> {
                p.setStockQuantity(p.getStockQuantity() + qty);
                productRepo.save(p);
            });
        });
    }
}
