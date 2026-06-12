package com.shopverse.application.service.concurrency;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Ch09-01: Distributed lock via Redisson — prevents concurrent inventory updates.
 * Uses Redis SETNX under the hood with automatic lease expiry.
 */
@Service
public class DistributedLockService {

    private static final Logger log = LoggerFactory.getLogger(DistributedLockService.class);
    private final RedissonClient redisson;

    public DistributedLockService(RedissonClient redisson) {
        this.redisson = redisson;
    }

    /**
     * Acquires lock, executes supplier, releases lock.
     *
     * @param lockKey    Redis key for the lock (e.g. "lock:product:42")
     * @param waitSec    max seconds to wait for the lock
     * @param leaseSec   max seconds to hold the lock
     * @param action     the critical section
     */
    public <T> T withLock(String lockKey, long waitSec, long leaseSec, Supplier<T> action) {
        RLock lock = redisson.getLock(lockKey);
        try {
            boolean acquired = lock.tryLock(waitSec, leaseSec, TimeUnit.SECONDS);
            if (!acquired) {
                throw new IllegalStateException("Could not acquire lock: " + lockKey);
            }
            log.debug("Lock acquired: {}", lockKey);
            return action.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while acquiring lock: " + lockKey, e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("Lock released: {}", lockKey);
            }
        }
    }

    public void withLock(String lockKey, long waitSec, long leaseSec, Runnable action) {
        withLock(lockKey, waitSec, leaseSec, () -> { action.run(); return null; });
    }
}
