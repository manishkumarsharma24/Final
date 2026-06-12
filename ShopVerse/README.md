# ShopVerse — Full-Stack Spring Boot Capstone

A production-grade e-commerce platform covering every topic from the 19-chapter curriculum.
Built with Java 21, Spring Boot 3.2.5, and a full infrastructure stack.

---

## Architecture

```
┌─────────────────────────────────────────────────┐
│  shopverse-domain      (zero dependencies)       │
│  ├── model: Order, Product, Customer             │
│  ├── vo: Money, Address                          │
│  ├── event: sealed OrderEvent, ProductEvent      │
│  ├── exception hierarchy                         │
│  └── port interfaces (Repository, EventPublisher)│
├─────────────────────────────────────────────────┤
│  shopverse-application (Spring + domain)         │
│  ├── use cases: PlaceOrder, CreateProduct, ...   │
│  ├── CQRS: CommandBus, QueryBus                  │
│  ├── Saga: OrderSaga + compensation              │
│  ├── Discount strategies (Strategy pattern)      │
│  └── Payment factory + processors               │
├─────────────────────────────────────────────────┤
│  shopverse-infrastructure (adapters)             │
│  ├── JPA: entities, repositories, MapStruct      │
│  ├── Redis: session store, pub/sub, rate limit   │
│  ├── MongoDB: ReviewDocument                     │
│  ├── Cassandra: OrderActivityEntity              │
│  ├── Neo4j: ProductNode (recommendations)        │
│  ├── Elasticsearch: ProductDocument + sync       │
│  └── Kafka: producer + consumer (DLT)           │
├─────────────────────────────────────────────────┤
│  shopverse-web (API layer)                       │
│  ├── REST: ProductController, OrderController    │
│  ├── Reactive: ReactiveProductController (SSE)   │
│  ├── GraphQL: ProductGraphQLController           │
│  ├── Security: JWT + Spring Security             │
│  ├── AOP: LoggingAspect, PerformanceAspect       │
│  └── Resilience4j: circuit breaker + rate limit  │
└─────────────────────────────────────────────────┘
```

---

## Chapter → Class Mapping

| Chapter | Topic | Key Classes |
|---------|-------|-------------|
| Ch02 | Java Core / OOP | `Money`, `Address`, `Product.Builder`, `OrderStatus`, `CustomerTier`, `Result<T,E>` |
| Ch02 | Design Patterns | `DiscountStrategy`, `TierDiscountStrategy`, `PaymentProcessorFactory`, `OrderEventListener` |
| Ch03 | Spring IoC / DI | `PlaceOrderUseCase`, `SecurityConfig`, `AsyncConfig`, `CacheConfig` |
| Ch03 | Spring AOP | `LoggingAspect`, `PerformanceAspect` |
| Ch04 | JPA / Hibernate | `ProductEntity`, `OrderEntity`, `AddressEmbeddable`, `ProductMapper`, `JpaProductRepository` |
| Ch04 | Advanced JPA | `ProductSpecification`, `ProductSummary`, `JpaProductRepositoryWithSpec` |
| Ch04 | Optimistic Lock | `InventoryService` (`@Version` + `@Retryable`) |
| Ch05 | SQL / Flyway | `V1__initial_schema.sql`, `V2__partitioning_and_procedures.sql` |
| Ch05 | Stored Procs | `replenish_stock()`, `order_total()` in V2 migration |
| Ch05 | Partitioning | `order_audit` range-partitioned table in V2 migration |
| Ch06 | Redis | `RedisSessionStore`, `RedisPubSubPublisher`, `OrderCacheService` |
| Ch06 | MongoDB | `ReviewDocument`, `ReviewRepository` |
| Ch06 | Cassandra | `OrderActivityEntity`, `OrderActivityRepository` |
| Ch06 | Neo4j | `ProductNode`, `ProductGraphRepository` |
| Ch07 | REST API | `ProductController`, `OrderController`, `GlobalExceptionHandler` |
| Ch07 | DTOs | `ProductRequest`, `ProductResponse`, `OrderResponse`, `ApiResponse<T>`, `PagedResponse<T>` |
| Ch07 | JWT Security | `JwtTokenProvider`, `JwtAuthenticationFilter`, `SecurityConfig` |
| Ch07 | CQRS | `CommandBus`, `QueryBus`, `SimpleCommandBus`, `PlaceOrderCommand`, `SearchProductsQuery` |
| Ch08 | Elasticsearch | `ProductDocument`, `ProductSearchRepository`, `ProductSearchService` (autocomplete) |
| Ch08 | ES Sync | `ProductSyncService` (event-driven index updates) |
| Ch09 | Distributed Lock | `DistributedLockService` (Redisson) |
| Ch09 | Idempotency | `IdempotencyService` (Redis-backed) |
| Ch09 | Saga | `OrderSaga` (orchestration + compensation) |
| Ch10 | i18n | `I18nConfig`, `messages*.properties` |
| Ch11 | Async / Virtual | `AsyncConfig` (Java 21 virtual threads), `OrderEventListener` (`@Async`) |
| Ch11 | Reactive | `ReactiveProductController` (Mono/Flux/SSE) |
| Ch11 | Caching | `CachedProductService` (`@Cacheable/@CachePut/@CacheEvict`) |
| Ch12 | Kafka | `OrderKafkaProducer`, `OrderKafkaConsumer` (`@RetryableTopic` + DLT) |
| Ch12 | GraphQL | `schema.graphqls`, `ProductGraphQLController` |
| Ch12 | Resilience4j | `SearchController` (`@CircuitBreaker`, `@RateLimiter`) |
| Ch13 | Docker | `Dockerfile` (multi-stage, G1GC, non-root) |
| Ch13 | Docker Compose | `docker-compose.yml` (all 10 services) |
| Ch13 | JMH | `ProductSearchBenchmark`, `OrderBuilderBenchmark` |
| Ch14 | Kubernetes | `k8s/helm/shopverse/` (Deployment, Service, HPA, Ingress) |
| Ch14 | Domain Events | `OrderEvent` (sealed), `ProductEvent` (sealed), `OrderKafkaProducer` |
| Ch15 | Nginx | `nginx/nginx.conf` (upstream, rate-limit, cache, security headers) |
| Ch15 | E2E Tests | `OrderFlowIntegrationTest` (Testcontainers) |
| Ch16 | CI/CD | `.github/workflows/ci.yml` (build→test→docker→helm deploy) |
| Ch16 | Terraform | `infra/terraform/` (EKS + RDS + ElastiCache) |
| Ch16 | Prometheus | `infra/monitoring/prometheus.yml`, `alert_rules.yml` |
| Ch16 | OpenTelemetry | `ObservabilityConfig`, OTLP trace export |

---

## Quick Start

```bash
# Start all services
docker-compose up -d

# App runs at http://localhost:8080
# Grafana at http://localhost:3000 (admin/admin)
# Prometheus at http://localhost:9090

# Run benchmarks
mvn -pl benchmarks exec:java -Dexec.mainClass=org.openjdk.jmh.Main
```

## Running Tests

```bash
# Unit tests
mvn test

# Integration tests (requires Docker for Testcontainers)
mvn verify -pl shopverse-e2e-tests
```

## Deploy to Kubernetes

```bash
helm upgrade --install shopverse ./k8s/helm/shopverse \
  --namespace shopverse --create-namespace \
  --set secrets.DB_PASS=yourpass \
  --set secrets.JWT_SECRET=your-32-char-secret
```
