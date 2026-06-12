package com.shopverse.application.service.idempotency;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Ch09-03: Idempotency — prevents duplicate order placement on client retry.
 * Stores idempotency key → result in Redis with 24-hour TTL.
 */
@Service
public class IdempotencyService {

    private static final String PREFIX = "idempotency:";
    private static final Duration TTL  = Duration.ofHours(24);

    private final RedisTemplate<String, Object> redisTemplate;

    public IdempotencyService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isProcessed(String idempotencyKey) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + idempotencyKey));
    }

    public void markProcessed(String idempotencyKey, Object result) {
        redisTemplate.opsForValue().set(PREFIX + idempotencyKey, result, TTL);
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> getResult(String idempotencyKey) {
        Object val = redisTemplate.opsForValue().get(PREFIX + idempotencyKey);
        return Optional.ofNullable((T) val);
    }
}
