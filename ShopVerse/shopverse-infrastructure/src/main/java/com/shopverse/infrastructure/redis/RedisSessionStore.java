package com.shopverse.infrastructure.redis;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Ch06-01: Redis as session/token store.
 * Ch06-01: Uses RedisTemplate with String keys and JSON values.
 */
@Component
public class RedisSessionStore {

    private static final String PREFIX = "session:";
    private final RedisTemplate<String, Object> redisTemplate;

    public RedisSessionStore(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void store(String token, Object principal, Duration ttl) {
        redisTemplate.opsForValue().set(PREFIX + token, principal, ttl);
    }

    public Optional<Object> get(String token) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(PREFIX + token));
    }

    public void invalidate(String token) {
        redisTemplate.delete(PREFIX + token);
    }

    /** Ch06-01: Atomic increment for rate limiting counter. */
    public long incrementHits(String key, Duration window) {
        Long count = redisTemplate.opsForValue().increment("ratelimit:" + key);
        if (count != null && count == 1) {
            redisTemplate.expire("ratelimit:" + key, window);
        }
        return count == null ? 0L : count;
    }
}
