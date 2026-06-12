package com.shopverse.application.service.cache;

import com.shopverse.domain.model.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Ch11-05: Manual Redis cache — stores serialized Order for 5 min.
 * Used when @Cacheable is insufficient (e.g. partial updates, conditional TTL).
 */
@Service
public class OrderCacheService {

    private static final String PREFIX = "order:";
    private static final Duration TTL  = Duration.ofMinutes(5);

    private final RedisTemplate<String, Object> redisTemplate;

    public OrderCacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void put(Order order) {
        redisTemplate.opsForValue().set(PREFIX + order.getId(), order, TTL);
    }

    @SuppressWarnings("unchecked")
    public Optional<Order> get(Long orderId) {
        Object val = redisTemplate.opsForValue().get(PREFIX + orderId);
        return Optional.ofNullable(val instanceof Order o ? o : null);
    }

    public void evict(Long orderId) {
        redisTemplate.delete(PREFIX + orderId);
    }
}
