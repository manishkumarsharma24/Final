package com.shopverse.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.shopverse.domain.model.Customer;
import com.shopverse.domain.model.Order;
import com.shopverse.domain.model.OrderItem;
import com.shopverse.domain.model.Product;
import com.shopverse.domain.vo.Address;
import com.shopverse.domain.vo.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;

/**
 * Ch11-05: Redis-backed Spring Cache — per-cache TTLs.
 * Ch06-01: Redis as cache store.
 *
 * Jackson MixIns — teach Jackson how to deserialize domain classes without
 * modifying them. Product uses a private Builder constructor (no default
 * constructor), and Money is an immutable record — both need hints.
 */
@Configuration
public class CacheConfig implements CachingConfigurer {

    // ── MixIn: tells Jackson to use Product.Builder for deserialization ────────
    @JsonDeserialize(builder = Product.Builder.class)
    abstract static class ProductMixIn {}

    // ── MixIn: tells Jackson builder methods have no prefix (name(), not setName())
    @JsonPOJOBuilder(withPrefix = "")
    abstract static class ProductBuilderMixIn {}

    // ── MixIn: tells Jackson how to construct the Money record ────────────────
    // @JsonIgnoreProperties — ignores the "@class" field that default typing
    // injects into nested objects; Money's record constructor only needs amount + currency
    @JsonIgnoreProperties(ignoreUnknown = true)
    abstract static class MoneyMixIn {
        @JsonCreator
        MoneyMixIn(@JsonProperty("amount") BigDecimal amount,
                   @JsonProperty("currency") String currency) {}
    }

    // ── MixIn: Address record (used in Order + Customer) ─────────────────────
    @JsonIgnoreProperties(ignoreUnknown = true)
    abstract static class AddressMixIn {
        @JsonCreator
        AddressMixIn(@JsonProperty("street")     String street,
                     @JsonProperty("city")        String city,
                     @JsonProperty("state")       String state,
                     @JsonProperty("postalCode")  String postalCode,
                     @JsonProperty("country")     String country) {}
    }

    // ── MixIn: Customer (4-arg constructor, remaining fields via setters) ─────
    @JsonIgnoreProperties(ignoreUnknown = true)
    abstract static class CustomerMixIn {
        @JsonCreator
        CustomerMixIn(@JsonProperty("id")        Long id,
                      @JsonProperty("firstName")  String firstName,
                      @JsonProperty("lastName")   String lastName,
                      @JsonProperty("email")      String email) {}
    }

    // ── MixIn: OrderItem (all-arg constructor, no setters) ────────────────────
    @JsonIgnoreProperties(ignoreUnknown = true)
    abstract static class OrderItemMixIn {
        @JsonCreator
        OrderItemMixIn(@JsonProperty("productId")   Long productId,
                       @JsonProperty("productName") String productName,
                       @JsonProperty("quantity")    int quantity,
                       @JsonProperty("unitPrice")   Money unitPrice) {}
    }

    // ── MixIn: Order (3-arg constructor; status/items restored separately) ────
    @JsonIgnoreProperties(ignoreUnknown = true)
    abstract static class OrderMixIn {
        @JsonCreator
        OrderMixIn(@JsonProperty("id")              Long id,
                   @JsonProperty("customerId")      Long customerId,
                   @JsonProperty("shippingAddress") Address shippingAddress) {}
    }

    // ── ObjectMapper configured for Redis ─────────────────────────────────────
    private ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // WRAPPER_ARRAY: every non-final type (including List/ArrayList) is stored as
        // ["fully.qualified.ClassName", value] so the round-trip through Redis always
        // finds the type-id string at array position 0.
        //
        // As.PROPERTY was previously used but Jackson 2.15 writes List<T> as [{"@class":"...T",...}]
        // (type on each element, NO outer ArrayList wrapper). On read, AsArrayTypeDeserializer
        // expects a string type-id at position 0 and crashes with MismatchedInputException.
        mapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType(Object.class)
                        .build(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.WRAPPER_ARRAY
        );

        // Register MixIns — no Jackson annotations needed in domain layer
        mapper.addMixIn(Product.class,         ProductMixIn.class);
        mapper.addMixIn(Product.Builder.class, ProductBuilderMixIn.class);
        mapper.addMixIn(Money.class,           MoneyMixIn.class);
        mapper.addMixIn(Address.class,         AddressMixIn.class);
        mapper.addMixIn(Customer.class,        CustomerMixIn.class);
        mapper.addMixIn(OrderItem.class,       OrderItemMixIn.class);
        mapper.addMixIn(Order.class,           OrderMixIn.class);

        // Don't fail on unknown properties globally — handles @class and other type metadata
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        // Register JavaTimeModule, RecordNamingStrategyModule, etc.
        mapper.findAndRegisterModules();

        return mapper;
    }

    /**
     * Swallow Redis deserialization / connection errors gracefully.
     *
     * GET errors → treated as a cache miss; Spring falls back to the real method,
     *              which re-populates Redis with the correct format automatically.
     * PUT/EVICT/CLEAR errors → logged and ignored; the DB remains the source of truth.
     *
     * This means a single FLUSHDB clears stale data, and subsequent requests
     * self-heal without any manual intervention or app restarts.
     */
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            private final Logger log = LoggerFactory.getLogger(CacheConfig.class);

            @Override
            public void handleCacheGetError(RuntimeException e, Cache cache, Object key) {
                log.warn("[Cache] GET error — cache={} key={} ({}); treating as miss",
                        cache.getName(), key, e.getMessage());
                // do NOT rethrow — Spring treats this as a cache miss → fetches from DB
            }

            @Override
            public void handleCachePutError(RuntimeException e, Cache cache, Object key, Object value) {
                log.warn("[Cache] PUT error — cache={} key={}: {}", cache.getName(), key, e.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException e, Cache cache, Object key) {
                log.warn("[Cache] EVICT error — cache={} key={}: {}", cache.getName(), key, e.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException e, Cache cache) {
                log.warn("[Cache] CLEAR error — cache={}: {}", cache.getName(), e.getMessage());
            }
        };
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        GenericJackson2JsonRedisSerializer serializer =
                new GenericJackson2JsonRedisSerializer(redisObjectMapper());

        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(serializer));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(Map.of(
                        "products",      defaults.entryTtl(Duration.ofMinutes(30)),
                        "products-list", defaults.entryTtl(Duration.ofMinutes(30)),
                        "customers",     defaults.entryTtl(Duration.ofMinutes(60)),
                        "orders",        defaults.entryTtl(Duration.ofMinutes(5))
                ))
                .build();
    }
}
