package com.shopverse.test;

import com.shopverse.infrastructure.cassandra.OrderActivityCassandraRepository;
import com.shopverse.infrastructure.elasticsearch.ProductSearchService;
import com.shopverse.infrastructure.elasticsearch.ProductSyncService;
import com.shopverse.infrastructure.kafka.OrderKafkaProducer;
import com.shopverse.infrastructure.mongo.ReviewMongoRepository;
import com.shopverse.infrastructure.neo4j.ProductGraphRepository;
import com.shopverse.infrastructure.redis.RedisPubSubPublisher;
import com.shopverse.infrastructure.redis.RedisSessionStore;
import com.shopverse.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base class for all controller component tests.
 *
 * Strategy:
 *  - H2 in-memory database (PostgreSQL compatibility mode) replaces PostgreSQL
 *  - Hibernate ddl-auto=create-drop generates the schema from JPA entities
 *  - Flyway disabled (migrations use PG-specific SQL: pg_trgm, partitions, procedures)
 *  - spring.cache.type=simple: @Cacheable uses ConcurrentHashMap — no Redis needed
 *  - @MockBean stubs replace all external-service beans (Redis, Redisson, Mongo,
 *    Cassandra, Neo4j, Elasticsearch, Kafka) so the context starts without Docker
 *  - @Transactional rolls back each test — no teardown needed
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JwtTokenProvider jwtTokenProvider;

    // ── External-service mocks ────────────────────────────────────────────────
    // These prevent the application context from connecting to Redis, Mongo, etc.

    /** Prevents LettuceConnectionFactory from making a real Redis connection. */
    @MockBean
    protected RedisConnectionFactory redisConnectionFactory;

    /** Prevents RedisTemplate from making real Redis calls (OrderCacheService). */
    @MockBean
    @SuppressWarnings("rawtypes")
    protected RedisTemplate redisTemplate;

    /** Prevents RedissonAutoConfiguration from connecting (DistributedLockService). */
    @MockBean
    protected RedissonClient redissonClient;

    /** Replaces Spring Data MongoDB repository used by MongoReviewRepositoryAdapter. */
    @MockBean
    protected ReviewMongoRepository reviewMongoRepository;

    /** Replaces Spring Data Cassandra repository used by CassandraOrderActivityAdapter. */
    @MockBean
    protected OrderActivityCassandraRepository orderActivityCassandraRepository;

    /** Replaces Elasticsearch search service used by SearchController. */
    @MockBean
    protected ProductSearchService productSearchService;

    /** Replaces Elasticsearch sync service used by ProductController. */
    @MockBean
    protected ProductSyncService productSyncService;

    /** Replaces Kafka producer (EventPublisher port implementation). */
    @MockBean
    protected OrderKafkaProducer orderKafkaProducer;

    /** Replaces Neo4j graph repository. */
    @MockBean
    protected ProductGraphRepository productGraphRepository;

    /** Replaces Redis pub/sub publisher. */
    @MockBean
    protected RedisPubSubPublisher redisPubSubPublisher;

    /** Replaces Redis session/token store. */
    @MockBean
    protected RedisSessionStore redisSessionStore;

    // ── JWT helpers ───────────────────────────────────────────────────────────

    protected String userToken(String email) {
        return "Bearer " + jwtTokenProvider.generateToken(email, "USER");
    }

    protected String adminToken(String email) {
        return "Bearer " + jwtTokenProvider.generateToken(email, "ADMIN");
    }
}
