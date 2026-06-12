# ShopVerse — Capstone Application Prompt
## "Build one application that uses every technology from every chapter"

---

## What You Are Building

**ShopVerse** is a production-grade e-commerce backend REST API. It is a structured monolith with clearly bounded contexts (Customer, Catalog, Orders, Inventory, Search, Notifications) that can later be split into microservices. The business logic is deliberately minimal — the purpose is to exercise every technology, pattern, and concept studied across all 19 chapters.

**Stack at a glance:** Java 21 · Spring Boot 3 · PostgreSQL · Redis · MongoDB · Cassandra · Neo4j · Elasticsearch · Kafka · Docker · Kubernetes · Nginx · GitHub Actions · AWS/GCP

---

## Non-Negotiable Rules

1. Every sub-chapter listed below **must** produce at least one named class, file, SQL script, config block, or test that demonstrates it. No sub-chapter may be skipped.
2. Every class must have a one-line comment at the top: `// Demonstrates: <Chapter> — <Sub-chapter name>`
3. DTOs are always separate from JPA entities. Controllers never return entity objects.
4. Constructor injection only — no `@Autowired` on fields.
5. Flyway manages all schema changes. `ddl-auto` is always `validate`.
6. Every build step must produce passing tests before moving to the next.
7. The final `README.md` must contain a **Chapter → Class** mapping table.

---

## Chapter 01 — Core Java

### 01-01 · Java Primitives & Type System
- Use `long` for all IDs, `BigDecimal` (never `double`) for all monetary amounts, `int` for quantities.
- Demonstrate widening/narrowing cast explicitly in `MoneyConverter.java` with a comment explaining data loss risk.
- Use `char[]` (not `String`) to hold the raw JWT secret in `JwtProperties.java` to avoid string pool retention.

### 01-02 · Collections — List
- `OrderService`: store `List<OrderItem>` as `ArrayList` (O(1) index access for item lookup by position).
- `NotificationDispatcher`: use `LinkedList<Notification>` as a queue (O(1) add/remove from head).
- Demonstrate `Collections.unmodifiableList()` wrapping the order items returned from the domain model.

### 01-03 · Collections — Set
- `ProductTag`: use `HashSet<String>` for a product's tags (O(1) contains check).
- `CategoryService`: use `TreeSet<Category>` (ordered by name) to return sorted category lists.
- Demonstrate `EnumSet` for `OrderStatus` flag checks in `OrderValidator.java`.

### 01-04 · Collections — Map
- `ShoppingCart`: backing store is `LinkedHashMap<Long, CartItem>` (insertion-ordered for display).
- `RateLimiter`: use `ConcurrentHashMap<String, AtomicLong>` for in-process per-user counters.
- `LocaleConfig`: use `EnumMap<SupportedLocale, MessageBundle>` for locale-to-bundle mapping.

### 01-05 · Collections — Queue & Deque
- `AsyncNotificationQueue`: `BlockingQueue<NotificationTask>` (ArrayBlockingQueue, bounded at 500) feeds the notification thread pool.
- `OrderProcessingPipeline`: `ArrayDeque<OrderCommand>` as a double-ended queue for command staging.
- `PriorityNotificationQueue`: `PriorityQueue<Notification>` ordered by `Notification.priority` field.

### 01-06 · Collections Internals
- Add a comment block in `CartService.java` explaining HashMap's load factor (0.75), resize threshold, and why `ConcurrentHashMap` uses segment locking instead.
- Demonstrate `HashMap` collision behaviour by writing a unit test with keys that share the same `hashCode()`.

### 01-07 · Generics
- `ApiResponse<T>`: generic wrapper for all REST responses `{ data: T, status, message }`.
- `PagedResponse<T extends Identifiable>`: bounded type parameter.
- `GenericRepository<T, ID>`: generic interface with `findById`, `save`, `delete` that all Spring Data repos extend.
- `Result<T, E extends AppException>`: sealed-class-based result type (`Success<T>` / `Failure<E>`).

### 01-08 · Exceptions
- Exception hierarchy: `ShopVerseException` (base, unchecked) → `DomainException` → `OrderNotFoundException`, `InsufficientInventoryException`, `ProductNotFoundException`.
- `PaymentException` → `PaymentDeclinedException`, `PaymentTimeoutException`.
- `GlobalExceptionHandler` (`@ControllerAdvice`) maps each exception type to an HTTP status and `ApiResponse<Void>`.
- All exceptions carry a `ErrorCode` enum value for machine-readable error identification.

### 01-09 · Java 8 Features
- Every service method returning a list uses `Stream` API: `filter`, `map`, `collect`, `reduce`, `flatMap`.
- All repository methods that can return null return `Optional<T>` — no null returns anywhere in service layer.
- Lambda and method references used throughout: `orders.stream().map(OrderMapper::toResponse)`.
- All date/time uses `java.time`: `Instant` in DB, `LocalDateTime` for display, `ZonedDateTime` for customer-facing timestamps.
- `CompletableFuture` in `NotificationService.sendAsync()`.

### 01-10 · Java 9–21 Features
- **Records**: all DTOs are Java records (`PlaceOrderRequest`, `OrderResponse`, `ProductSummary`).
- **Sealed classes + Pattern matching**: `OrderEvent` is a sealed interface; `OrderPlacedEvent`, `OrderShippedEvent`, `OrderCancelledEvent` are permitted records. `EventHandler` uses `switch` pattern matching on the sealed type.
- **Text blocks**: all multi-line SQL strings in custom `@Query` annotations use text blocks.
- **Switch expressions**: `PricingStrategy` selection uses a switch expression on `CustomerTier` enum.
- **Virtual threads (Java 21)**: configure `spring.threads.virtual.enabled=true`; document why this helps with blocking JDBC calls.
- **Sequenced collections**: use `SequencedCollection` where insertion order matters.

### 01-11 · Multithreading
- `NotificationThreadPool`: `ExecutorService` backed by a fixed thread pool of 10, with a named `ThreadFactory`.
- `InventoryReserveService`: use `synchronized` block on `productId` intern for low-level locking demo; then replace with `ReentrantLock` and document the improvement.
- `OrderCompletionTask` implements `Callable<OrderResult>`, submitted to `ExecutorService`, result retrieved via `Future.get(timeout)`.
- Demonstrate `Thread.sleep` interruption handling with proper `InterruptedException` restore.

### 01-12 · java.util.concurrent
- `AtomicLong` for `OrderIdGenerator` (sequence without synchronization).
- `CountDownLatch` in `ApplicationStartupValidator`: waits for DB, Redis, Kafka to be ready before serving traffic.
- `Semaphore` in `FlashSaleService`: limits concurrent flash-sale purchases to 10 at a time.
- `CopyOnWriteArrayList` for `EventListenerRegistry` (read-heavy, write-rare list of listeners).
- `ScheduledExecutorService` in `PartitionManagerJob` (before replacing with `@Scheduled`).

### 01-13 · JVM Internals
- Document in `Dockerfile`: `-XX:+UseG1GC -Xms512m -Xmx1g -XX:MaxGCPauseMillis=200 -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/dumps/`.
- `application-prod.yml`: document metaspace, string deduplication flags.
- `JvmMetricsEndpoint` (`@Endpoint`): custom Actuator endpoint exposing heap used/max, GC count via `ManagementFactory.getMemoryMXBean()`.
- Add a comment in `CacheConfig.java` explaining that interned strings live in metaspace, not heap.

---

## Chapter 02 — Design Patterns

### 02-01 · SOLID Principles
- **S** — `OrderService` does only order orchestration; `AuditService` does only audit writing.
- **O** — `PricingStrategy` is open for extension (add `FlashSalePricing`) without modifying `OrderService`.
- **L** — `PremiumCustomer extends Customer` can be used wherever `Customer` is expected.
- **I** — `Readable`, `Writable`, `Searchable` are separate interfaces; `ProductRepository` implements all three.
- **D** — `OrderService` depends on `PricingStrategy` interface, not `RegularPricing` concrete class.

### 02-02 · Creational Patterns
- **Singleton**: `ApplicationConfig` Spring bean — document that Spring manages the singleton scope.
- **Factory Method**: `NotificationFactory.create(NotificationType)` returns `EmailNotification`, `SmsNotification`, or `PushNotification`.
- **Abstract Factory**: `DataAccessFactory` produces either `JpaOrderRepository` or `InMemoryOrderRepository` depending on the active profile.
- **Builder**: `Order.Builder` — fluent builder; `OrderBuilder.forCustomer(id).withItem(sku, qty).withPromoCode("X").build()`.
- **Prototype**: `OrderTemplate` — a saved order template that can be `clone()`d to create a repeat order.

### 02-03 · Structural Patterns Part 1
- **Adapter**: `StripePaymentAdapter` wraps the Stripe SDK and implements the internal `PaymentGateway` interface.
- **Bridge**: `NotificationSender` (abstraction) + `NotificationChannel` (implementor) — `EmailSender` and `SmsSender` are channels; `UrgentNotification` and `RoutineNotification` are abstractions.
- **Composite**: `CategoryNode` — leaf categories and composite categories both implement `CategoryComponent`; `getTotalProductCount()` recurses.

### 02-04 · Structural Patterns Part 2
- **Decorator**: `LoggingOrderService` wraps `OrderServiceImpl` — logs method entry/exit/duration without changing business logic.
- **Facade**: `CheckoutFacade` — single call to `checkout(cartId)` internally calls `CartService`, `InventoryService`, `PaymentService`, `OrderService`, `NotificationService`.
- **Flyweight**: `CurrencySymbolCache` — a `HashMap<String, CurrencySymbol>` with shared immutable `CurrencySymbol` objects for USD, EUR, GBP etc.
- **Proxy**: `LazyProductImageProxy` — loads the full image URL only when `getImageUrl()` is first called (virtual proxy).

### 02-05 · Behavioral Patterns Part 1
- **Chain of Responsibility**: `OrderValidationChain` — `StockValidator → PriceValidator → FraudValidator → AgeRestrictionValidator`, each can pass or reject.
- **Command**: `OrderCommand` interface; `PlaceOrderCommand`, `CancelOrderCommand`, `RefundOrderCommand` are concrete commands; `OrderCommandInvoker` executes and logs them.
- **Iterator**: `ProductCatalogIterator` — custom `Iterator<Product>` that iterates pages from the DB without loading all into memory.

### 02-06 · Behavioral Patterns Part 2
- **Mediator**: `OrderEventBus` — services communicate through it rather than calling each other directly.
- **Memento**: `OrderStateMemento` — saves the state of an `Order` before a status change so it can be rolled back on failure.
- **Observer**: `OrderEventPublisher` + `InventoryListener`, `AuditListener`, `NotificationListener` — all registered and notified on `OrderPlacedEvent`.

### 02-07 · Behavioral Patterns Part 3
- **State**: `OrderStateMachine` — `PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED`; `CONFIRMED → CANCELLED`. Each state is a class implementing `OrderState`; invalid transitions throw `InvalidStateTransitionException`.
- **Strategy**: `PricingContext` holds a `PricingStrategy`; `RegularPricing`, `MemberPricing`, `FlashSalePricing` are strategies selected by `CustomerTier`.
- **Template Method**: `abstract ReportGenerator` with `generateReport()` as template method calling abstract `gatherData()`, `formatData()`, `writeOutput()`; `DailySalesReport` and `MonthlyRevenueReport` extend it.

### 02-08 · Enterprise Patterns
- **Repository**: All data access behind `Repository<T,ID>` interfaces — no direct `EntityManager` calls in service layer.
- **Unit of Work**: Document that JPA `EntityManager` / `@Transactional` is the Unit of Work; `EntityManagerUnitOfWorkDemo.java` shows explicit `persist`/`flush`/`clear` cycle.
- **DTO / Mapper**: `OrderMapper` using MapStruct (`@Mapper`) — `Order → OrderResponse`, `PlaceOrderRequest → Order`.
- **Service Layer**: `OrderApplicationService` orchestrates domain objects and repositories; no business logic in controllers.

### 02-09 · Domain-Driven Design Basics
- **Aggregate**: `Order` is the aggregate root; `OrderItem`s can only be added/removed through `Order` methods.
- **Value Objects**: `Money(amount, currency)`, `Address(street, city, postcode, country)` — immutable records with `equals`/`hashCode` by value.
- **Domain Events**: `OrderPlacedEvent`, `OrderShippedEvent`, `OrderCancelledEvent` — raised inside the aggregate, published by the application service.
- **Ubiquitous Language**: Package names (`com.shopverse.ordering`, `com.shopverse.catalog`, `com.shopverse.inventory`) reflect bounded contexts.

### 02-10 · Clean Architecture & Refactoring
- **Layers**: `domain/` (entities, value objects, domain events — zero framework deps), `application/` (use cases, ports), `infrastructure/` (JPA repos, Redis, Kafka adapters), `web/` (controllers, DTOs).
- **Ports & Adapters**: `OrderRepository` port (interface in `application/`) implemented by `JpaOrderRepository` in `infrastructure/`.
- **Refactoring**: `OrderService` starts as a 200-line god class; refactor into `PlaceOrderUseCase`, `CancelOrderUseCase`, `ShipOrderUseCase` — demonstrate the before/after with a comment.

---

## Chapter 03 — Spring Framework

### 03-01 · IoC Container & DI
- Constructor injection on every `@Service`, `@Component`, `@Repository`.
- `@Configuration` class `AppConfig` defines `@Bean` for `ObjectMapper`, `RestTemplate`, `Clock`.
- `@Qualifier("primaryDataSource")` and `@Qualifier("readDataSource")` on the two `DataSource` beans.
- `@Primary` on the routing `DataSource`; `@Conditional` bean for `MockPaymentGateway` in `test` profile.

### 03-02 · Bean Lifecycle & Scopes
- `ApplicationReadinessValidator` implements `InitializingBean` — validates all required env vars on startup.
- `KafkaProducerBean`: `@PostConstruct` warms up the producer; `@PreDestroy` flushes and closes it.
- `RequestScopedAuditContext` (`@Scope("request")`) — holds the current request's trace ID; injected via `ObjectProvider` into singleton services.
- `@Scope("prototype")` for `OrderBuilder` bean — a new builder instance per injection point.
- `BeanDefinitionRegistryPostProcessor` in `DynamicFeatureFlagRegistrar` — registers feature-flag beans dynamically.

### 03-03 · Spring AOP & Proxy
- `@LogExecutionTime` — custom annotation + `@Around` aspect that logs method name, duration, and arguments.
- `@RateLimit(perMinute=60)` — custom annotation + `@Around` aspect backed by Redis `INCR`/`EXPIRE`.
- `@Auditable` — `@AfterReturning` aspect that writes an audit entry for annotated service methods.
- `@ValidateInput` — `@Before` aspect that runs bean validation on method arguments.
- Document in `AopConfig.java`: CGLIB proxy used for classes, JDK dynamic proxy for interfaces; explain `@EnableAspectJAutoProxy(proxyTargetClass=true)`.

### 03-04 · Spring MVC & REST
- `OrderController`, `ProductController`, `CartController`, `RecommendationController`, `SearchController`.
- `@RequestMapping("/api/v1/")` on all controllers — versioned from day one.
- `ResponseEntity<ApiResponse<T>>` return type on every endpoint.
- `@Valid` on all request bodies; `BindingResult` errors mapped in `GlobalExceptionHandler`.
- Content negotiation: same endpoint serves `application/json` and `application/xml` — add `jackson-dataformat-xml`.
- `@CrossOrigin` configured globally in `WebMvcConfig`.

### 03-05 · Spring Boot Autoconfiguration & Actuator
- Custom `@ConditionalOnProperty(name="shopverse.feature.recommendations", havingValue="true")` guards the Neo4j recommendation bean.
- `ShopVerseAutoConfiguration` in a library module registered via `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
- Custom Actuator endpoint `@Endpoint(id="shopverse-health")` exposing DB replica lag, Redis ping, Kafka consumer lag.
- `management.endpoints.web.exposure.include=health,metrics,info,shopverse-health`.
- `/actuator/info` populated from `build-info.properties` (Maven `spring-boot-maven-plugin` buildInfo goal).

### 03-06 · Spring Data Repository
- `OrderRepository extends JpaRepository<Order, Long>` with:
  - `@Query` (JPQL) for orders by status + date range using text block.
  - `@EntityGraph(attributePaths={"items","items.product"})` on `findWithItemsById`.
  - `@Procedure(procedureName="sp_complete_order")` mapping.
  - `Specification<Order>` for dynamic filter (status, customerId, dateRange).
- `ProductRepository`: `findBySkuIn(Collection<String> skus)` — derived query.
- Paging: all list endpoints accept `Pageable`; return `Page<T>`.

### 03-07 · Spring Transactions
- `OrderApplicationService.placeOrder()` — `@Transactional(isolation=REPEATABLE_READ, rollbackFor=ShopVerseException.class)`.
- `PaymentService.charge()` — `@Transactional(propagation=REQUIRES_NEW)` so payment failure rolls back only payment, not the whole order.
- `AuditService.write()` — `@Transactional(propagation=NOT_SUPPORTED)` to always run outside the caller's transaction.
- `@Retryable(retryFor=CannotAcquireLockException.class, maxAttempts=3, backoff=@Backoff(delay=100))` on `placeOrder`.
- Self-invocation trap: document it in `SelfInvocationDemo.java` with a comment showing why an internal `@Transactional` call doesn't start a new transaction.

### 03-08 · Spring Security
- JWT authentication: `JwtAuthenticationFilter` → `JwtTokenProvider` (issue + validate) → `CustomerUserDetailsService`.
- OAuth2 Resource Server: `spring.security.oauth2.resourceserver.jwt.jwk-set-uri` pointing to Keycloak (docker-compose).
- CSRF disabled for stateless REST; enabled for the admin UI (document both configurations).
- Method security: `@PreAuthorize("hasRole('ADMIN')")` on `ProductController.delete()`, `@PostAuthorize("returnObject.customerId == authentication.name")` on `OrderController.getOrder()`.
- `BCryptPasswordEncoder` (strength 12) for password hashing.
- `SecurityConfig` defines filter chain with `sessionManagement(STATELESS)`.

### 03-09 · Spring Events & Integration
- `ApplicationEventPublisher` in `OrderApplicationService` publishes `OrderPlacedEvent`.
- `@EventListener(OrderPlacedEvent.class)` in `InventoryEventListener` — synchronous.
- `@Async @EventListener` in `NotificationEventListener` — asynchronous (runs on `notificationExecutor` thread pool).
- `@TransactionalEventListener(phase=AFTER_COMMIT)` in `SearchIndexListener` — only fires if the placing transaction committed.
- Spring Integration: `IntegrationFlow` that reads from a local `reports/` directory, processes CSV, and persists to DB (demonstrates file-based integration).

### 03-10 · Spring Testing
- `@ExtendWith(MockitoExtension.class)` unit tests for every service.
- `@WebMvcTest(OrderController.class)` slice test — MockMvc, `@MockBean` for services.
- `@DataJpaTest` slice test — in-memory H2, real repository queries tested.
- `@SpringBootTest(webEnvironment=RANDOM_PORT)` integration test — `TestRestTemplate`.
- `Testcontainers`: `@Container PostgreSQLContainer`, `@Container RedisContainer`, `@Container KafkaContainer` in `AbstractIntegrationTest` base class.
- `MockMvc` security test: verify that unauthenticated requests to `/api/v1/orders` return 401.

---

## Chapter 04 — JPA / Hibernate

### 04-01 · JPA Entity Lifecycle
- `EntityLifecycleDemo.java`: explicitly demonstrates all four states — `new → managed → detached → removed` — using `EntityManager` directly with comments.
- `@PostLoad`, `@PostPersist`, `@PostUpdate` callbacks on `Order` entity for audit timestamp population.

### 04-02 · Hibernate Architecture
- Comment block in `HibernateConfig.java` explaining: `SessionFactory` (one per app) → `Session` (one per request/transaction) → first-level cache (identity map within session).
- Enable `hibernate.generate_statistics=true` in dev profile; log stats via `StatisticsService` on shutdown.
- Show explicit `session.evict(entity)` and `session.clear()` usage in `BulkImportService`.

### 04-03 · Entity Mappings
- `AuditableEntity` (`@MappedSuperclass`): `createdAt`, `updatedAt` (auto via `@PrePersist`/`@PreUpdate`), `createdBy` (via `AuditorAware`).
- `@Embedded` `Address` value object in `Customer`.
- `@ElementCollection` for `Product.tags` (set of strings stored in a join table).
- `@Converter(autoApply=true)` `MoneyConverter` — converts `Money` record to/from `NUMERIC` column.
- `@Enumerated(EnumType.STRING)` for `OrderStatus`; never use `ORDINAL`.
- `@Inheritance(strategy=JOINED)` on `Promotion` → `PercentagePromotion`, `FixedAmountPromotion`.
- `@DiscriminatorColumn` + `@DiscriminatorValue` on the `Promotion` hierarchy.

### 04-04 · JPQL & Criteria API
- `OrderRepository`: named query `@NamedQuery(name="Order.byCustomerAndStatus", query="...")`.
- `@Query` with JPQL text block: orders in date range with JOIN FETCH.
- `ProductSearchRepository.search(filters)`: `CriteriaBuilder` dynamic query — adds `Predicate` only if filter value is non-null.
- `OrderStatisticsRepository`: JPQL aggregate query — `SELECT new com.shopverse.dto.OrderStats(o.status, COUNT(o), SUM(o.total)) FROM Order o GROUP BY o.status`.

### 04-05 · Lazy Loading & N+1
- `Order.items` is `@OneToMany(fetch=LAZY)` — default, do not change.
- `OrderRepository.findWithItemsById`: `@EntityGraph(attributePaths={"items","items.product"})` — single JOIN query.
- `ProductRepository.findAllWithCategory()`: `JOIN FETCH p.category` in JPQL to prevent N+1 on category.
- `@BatchSize(size=30)` on `Customer.orders` — demonstrate batch loading as an alternative.
- `N1DetectionTest.java`: Hibernate `Statistics` interceptor counts queries; assert `findAllOrders()` issues ≤ 2 queries.

### 04-06 · JPA Caching L1 & L2
- L1 (first-level): `EntityManagerCacheDemo` — shows that two `findById` calls in same transaction hit L1 second time (count queries via Statistics).
- L2 (second-level): Caffeine provider via `hibernate-jcache`; `@Cache(usage=READ_WRITE)` on `Product` and `Category`.
- Query cache: `@QueryHints(@QueryHint(name=HINT_CACHEABLE, value="true"))` on `findAll` in `CategoryRepository`.
- Spring Cache abstraction: `@Cacheable("products")` on `ProductService.findById()`; `@CacheEvict("products")` on `update()`; `@CachePut` on `create()`.

### 04-07 · Optimistic & Pessimistic Locking
- `Product.stockCount` annotated `@Version private int version` — prevents oversell via `OptimisticLockException`.
- `FlashSaleService.reserveStock()`: `@Lock(LockModeType.PESSIMISTIC_WRITE)` on repository method — SELECT FOR UPDATE.
- `OrderService.placeOrder()`: catches `OptimisticLockException`, retries via `@Retryable`.
- `DeadlockDemo.java`: shows two threads acquiring locks in opposite order — document the fix (consistent lock ordering).

### 04-08 · Dirty Checking & Flush
- `OrderService.updateStatus()`: loads entity, changes field — Hibernate dirty-checking auto-issues UPDATE at flush; no explicit `save()` needed. Add comment.
- `BulkPriceUpdateService`: sets `FlushMode.MANUAL`, updates 10k products, calls `flush()` + `clear()` every 500 rows to avoid OOM.
- `ReadOnlyProductService` uses `@Transactional(readOnly=true)` — document that Hibernate skips dirty-check snapshot for read-only sessions (performance gain).

### 04-09 · Flyway & Liquibase
- Flyway: migrations `V1__core_schema.sql` through `V8__add_partitioning_and_triggers.sql` for all PostgreSQL DDL.
- Liquibase: `db/changelog/elasticsearch-sync-tracking.xml` — tracks the `es_sync_log` table used by the CDC sync service (demonstrate that both tools can coexist in different schemas/purposes).
- `FlywayConfig.java`: `Flyway.configure().locations("classpath:db/migration").baselineOnMigrate(true)`.
- Test: `@FlywayTest` (Flyway test extensions) verifies each migration runs cleanly.

### 04-10 · JPA Performance Tuning
- `ProductListProjection`: interface projection with only `id`, `name`, `price`, `sku` — avoids loading full entity for list pages.
- `@Immutable` on `OrderAuditLog` entity — Hibernate skips dirty-check entirely.
- Batch insert: `spring.jpa.properties.hibernate.jdbc.batch_size=50`, `hibernate.order_inserts=true`, `hibernate.order_updates=true`; `BulkOrderImportService` demonstrates.
- Keyset pagination (`ProductRepository.findByIdGreaterThan(lastId, limit)`) instead of OFFSET for deep pages.
- `@Formula("(SELECT COUNT(*) FROM order_items oi WHERE oi.order_id = id)")` derived property on `Order` for item count without loading items.

---

## Chapter 05 — SQL Databases (PostgreSQL)

### 05-01 · SQL Fundamentals
- `V1__core_schema.sql`: `CREATE TABLE` for all entities with `NOT NULL`, `UNIQUE`, `CHECK`, FK constraints.
- `CHECK (status IN ('PENDING','CONFIRMED','PROCESSING','SHIPPED','DELIVERED','CANCELLED'))` on `orders.status`.
- `ALTER TABLE` demonstration in `V2__add_columns.sql` (add `discount_amount` column).
- CTEs used in `OrderStatisticsRepository` for monthly revenue rollup.
- `INSERT ... ON CONFLICT (sku) DO UPDATE SET ...` (UPSERT) in `ProductSyncService`.

### 05-02 · Indexes
- `V3__indexes.sql`:
  - B-Tree: `CREATE INDEX idx_orders_customer ON orders(customer_id)`.
  - Partial: `CREATE INDEX idx_orders_pending ON orders(customer_id) WHERE status = 'PENDING'`.
  - Covering: `CREATE INDEX idx_orders_cov ON orders(customer_id) INCLUDE (status, total, created_at)`.
  - Composite: `CREATE INDEX idx_order_items_comp ON order_items(order_id, product_id)`.
  - GIN: `CREATE INDEX idx_products_tags ON products USING GIN(tags)` (for `@> '{tag}'` queries).
  - Expression: `CREATE INDEX idx_products_lower_name ON products(LOWER(name))`.
- JPA: `@Table(indexes={@Index(columnList="customer_id"), @Index(columnList="status,created_at")})`.

### 05-03 · Query Optimization
- `EXPLAIN ANALYZE` comment blocks in `OrderRepository` above the two most critical queries.
- `QueryOptimizationNotes.md` in `docs/` — documents: seq scan vs index scan, type cast anti-pattern, function-on-column anti-pattern.
- `pg_stat_statements` noted in `DatabaseMonitoringService` as the production tool for finding slow queries.
- `ProductSearchRepository`: demonstrate fixing a slow query by removing `CAST` from the WHERE clause.

### 05-04 · Transactions & ACID
- `V4__stored_procedure.sql`: `CREATE OR REPLACE PROCEDURE sp_complete_order(p_order_id BIGINT)` — atomically updates order status, decrements stock, inserts audit row.
- Called from Spring via `@Procedure(procedureName="sp_complete_order")` in `OrderRepository`.
- `AcidDemo.java`: shows all four ACID properties with comments referencing the bank-transfer analogy.

### 05-05 · Isolation & MVCC
- `OrderApplicationService.placeOrder()`: `@Transactional(isolation=REPEATABLE_READ)`.
- `ReportService.generateDailyReport()`: `@Transactional(isolation=SERIALIZABLE)` — explain why needed.
- `MvccDemo.java`: shows `xmin`/`xmax` comment block explaining how PostgreSQL MVCC works.
- `@Retryable(retryFor=CannotSerializeTransactionException.class)` on serializable methods.
- `VacuumMonitoringService`: queries `pg_stat_user_tables` for `n_dead_tup` and alerts when bloat > threshold.

### 05-06 · Advanced SQL — Joins & Window Functions
- `CustomerLifetimeValueQuery.java`: uses `ROW_NUMBER() OVER (PARTITION BY customer_id ORDER BY total DESC)` to rank orders per customer.
- `LATERAL JOIN` in `RecentOrdersPerCustomerQuery.java`: gets the 3 most recent orders for each customer in one query.
- `SELF JOIN` in `CategoryHierarchyQuery.java`: `c1 JOIN categories c2 ON c2.id = c1.parent_id`.
- Anti-join: `SELECT * FROM products p WHERE NOT EXISTS (SELECT 1 FROM order_items oi WHERE oi.product_id = p.id)` — products never ordered.
- Running total: `SUM(total) OVER (ORDER BY created_at)` in monthly revenue trend query.
- `LAG`/`LEAD`: month-over-month revenue change in `RevenueReport`.

### 05-07 · Stored Procedures & Triggers
- `V5__procedures.sql`:
  - `sp_complete_order(order_id)` — see 05-04.
  - `fn_customer_lifetime_value(customer_id)` — returns `NUMERIC`, uses subquery with aggregation.
- `V6__triggers.sql`:
  - `trg_orders_audit` — `AFTER INSERT OR UPDATE OR DELETE ON orders FOR EACH ROW` → inserts into `order_audit_log(table_name, operation, old_data JSONB, new_data JSONB, changed_at)`.
  - `trg_prevent_status_regression` — `BEFORE UPDATE` — raises EXCEPTION if status goes backwards.
- Spring: `SimpleJdbcCall` for `fn_customer_lifetime_value`.

### 05-08 · Table Partitioning
- `V7__partitioning.sql`: convert `orders` to `PARTITION BY RANGE(created_at)` — create monthly partitions.
- `CREATE TABLE orders_default PARTITION OF orders DEFAULT`.
- `PartitionManagerJob` (`@Scheduled(cron="0 0 1 * * *")`): creates next month's partition and its index if not yet existing.
- `EXPLAIN` query on `orders WHERE created_at BETWEEN ...` — add assertion in test that `Partitions pruned` appears in the plan.

### 05-09 · Replication & HA
- `DataSourceConfig.java`: `RoutingDataSource` with `writeDataSource` (primary) and `readDataSource` (replica).
- `ReadWriteRoutingAspect`: `@Around` — sets context to `"read"` for `@Transactional(readOnly=true)`, `"write"` otherwise.
- `ReplicationLagHealthIndicator`: queries `pg_stat_replication`, alerts if lag > 50 MB.
- `application-prod.yml`: `prepareThreshold=0` for PgBouncer compatibility; HikariCP pool sizes documented.
- `docker-compose.yml` includes `postgres-primary` and `postgres-replica` with streaming replication configured.

### 05-10 · PostgreSQL vs MySQL
- `application-dev.yml`: uses PostgreSQL; add a `mysql` Spring profile with MySQL dialect and equivalent JDBC URL.
- `FlywayConfig.java`: `spring.flyway.locations=classpath:db/migration/{vendor}` — separate migration scripts for `postgresql/` and `mysql/`.
- `docs/database-choice.md`: documents why PostgreSQL was chosen (JSONB, materialized views, partitioning, no licensing cost); lists MySQL tradeoffs.
- `MySqlCompatibilityTest.java`: verifies the 5 most critical queries run on both dialects.

---

## Chapter 06 — NoSQL Databases

### 06-01 · NoSQL Types & CAP Theorem
- `docs/data-store-decisions.md`: a table listing every data store in the app with its CAP classification (CP or AP), reason for choosing it, and which use case it serves.
- `CapTheoremDemo.java`: comment block walking through the split-brain scenario for each store.

### 06-02 · Redis — Data Structures & Patterns
- **String**: `SessionTokenStore` — `SET session:{token} {userId} EX 3600`.
- **Hash**: `ShoppingCartService` — `HSET cart:{customerId} {productId} {qty}`; `HGETALL`, `HDEL`, `EXPIRE`.
- **List**: `RecentlyViewedService` — `LPUSH viewed:{customerId} {productId}`, `LTRIM viewed:{customerId} 0 9` (keep last 10).
- **Set**: `ProductTagIndex` — `SADD tag:{tagName} {productId}`; `SINTER tag:electronics tag:sale` for multi-tag filtering.
- **Sorted Set**: `ProductLeaderboard` — `ZADD leaderboard {score} {productId}`; `ZREVRANGE leaderboard 0 9` for top 10.
- **Pub/Sub**: `InventoryPubSub` — publishes `inventory:low` channel when stock < threshold; `LowStockSubscriber` listens.
- **Lua script**: `AtomicRateLimiter` — Lua script for `INCR`+`EXPIRE` atomically (prevent race in rate limiting).
- **Streams**: `OrderEventStream` — `XADD orders:events * orderId {id} status {status}`; `XREAD` consumer group.
- Spring: `RedisTemplate<String, Object>`, `StringRedisTemplate`, `@Cacheable` with `RedisCacheManager`.

### 06-03 · Cassandra — Wide Column Store
- `OrderEventLog` table: `PRIMARY KEY (order_id, event_time)` — partition key `order_id`, clustering `event_time DESC`.
- Keyspace: `shopverse` with `replication = {'class':'SimpleStrategy', 'replication_factor':1}` (dev) / `NetworkTopologyStrategy` (prod comment).
- `OrderEventRepository extends CassandraRepository<OrderEventLog, OrderEventLogKey>`.
- `OrderApplicationService.placeOrder()` appends an `ORDER_PLACED` event to Cassandra after committing the PostgreSQL transaction.
- Consistency level: `@Consistency(ConsistencyLevel.QUORUM)` for writes; `LOCAL_ONE` for reads — annotated with explanation.

### 06-04 · DynamoDB — AWS Managed NoSQL
- `DynamoDbConfig.java`: `DynamoDbClient` bean (uses `software.amazon.awssdk:dynamodb-enhanced`).
- `UserSessionTable`: Single Table Design — `PK=USER#{userId}`, `SK=SESSION#{sessionId}`, TTL attribute `expiresAt`.
- `DynamoDbSessionStore implements SessionStore` — alternative to Redis sessions for AWS deployment.
- GSI: `UserSessionTable` has a GSI on `email` for "find all sessions by email".
- `DynamoDbConfig` is guarded by `@ConditionalOnProperty("shopverse.aws.dynamodb.enabled")` — disabled in local dev.

### 06-05 · MongoDB — Document Store
- `ProductDocument`: `@Document("products")` — embeds `List<ProductSpecification>` (flexible key-value pairs) and `List<String> tags`.
- `ReviewDocument`: `@Document("reviews")` — embeds `pros: List<String>`, `cons: List<String>`, `verifiedPurchase: boolean`.
- `ProductDocumentRepository extends MongoRepository<ProductDocument, String>`.
- `MongoTemplate` aggregation: `Aggregation.newAggregation(match(...), group("productId").avg("rating").as("avgRating"), sort(DESC, "avgRating"))` — top-rated products.
- `ReviewService.getAverageRating(productId)`: uses the aggregation pipeline result.
- Text index: `@TextIndexed` on `ProductDocument.name` and `description`; `TextCriteria` for full-text search.

### 06-06 · Graph Databases — Neo4j
- `ProductNode` (`@Node("Product")`): `id`, `sku`, `name`.
- `CustomerNode` (`@Node("Customer")`): `id`, `email`.
- `PurchasedRelationship` (`@RelationshipProperties`): `orderId`, `purchasedAt`, `quantity`.
- `CustomerNode.purchasedProducts`: `@Relationship(type="PURCHASED", direction=OUTGOING) List<PurchasedRelationship>`.
- `RecommendationRepository`: `@Query("MATCH (c:Customer {id:$customerId})-[:PURCHASED]->(p:Product)<-[:PURCHASED]-(other:Customer)-[:PURCHASED]->(rec:Product) WHERE NOT (c)-[:PURCHASED]->(rec) RETURN rec, COUNT(*) AS score ORDER BY score DESC LIMIT 10")`.
- `Neo4jSyncListener`: `@Async @TransactionalEventListener(AFTER_COMMIT)` — writes `PURCHASED` relationships when an order completes.

### 06-07 · Time Series Databases
- Micrometer + Prometheus (default): `MeterRegistry` injected into `OrderApplicationService`; `Timer orderPlacementTimer`, `Counter failedPaymentCounter`, `Gauge activeCartGauge`.
- `@Timed("shopverse.product.search")` on `ProductSearchService.search()`.
- InfluxDB: `InfluxDBClient` bean (guarded by `@ConditionalOnProperty`); `InfluxDbMetricsWriter` writes business metrics (orders/minute, revenue/hour) as InfluxDB line protocol.
- `TimescaleDbConfig.java`: comment block explaining how `order_metrics` table would be created as a TimescaleDB hypertable if TimescaleDB extension were enabled.

### 06-08 · NoSQL Data Modeling Patterns
- **Embed vs Reference**: `ProductDocument` embeds `specifications` (accessed together, bounded size); `reviews` are referenced (unbounded, queried separately).
- **Bucket Pattern**: `DailyOrderBucket` MongoDB document — `{ date, orders: [...], count, totalRevenue }` — daily rollup bucket.
- **Computed Pattern**: `ProductDocument.avgRating` field updated by `ReviewService` on every new review (avoid recomputing aggregation every read).
- **Outlier Pattern**: `ProductDocument.hasMoreReviews: boolean` flag when review count exceeds 1000 — overflow stored in separate collection.
- Document the pattern choice with a comment block in each class.

### 06-09 · Choosing SQL vs NoSQL
- `docs/data-store-decisions.md` (same file as 06-01) contains a **Decision Matrix** with columns: Store, Data Model, CAP, Use Case in ShopVerse, Why Not Relational, Why Not Another NoSQL.
- `PolyglotPersistenceConfig.java`: central config class that wires all five stores; comment at top explains the polyglot design.

### 06-10 · Cloud-Native & Multi-Model Databases
- `CosmosDbConfig.java` (guarded `@ConditionalOnProperty("shopverse.azure.cosmos.enabled")`): `CosmosClient` using Cosmos DB SQL API as an alternative document store.
- `AuroraConfig.java` comment block: explains how to switch `writeDataSource` to AWS Aurora PostgreSQL serverless by changing the JDBC URL — zero code change needed.
- `CdcConfig.java`: comment block + Debezium dependency explaining how PostgreSQL `pgoutput` replication slot feeds changes to Kafka for Elasticsearch sync (see Chapter 10).

---

## Chapter 07 — Build Tools

### 07-01 · Maven Build Lifecycle & POM
- `shopverse` is a **Maven multi-module project**: `shopverse-parent` (BOM), `shopverse-domain`, `shopverse-application`, `shopverse-infrastructure`, `shopverse-web`, `shopverse-e2e-tests`.
- `pom.xml` (parent): `<packaging>pom</packaging>`, `<dependencyManagement>` BOM imports Spring Boot BOM + custom BOM.
- Maven lifecycle phases documented in `docs/build.md`: `validate → compile → test → package → verify → install → deploy`.

### 07-02 · Maven Dependency Management
- `shopverse-bom` module: centralises all third-party versions (`jjwt`, `testcontainers`, `mapstruct`, `resilience4j`).
- `<scope>test</scope>` for Testcontainers; `<scope>provided</scope>` for `lombok`.
- `<exclusions>` on `spring-boot-starter-logging` to replace with Log4j2.
- `mvn dependency:tree` output checked into `docs/dependency-tree.txt`.

### 07-03 · Maven Plugins
- `spring-boot-maven-plugin`: `repackage` goal, `buildInfo` goal (populates `/actuator/info`), `layered JAR` for Docker caching.
- `flyway-maven-plugin`: `mvn flyway:migrate` runs migrations in CI before integration tests.
- `jacoco-maven-plugin`: minimum 80% line coverage enforced via `<haltOnFailure>true</haltOnFailure>`.
- `maven-enforcer-plugin`: enforces Java 21, Maven 3.9+, no `SNAPSHOT` dependencies in `prod` profile.
- `spotbugs-maven-plugin`: static analysis; CI fails on HIGH severity findings.

### 07-04 · Maven Multi-Module Projects
- Each module has its own `pom.xml` with a single responsibility.
- `shopverse-domain`: zero framework dependencies — pure Java records and interfaces.
- `shopverse-infrastructure`: all JPA, Redis, Kafka, MongoDB adapters.
- `shopverse-web`: controllers, DTOs, Spring MVC config.
- Inter-module dependency: `shopverse-web` depends on `shopverse-application`; `shopverse-application` depends on `shopverse-domain`. No upward deps.

### 07-05 · Gradle Build Script & Tasks
- `shopverse-infrastructure` also has a `build.gradle.kts` (side-by-side with `pom.xml`) to demonstrate Gradle syntax — documented in `docs/gradle-equivalent.md`.
- Custom Gradle task `generateApiDocs` that runs after `test` and calls `openapi-generator`.

### 07-06 · Gradle Dependency Management
- `gradle/libs.versions.toml` version catalog in the Gradle demo module: `[versions]`, `[libraries]`, `[bundles]`.

### 07-07 · Gradle Multi-Module & Build Cache
- `settings.gradle.kts` `includeBuild` composite build documented.
- `build-cache { local { enabled = true } }` in `gradle.properties` — explained in `docs/build.md`.

### 07-08 · Gradle Plugins
- `shopverse-web/build.gradle.kts` applies: `id("org.springframework.boot")`, `id("io.spring.dependency-management")`, `id("jacoco")`.

### 07-09 · Maven vs Gradle Deep Comparison
- `docs/maven-vs-gradle.md`: performance comparison table, incremental build support, IDE support, learning curve — with ShopVerse's rationale for choosing Maven as primary.

### 07-10 · Build Tool CI/CD Integration
- `.github/workflows/ci.yml`: `mvn -B verify` — runs on every push.
- `mvn flyway:migrate` step before integration tests.
- `mvn jacoco:report` + upload to Codecov.
- Maven `prod` profile activated in the `release` workflow.

---

## Chapter 08 — Security

### 08-01 · TLS / HTTPS / X.509 Certificates
- `application-prod.yml`: `server.ssl.key-store`, `server.ssl.key-store-password`, `server.ssl.protocol=TLS`, `server.ssl.enabled-protocols=TLSv1.3`.
- `nginx.conf`: TLS termination at Nginx; Spring Boot listens on HTTP internally.
- `docs/tls-setup.md`: steps to generate a self-signed cert with `keytool` for dev.

### 08-02 · Authentication — Sessions, Cookies, JWT
- `JwtTokenProvider`: issues HS512 JWT with `sub` (customerId), `roles`, `iat`, `exp` (15 min access / 7 day refresh).
- `RefreshTokenService`: stores refresh token SHA256 hash in Redis with TTL; issues new access token.
- `JwtAuthenticationFilter extends OncePerRequestFilter`: extracts and validates Bearer token.
- `TokenBlacklist`: Redis `SET` of invalidated JTIs for logout.

### 08-03 · OAuth2 Flows
- `spring-security-oauth2-resource-server`: configures JWT decoder with Keycloak's JWKS endpoint.
- `OAuth2LoginConfig`: `spring.security.oauth2.client` for authorization-code flow (admin UI).
- `OAuth2TokenRelay`: passes access token downstream to internal services.

### 08-04 · OIDC & SSO
- `KeycloakConfig.java`: `spring.security.oauth2.resourceserver.jwt.issuer-uri=http://keycloak:8080/realms/shopverse`.
- Keycloak in `docker-compose.yml` with a pre-configured `shopverse` realm, `shopverse-api` client, and demo users.
- `OidcUserService extends DefaultOidcUserService`: extracts custom claims (`shopverse_roles`) from ID token.

### 08-05 · SAML2 Enterprise SSO
- `Saml2Config.java` (`@ConditionalOnProperty("shopverse.security.saml.enabled")`): `RelyingPartyRegistrationRepository` configured for an IdP metadata URL.
- `docs/saml2-setup.md`: step-by-step guide for connecting with an enterprise IdP (e.g., Azure AD).

### 08-06 · Spring Security Architecture
- Comment block in `SecurityConfig.java` explaining: `SecurityFilterChain` → `AuthenticationManager` → `AuthenticationProvider` → `UserDetailsService` → `UserDetails`.
- `ShopVerseSecurityFilterChain`: explicit ordering: CORS → CSRF → JWT filter → authorization rules.
- Custom `AccessDeniedHandler` and `AuthenticationEntryPoint` returning JSON (not HTML redirect).

### 08-07 · API Security
- `RateLimitAspect` (Redis-backed): `@RateLimit(perMinute=60)` on `OrderController.placeOrder()` and `AuthController.login()`.
- CORS: `CorsConfigurationSource` — whitelist configured origins; `@CrossOrigin` not used (centralized config).
- Input validation: `@Valid` + custom `@ValidSku`, `@ValidCurrency` annotations.
- `docs/api-security.md`: documents rate limiting strategy, CORS policy, input validation approach.

### 08-08 · OWASP Top 10 & Injection Prevention
- SQL injection: all queries use JPQL named parameters or `PreparedStatement` — demonstrated with a comment showing the vulnerable vs safe version.
- XSS: `HtmlUtils.htmlEscape()` used in any field that will be rendered in HTML; `X-XSS-Protection` header via `HttpHeadersConfig`.
- `SecurityHeadersConfig`: sets `Content-Security-Policy`, `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, `Strict-Transport-Security`.
- `OWASPNotes.md` in `docs/`: maps each Top 10 item to the ShopVerse mitigation.

### 08-09 · Password Hashing & Cryptography
- `BCryptPasswordEncoder(strength=12)` for customer passwords.
- `AesEncryptionService`: AES-256-GCM for encrypting `Customer.paymentToken` at rest (`@Converter`-based transparent encryption).
- `SecureRandomTokenGenerator`: `SecureRandom.getInstanceStrong()` for password reset tokens.

### 08-10 · RBAC, ABAC & Zero Trust
- Roles: `ROLE_CUSTOMER`, `ROLE_SELLER`, `ROLE_ADMIN`.
- RBAC: `@PreAuthorize("hasRole('ADMIN')")` on admin endpoints.
- ABAC: `@PreAuthorize("#order.customerId == authentication.name")` — attribute-based check (order belongs to caller).
- `PermissionEvaluator` implementation: `hasPermission(auth, targetObject, "READ")` for fine-grained checks.
- `docs/zero-trust.md`: comment on how mTLS between internal services would be added.

---

## Chapter 09 — Caching

### 09-01 · CPU Caches & Memory Hierarchy
- Comment block in `HotProductCache.java` explaining L1/L2/L3 CPU cache sizes and cache-line alignment; relates to why `ConcurrentHashMap` segment size is chosen to fit a cache line.

### 09-02 · JVM Object & Heap Caching
- `CaffeineProductCache`: `Caffeine.newBuilder().maximumSize(10_000).expireAfterWrite(5, MINUTES).recordStats().build()` — heap-level LRU cache.
- `CacheStatsEndpoint` (`@ReadOperation`): exposes Caffeine hit rate, miss rate, eviction count via Actuator.

### 09-03 · Application-Level Caching
- Spring Cache abstraction wired to both Caffeine (local) and Redis (distributed).
- `@Cacheable(value="products", key="#id")` on `ProductService.findById()`.
- `@CacheEvict(value="products", key="#product.id")` on `ProductService.update()`.
- `@CachePut(value="products", key="#result.id")` on `ProductService.create()`.
- `@Caching` composite annotation on `ProductService.delete()` — evicts from both `products` and `product-search` caches.

### 09-04 · Distributed Cache — Redis
- `RedisCacheManager` with per-cache TTL configuration:
  - `products`: 5 minutes
  - `categories`: 1 hour
  - `sessions`: 30 minutes
- `RedissonClient` for distributed data structures (see Chapter 11 — Redisson `RLock`).
- `RedisHealthIndicator` (custom): PINGs Redis, checks memory usage, reports in `/actuator/health`.

### 09-05 · Hazelcast & Memcached
- `HazelcastConfig.java` (`@ConditionalOnProperty("shopverse.cache.provider=hazelcast")`): `HazelcastInstance` as an alternative `CacheManager` — drop-in replacement via Spring Cache abstraction.
- `MemcachedConfig.java` (`@ConditionalOnProperty("shopverse.cache.provider=memcached")`): `XMemcachedClient` configured as a simple `put`/`get`/`delete` store for session data alternative.
- `docs/cache-provider-comparison.md`: Redis vs Hazelcast vs Memcached comparison for ShopVerse's needs.

### 09-06 · DB Query Cache & Connection Pooling
- Hibernate query cache enabled for `CategoryRepository.findAll()` (rarely changes).
- HikariCP: `maximumPoolSize=10`, `minimumIdle=2`, `connectionTimeout=3000`, `idleTimeout=600000`, `maxLifetime=1800000` in `application.yml`.
- `HikariMetricsTracker` integration: pool size, active connections, pending threads reported to Micrometer.

### 09-07 · HTTP Caching
- `ProductController.getProduct()`: sets `Cache-Control: max-age=300, public` for public product data.
- `OrderController.getOrder()`: sets `Cache-Control: no-store` for sensitive order data.
- `ETag` header generated from `order.version` hash — `ShallowEtagHeaderFilter` registered in `WebConfig`.
- `@RequestMapping` with `If-None-Match` handling: return `304 Not Modified` when ETag matches.

### 09-08 · CDN & Edge Caching
- `nginx.conf` (`proxy_cache_path`, `proxy_cache_valid 200 5m`): Nginx caches product catalog responses at the edge.
- `ProductImageController`: response includes `Cache-Control: max-age=86400, immutable` for static product images served via `/images/`.
- `docs/cdn-setup.md`: CloudFront distribution config for the `/images/` path pointing to S3.

### 09-09 · Cache Strategies
- **Cache-aside**: `ProductService.findById()` — check cache, miss → DB, populate cache.
- **Write-through**: `ProductService.update()` — update DB and cache in same transaction.
- **Write-behind**: `ProductViewCountService` — increments Redis counter; background `@Scheduled` job flushes to DB every 5 min.
- **Refresh-ahead**: `CategoryCacheWarmer` — `@EventListener(ApplicationReadyEvent.class)` preloads all categories into cache.

### 09-10 · Cache Eviction & Consistency
- LRU eviction: `Caffeine.maximumSize(10_000)` → evicts LRU when full.
- Redis `maxmemory-policy allkeys-lru` documented in `docker-compose.yml` Redis command args.
- Cache stampede prevention: `RedissonClient.getLock("product-cache-init")` — only one instance rebuilds cache after eviction.
- `CacheConsistencyNotes.md`: documents the eventual consistency window between DB update and cache eviction.

---

## Chapter 10 — Search & Elasticsearch

### 10-01 · Search Fundamentals & Inverted Index
- Comment block in `ElasticsearchConfig.java` explaining the inverted index: term → list of document IDs; why it makes full-text search O(1) vs O(n) table scan.

### 10-02 · Elasticsearch Architecture
- `ElasticsearchConfig.java`: `ElasticsearchClient` bean connecting to `http://elasticsearch:9200`.
- Dev: single-node cluster in `docker-compose.yml`; prod: 3-node cluster documented.
- `IndexManagementService`: creates `products` index with custom settings on startup if not present.

### 10-03 · Index Mappings & Analyzers
- `products-mapping.json` (classpath resource): explicit mapping — `name: {type: text, analyzer: shopverse_analyzer}`, `sku: {type: keyword}`, `price: {type: scaled_float}`, `tags: {type: keyword}`.
- Custom `shopverse_analyzer`: `standard` tokenizer + `lowercase` + `stop` + `synonym` filters.
- `IndexManagementService.createIndex()`: loads and applies `products-mapping.json`.

### 10-04 · Elasticsearch Query DSL
- `ProductSearchService.search(ProductSearchRequest)`:
  - `bool` query with `must` (full-text on name/description), `filter` (price range, tags, category), `should` (boost in-stock items).
  - `multi_match` query across `name^3` (boosted), `description^1`.
- Results mapped to `ProductSearchResult` records.

### 10-05 · Relevance Scoring & Boosting
- `function_score` query in `ProductSearchService.searchWithBoost()`: boosts products with `avgRating > 4` by factor 1.5 and products with `stock > 0` by factor 2.
- `explain=true` option in dev mode to log relevance scores.

### 10-06 · Autocomplete & Fuzzy Search
- `completion` suggester on `ProductDocument.nameSuggest` field (type `completion`).
- `SearchController.autocomplete()`: `GET /api/v1/search/suggest?q=lap` → returns top 5 product name suggestions.
- Fuzzy search: `match` query with `fuzziness: AUTO` — handles typos in product names.

### 10-07 · Spring Data Elasticsearch
- `ProductSearchDocument` (`@Document(indexName="products")`): mirrors `ProductDocument` with search-optimised field types.
- `ProductSearchRepository extends ElasticsearchRepository<ProductSearchDocument, String>`.
- Custom `@Query` in repository: `@Query("{\"bool\": {\"must\": [{\"match\": {\"name\": \"?0\"}}]}}")`.

### 10-08 · Search Data Sync — CDC & Debezium
- `DebeziumConfig.java` (`@ConditionalOnProperty("shopverse.search.sync=debezium")`): configures Debezium embedded engine with `io.debezium.connector.postgresql.PostgresConnector`; captures changes from `products` table via `pgoutput` replication slot.
- `ProductChangeEventHandler`: consumes Debezium `ChangeEvent`, calls `ProductSearchService.upsert()` or `delete()`.
- Fallback: `SearchSyncListener` — `@TransactionalEventListener(AFTER_COMMIT)` — manually syncs on product save/update when Debezium is disabled.

### 10-09 · Elasticsearch Performance Tuning
- `BulkProductIndexer`: uses ES `BulkRequest` to index products in batches of 500 — used during initial load.
- `refresh_interval: 30s` set on the `products` index for write-heavy scenarios (document the tradeoff).
- `docs/elasticsearch-tuning.md`: shard count recommendation (1 shard per 30 GB), replica strategy.

### 10-10 · Search Alternatives
- `docs/search-alternatives.md`: comparison table — Elasticsearch vs OpenSearch vs Meilisearch vs PostgreSQL full-text vs Typesense — with ShopVerse's rationale for Elasticsearch.
- `PostgresFtsRepository`: `@Query(value="SELECT * FROM products WHERE to_tsvector('english', name || ' ' || description) @@ plainto_tsquery('english', :q)", nativeQuery=true)` — demonstrates PostgreSQL FTS as a simpler alternative for small catalogs.

---

## Chapter 11 — Concurrency & Locking

### 11-01 · Optimistic Concurrency Control
- `Product.version` (`@Version int`) — JPA optimistic lock; `StockUpdateService.decrementStock()` catches `OptimisticLockException` and retries up to 3 times.
- Unit test: two threads decrement the same product's stock concurrently; assert final stock is correct.

### 11-02 · Pessimistic Locking
- `FlashSaleService.reserveStock()`: `@Lock(LockModeType.PESSIMISTIC_WRITE)` on the repository method → generates `SELECT FOR UPDATE`.
- `DeadlockPreventionService`: always acquires locks in ascending `productId` order — comment explaining the fix.

### 11-03 · ETags & HTTP Conditional Requests
- `ProductController.getProduct()`: generates ETag from `product.version` hash; returns `304 Not Modified` with matching `If-None-Match`.
- `OrderController.updateOrder()`: accepts `If-Match` header — rejects with `412 Precondition Failed` if version mismatch.
- `ShallowEtagHeaderFilter` registered in `WebConfig` for automatic ETag generation.

### 11-04 · Distributed Locks
- `RedissonDistributedLock` wrapping `RLock redissonClient.getLock("flash-sale:{productId}")`.
- `FlashSaleService.buyNow()`: acquires distributed lock with 10 s timeout, executes purchase, releases lock in `finally`.
- `DistributedLockAspect`: `@DistributedLock(key="#productId")` annotation + `@Around` aspect using Redisson.

### 11-05 · Two-Phase Locking & Deadlocks
- `TwoPhaseLockingDemo.java`: comment walkthrough of 2PL: growing phase (acquire locks) → shrinking phase (release after commit).
- `pg_locks` query in `LockMonitoringService.findBlockedQueries()` — surfaces deadlocks in the admin health endpoint.
- `DeadlockRetryPolicy`: `@Retryable(retryFor=DeadlockLoserDataAccessException.class, maxAttempts=3)`.

### 11-06 · MVCC & Snapshot Isolation
- `SnapshotIsolationDemo.java`: runs two concurrent transactions at `REPEATABLE READ`; shows that T2 reads its snapshot, not T1's uncommitted changes.
- `WriteSkewPreventionService`: uses `SERIALIZABLE` isolation + `@Retryable` for the doctor on-call example (applied to flash sale double-booking scenario).

### 11-07 · Application-Level Conflict Resolution
- `CartMergeService.merge(guestCart, customerCart)`: **last-write-wins** strategy with timestamp comparison — if item in both carts, keep the one with the later `updatedAt`.
- `OrderConflictResolver`: on `OptimisticLockException`, re-reads entity and re-applies the update — demonstrates **merge** strategy.

### 11-08 · Event Sourcing & Idempotency
- `IdempotencyFilter`: reads `Idempotency-Key` header on `POST /api/v1/orders`; checks Redis for a stored response; if found, returns cached response; else processes and stores result.
- `OrderEventStore` (Cassandra `OrderEventLog`): every state change appended as an immutable event — `ORDER_PLACED`, `PAYMENT_CAPTURED`, `ORDER_SHIPPED` etc.
- `OrderProjection.rebuild(orderId)`: replays all events from `OrderEventStore` to reconstruct current state.

### 11-09 · CRDTs & Collaborative Editing
- `CartCrdtService`: implements a simple **G-Counter CRDT** for cart item quantities — each node increments its own counter; merge = max per node. Stored in Redis as a hash.
- `docs/crdts.md`: explains where full CRDT (e.g., collaborative cart editing across nodes) would be beneficial in ShopVerse.

### 11-10 · Database Lock Monitoring
- `LockMonitoringService.getBlockedQueries()`: queries `pg_stat_activity` joined with `pg_locks` to find waiting queries and their blockers.
- `LockMonitoringEndpoint` (`@Endpoint(id="db-locks")`): custom Actuator endpoint returning current lock wait graph.
- Alert: if any query has been waiting > 30 s, `ReplicationLagHealthIndicator` sets health to `DOWN`.

---

## Chapter 12 — Internationalization

### 12-01 · i18n Fundamentals
- `LocaleConfig.java`: `LocaleContextHolder`, `AcceptHeaderLocaleResolver` — locale detected from `Accept-Language` header.
- `MessageSource` bean: `ReloadableResourceBundleMessageSource`, base name `classpath:i18n/messages`, UTF-8, cache 3600 s.

### 12-02 · Message Bundles & Resource Files
- `i18n/messages.properties` (default — English): `order.placed=Your order {0} has been placed.`
- `i18n/messages_fr.properties`: French translations.
- `i18n/messages_de.properties`: German translations.
- `i18n/messages_ar.properties`: Arabic translations (RTL).
- `NotificationService` uses `messageSource.getMessage("order.placed", new Object[]{orderId}, locale)`.

### 12-03 · Number & Currency Formatting
- `CurrencyFormattingService.format(Money money, Locale locale)`: uses `NumberFormat.getCurrencyInstance(locale)` — returns `$1,234.56` for en-US, `1.234,56 €` for de-DE.
- `MoneySerializer` (Jackson `@JsonComponent`): serializes `Money` as `{ amount: "1,234.56", currency: "USD", formatted: "$1,234.56" }` based on request locale.

### 12-04 · Date/Time Localization
- `DateTimeFormattingService.format(Instant instant, Locale locale, ZoneId zone)`: `DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(locale).withZone(zone)`.
- All API responses include `createdAt` as both ISO-8601 UTC string and `createdAtFormatted` (locale-formatted).

### 12-05 · Timezone Handling
- All `Instant` values stored in DB as `TIMESTAMPTZ` (PostgreSQL UTC).
- `Customer.preferredZoneId` field: `ZoneId` stored as string (`"Europe/London"`).
- `OrderResponseMapper.toResponse(order, customer)`: converts `createdAt` Instant → ZonedDateTime using `customer.preferredZoneId`.
- `ZoneId.of(...)` validated on input; `ZoneId.getAvailableZoneIds()` used to build the allowed zones list.

### 12-06 · Character Encoding
- `application.yml`: `server.servlet.encoding.charset=UTF-8`, `server.servlet.encoding.force=true`.
- All `RestTemplate` and `WebClient` calls: explicit `MediaType.APPLICATION_JSON` with `UTF-8` charset.
- `StandardCharsets.UTF_8` used in every `byte[]` ↔ `String` conversion — never default charset.

### 12-07 · RTL Language Support
- `LocaleResponseAdvice` (`@ControllerAdvice`): adds `X-Language-Direction: rtl` response header when locale is `ar` or `he`.
- `docs/rtl-support.md`: guidance for frontend on applying `dir="rtl"` based on the header.

### 12-08 · Multi-Tenancy — Locale Per User
- `TenantContext` (ThreadLocal): holds `tenantId`, `locale`, `currency`, `zoneId` for the current request.
- `TenantContextFilter`: populates `TenantContext` from JWT claims (`tenant_id`, `locale`).
- `TenantAwareMessageSource`: wraps `MessageSource`, falls back to tenant-specific bundle if present.
- `TenantAwarePricingService`: returns prices in the tenant's default currency.

### 12-09 · Translation Workflow
- `docs/translation-workflow.md`: describes the extraction → translation → import cycle using `spring-messages-export` Maven plugin to generate XLIFF files for translators.
- `messages_keys.txt`: auto-generated list of all message keys for translators.

### 12-10 · Testing i18n
- `I18nControllerTest`: parameterized test with `@CsvSource({"en,Your order","fr,Votre commande","de,Ihre Bestellung"})` — sends `Accept-Language` header and asserts response body language.
- `CurrencyFormattingTest`: asserts `$1,234.56` for en-US and `1.234,56 €` for de-DE.
- `TimezoneConversionTest`: asserts UTC-stored time is correctly rendered in `"Europe/London"` and `"Asia/Kolkata"`.

---

## Chapter 13 — Performance & Async

### 13-01 · Sync, Async & Semi-Sync
- `OrderApplicationService.placeOrder()`: synchronous (caller waits for confirmation).
- `NotificationService.sendAsync()`: `CompletableFuture.supplyAsync(() -> send(), notificationExecutor)` — caller does not wait.
- `InventoryReservationService.reserveWithTimeout()`: semi-sync — waits up to 2 s for the reservation future, then cancels and returns `TIMEOUT` error.

### 13-02 · Reactive Programming — Project Reactor
- `ProductCatalogReactiveService`: `Flux<ProductSummary> streamAll()` — streams products from MongoDB using `ReactiveMongoTemplate`.
- `CartReactiveService`: `Mono<Cart> getCart(String customerId)` — non-blocking Redis read via `ReactiveRedisTemplate`.
- `Flux.zip`, `Flux.merge`, `Mono.flatMap`, error handling with `onErrorResume`.

### 13-03 · Spring WebFlux & Non-Blocking IO
- `ReactiveProductController` (`@RestController` in a WebFlux context, on separate `/reactive` prefix): `GET /reactive/products` returns `Flux<ProductSummary>` with `text/event-stream` for SSE.
- `WebClient` (not `RestTemplate`) used in `ExternalPricingClient` for calling external pricing API.
- `R2dbcConfig.java` (`@ConditionalOnProperty("shopverse.reactive.r2dbc.enabled")`): `R2dbcEntityTemplate` as an alternative for fully reactive DB access.

### 13-04 · Backpressure & Flow Control
- `ProductStreamController`: `Flux.fromIterable(products).delayElements(Duration.ofMillis(10))` — throttled streaming.
- `.onBackpressureBuffer(500, product -> log.warn("Buffer full, dropping {}", product.id()))` on the product stream.
- `docs/backpressure.md`: explains reactive streams contract and how Spring WebFlux implements it.

### 13-05 · Thread Pool & Executor Tuning
- `AsyncConfig.java`: `ThreadPoolTaskExecutor notificationExecutor` — `corePoolSize=5`, `maxPoolSize=20`, `queueCapacity=100`, `threadNamePrefix=notification-`.
- `AsyncConfig.java`: `ThreadPoolTaskExecutor reportExecutor` — `corePoolSize=2`, `maxPoolSize=5`.
- `@Async("notificationExecutor")` on `NotificationService.send()`.
- `VirtualThreadExecutor` (Java 21): `Executors.newVirtualThreadPerTaskExecutor()` for the HTTP request handling (Spring Boot 3.2 `spring.threads.virtual.enabled=true`).

### 13-06 · Profiling & Performance Analysis
- `AsyncProfilerConfig.java`: comment block on how to attach async-profiler: `java -agentpath:libasyncProfiler.so=start,file=profile.html`.
- `PerformanceTestProfile` (`application-perf.yml`): disables all caches, enables `hibernate.generate_statistics`, sets log level TRACE for JDBC.
- `PerformanceNotes.md`: documents top 3 bottlenecks found during profiling and the fixes applied.

### 13-07 · Load Testing
- `load-tests/k6-order-flow.js`: k6 script — ramps from 10 → 100 virtual users over 5 min, runs the full `login → add-to-cart → checkout` flow.
- `load-tests/jmeter-product-search.jmx`: JMeter plan — 200 concurrent users, 5 min, `GET /api/v1/products/search?q=laptop`.
- `docs/load-test-results.md`: baseline results and SLOs (p99 < 500 ms, error rate < 0.1%).

### 13-08 · Memory Profiling & GC Tuning
- `Dockerfile`: `-XX:+UseG1GC -Xms512m -Xmx1g -XX:MaxGCPauseMillis=200 -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/dumps -XX:+UseStringDeduplication`.
- `GcMetricsEndpoint` (Actuator): exposes GC pause times, heap used, GC count via `GarbageCollectorMXBean`.
- Comment in `ProductCacheWarmer.java`: "loading 10k products into Caffeine uses ~50 MB heap — monitor with -verbose:gc".

### 13-09 · JMH Benchmarking
- `benchmarks/` Maven module with JMH dependency.
- `ProductSearchBenchmark`: `@Benchmark @BenchmarkMode(Mode.Throughput)` — benchmarks three search strategies: JPA LIKE, PostgreSQL FTS, Elasticsearch.
- `OrderBuilderBenchmark`: compares `new Order(...)` vs `Order.Builder` performance.
- `mvn -pl benchmarks exec:java` documented in `docs/benchmarking.md`.

### 13-10 · Database Performance Tuning
- `application.yml`: `spring.jpa.properties.hibernate.jdbc.fetch_size=50` (cursor-based fetching).
- `SlowQueryLogService`: queries `pg_stat_statements` WHERE `mean_exec_time > 100` — logs top 10 slow queries on startup.
- `ConnectionPoolMetrics`: exposes HikariCP pool saturation to Micrometer — alert if `pending_threads > 5`.
- Batch insert benchmark: `BatchInsertBenchmark` compares `saveAll()` vs JDBC `batchUpdate()` for 10k records.

---

## Chapter 14 — System Design

### 14-01 · System Design Methodology
- `docs/system-design/requirements.md`: functional requirements, non-functional requirements (10k orders/day, p99 < 200 ms, 99.9% uptime), capacity estimation (storage, bandwidth, QPS).

### 14-02 · Scalability Patterns
- Horizontal scaling: Spring Boot app is stateless (JWT auth, Redis sessions) — any instance can handle any request. Documented in `docs/system-design/scalability.md`.
- Database read scaling: read replicas via `RoutingDataSource` (Chapter 05-09).
- Async processing: heavy operations (email, search indexing) offloaded to Kafka consumers.

### 14-03 · Consistent Hashing & Load Balancing
- `docs/system-design/load-balancing.md`: consistent hashing used by Redis Cluster for key distribution; Nginx round-robin for app instances.
- `nginx.conf`: `upstream shopverse_backend { least_conn; server app1:8080; server app2:8080; server app3:8080; }`.

### 14-04 · CAP Theorem & Consistency Models
- Reuses `docs/data-store-decisions.md` (Chapter 06-01) — extended with consistency model for each store (strong, eventual, causal).
- `CapTheoremEndpoint` (Actuator `@ReadOperation`): returns current consistency config for each data store.

### 14-05 · Message Queues & Event Streaming
- **Kafka**: `docker-compose.yml` includes `kafka:29092` (KRaft mode, no ZooKeeper).
- `KafkaConfig.java`: `ProducerFactory`, `ConsumerFactory`, `KafkaTemplate<String, OrderEvent>`.
- Topics: `orders.placed`, `orders.shipped`, `inventory.low`, `search.sync`.
- `OrderEventProducer.publish(OrderPlacedEvent)`: `kafkaTemplate.send("orders.placed", orderId, event)`.
- `SearchSyncConsumer` (`@KafkaListener(topics="search.sync")`): consumes product change events, syncs to Elasticsearch.
- `InventoryAlertConsumer` (`@KafkaListener(topics="inventory.low")`): triggers restock notifications.
- Dead Letter Topic: `orders.placed.DLT` — configure via `@RetryableTopic`.

### 14-06 · API Gateway & Service Discovery
- `spring-cloud-gateway` module (`shopverse-gateway`): routes `/api/v1/products/**` → catalog service, `/api/v1/orders/**` → ordering service.
- `GatewayConfig.java`: `RouteLocator` with `filters(f -> f.requestRateLimiter(r -> r.setRateLimiter(redisRateLimiter)))`.
- `docs/service-discovery.md`: documents how Kubernetes DNS (`shopverse-catalog.default.svc.cluster.local`) replaces Eureka/Consul in K8s deployments.

### 14-07 · Database Sharding & CQRS
- **CQRS**: `OrderCommandService` (writes: place, cancel, ship — uses PostgreSQL primary) and `OrderQueryService` (reads: find, list, report — uses read replica + Elasticsearch).
- `CommandBus` / `QueryBus` interfaces routing commands/queries to the correct handler.
- `docs/sharding.md`: documents how the monthly table partitioning is logical sharding by time; how to shard by `customerId` hash for global scale.

### 14-08 · Observability — Metrics, Tracing, Logging
- **Metrics**: Micrometer → Prometheus → Grafana (`docker-compose.yml` includes Prometheus + Grafana).
- **Distributed tracing**: Micrometer Tracing + Zipkin (or OpenTelemetry OTLP to Tempo). `management.tracing.sampling.probability=1.0` in dev.
- **Structured logging**: Logback with `logstash-logback-encoder` — JSON log lines with `traceId`, `spanId`, `customerId`, `orderId` MDC fields.
- **Correlation ID**: `CorrelationIdFilter` — reads `X-Correlation-ID` header (or generates UUID), sets MDC, adds to response.
- `docs/observability.md`: Grafana dashboard JSON (or screenshot) showing order throughput, error rate, p99 latency.

### 14-09 · System Design Walkthroughs — Part 1
- `docs/system-design/walkthroughs.md` — Part 1: Design a **URL shortener** as a reference exercise; map each design decision to a ShopVerse equivalent (e.g., "short URL = order ID generation via `AtomicLong` + base62 encoding").
- `Base62Encoder.java`: utility class used to generate short order reference codes.

### 14-10 · System Design Walkthroughs — Part 2
- `docs/system-design/walkthroughs.md` — Part 2: Design a **notification system**; map to `NotificationService` architecture — fanout via Kafka, template engine, multi-channel delivery.
- `NotificationRateLimiter`: per-user per-channel rate limiting using Redis token bucket.

---

## Chapter 15 — Microservices

### 15-01 · Monolith vs Microservices
- `docs/architecture-decision.md`: why ShopVerse starts as a structured monolith; which bounded contexts (Catalog, Ordering, Inventory, Search) are candidates for extraction; strangler fig pattern.

### 15-02 · Service Decomposition & Bounded Contexts
- Package structure reflects bounded contexts: `com.shopverse.catalog`, `com.shopverse.ordering`, `com.shopverse.inventory`, `com.shopverse.search`, `com.shopverse.notification`.
- No cross-context direct service calls — communication only via domain events (Kafka) or well-defined anti-corruption layer.

### 15-03 · REST, gRPC & GraphQL
- REST: all main APIs (see Chapter 03-04).
- **gRPC**: `shopverse-grpc` module — `inventory.proto` defines `CheckStock(CheckStockRequest) returns (StockResponse)`. `InventoryGrpcService` implements it. `GrpcInventoryClient` calls it from the ordering context.
- **GraphQL**: `spring-graphql` on `ProductController` — `GET /graphql` supports queries like `{ products(filter: {minPrice: 10}) { id name price } }`. Schema in `resources/graphql/schema.graphqls`.

### 15-04 · Event-Driven Architecture
- All cross-context communication uses Kafka (see Chapter 14-05).
- `OrderingContext` publishes `OrderPlacedEvent` to `orders.placed` topic.
- `InventoryContext` consumes `orders.placed`, reserves stock, publishes `StockReservedEvent` or `StockInsufficientEvent`.
- `NotificationContext` consumes `orders.placed` and `orders.shipped`.
- `docs/event-catalogue.md`: all events, their topics, producers, consumers.

### 15-05 · CQRS Pattern
- `OrderCommandService.placeOrder()`: writes to PostgreSQL primary. Returns only the order ID.
- `OrderQueryService.findOrderById()`: reads from PostgreSQL replica.
- `OrderSearchQueryService.search()`: reads from Elasticsearch.
- `CommandHandler` / `QueryHandler` interfaces — `OrderCommandHandler`, `OrderSearchQueryHandler`.

### 15-06 · Saga Pattern
- `PlaceOrderSaga` (choreography-based): `OrderPlaced` → `StockReserved` → `PaymentCaptured` → `OrderConfirmed`. Compensating events: `PaymentFailed` → `StockReleased` → `OrderCancelled`.
- `SagaStateStore` in Redis: tracks saga state per `orderId` for correlation.
- `SagaEventHandler`: listens for `PaymentFailed` and triggers compensation.

### 15-07 · Resilience Patterns
- Resilience4j: `shopverse-web/pom.xml` includes `resilience4j-spring-boot3`.
- `@CircuitBreaker(name="payment", fallbackMethod="paymentFallback")` on `PaymentService.charge()`.
- `@Retry(name="inventory")` on `InventoryService.reserve()`.
- `@RateLimiter(name="search")` on `ProductSearchService.search()`.
- `@Bulkhead(name="notifications")` on `NotificationService.send()`.
- `application.yml`: `resilience4j.circuitbreaker.instances.payment.failureRateThreshold=50`.

### 15-08 · API Versioning Strategies
- URL versioning: `/api/v1/` and `/api/v2/` — `v2` adds `recommendations` field to `OrderResponse`.
- `Accept` header versioning: `Accept: application/vnd.shopverse.v2+json` supported on `ProductController`.
- Deprecation headers: `Deprecation: true`, `Sunset: Sat, 01 Jan 2026 00:00:00 GMT` on v1 endpoints.
- `docs/api-versioning.md`: policy for backwards compatibility and deprecation.

### 15-09 · Microservices Testing Strategies
- Contract tests: `shopverse-contracts` module using Pact — `OrderingConsumerTest` defines the contract for `inventory-service`; `InventoryProviderVerificationTest` verifies it.
- Component test: `@SpringBootTest` with all external dependencies mocked via WireMock.
- `AbstractIntegrationTest` (see Chapter 03-10): Testcontainers base class shared across all integration tests.

### 15-10 · Microservices Antipatterns & Migration
- `docs/antipatterns.md`: documents which antipatterns ShopVerse deliberately avoids — chatty services (use Kafka), distributed monolith (clear context boundaries), shared DB (each context owns its schema).
- `docs/migration-plan.md`: step-by-step plan to extract `CatalogService` from the monolith using the strangler fig pattern.

---

## Chapter 16 — Docker

### 16-01 · Docker Architecture
- Comment block in `docs/docker.md` explaining Docker daemon, containerd, OCI image spec, and why layered images matter for CI cache hit rates.

### 16-02 · Dockerfile Best Practices
- `Dockerfile`:
  - Non-root user: `RUN addgroup -S shopverse && adduser -S shopverse -G shopverse; USER shopverse`.
  - Minimal base: `FROM eclipse-temurin:21-jre-jammy`.
  - `.dockerignore`: excludes `target/`, `*.md`, `.git`, `load-tests/`.
  - `HEALTHCHECK CMD curl --fail http://localhost:8080/actuator/health || exit 1`.

### 16-03 · Multi-Stage Builds
```dockerfile
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:21-jre-jammy AS runtime
RUN addgroup -S shopverse && adduser -S shopverse -G shopverse
WORKDIR /app
COPY --from=builder /build/target/shopverse-web-*.jar app.jar
USER shopverse
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseG1GC", "-jar", "app.jar"]
```

### 16-04 · Docker Networking
- `docker-compose.yml` defines a custom bridge network `shopverse-net`.
- All services on `shopverse-net` — app connects to `postgres:5432`, `redis:6379`, `kafka:29092` by service name.
- `elasticsearch` on a separate `elastic-net` (security isolation); `shopverse-net` and `elastic-net` bridged via the app container.

### 16-05 · Docker Volumes & Storage
- Named volumes in `docker-compose.yml`: `postgres-data`, `redis-data`, `es-data` (persist across `docker-compose down`).
- `docker-compose.override.yml` mounts `./dumps:/dumps` for heap dump output.

### 16-06 · Docker Compose
- `docker-compose.yml`: postgres, postgres-replica, redis, mongodb, cassandra, neo4j, elasticsearch, kafka, zookeeper (or KRaft), keycloak, prometheus, grafana, zipkin, nginx.
- `docker-compose.override.yml` (dev only): live-reload via `spring-boot-devtools`, skips TLS, exposes debug port `5005`.
- `docker-compose.prod.yml`: resource limits (`mem_limit: 2g`), restart policies (`restart: unless-stopped`).

### 16-07 · Docker Security
- Non-root user (see 16-02).
- Read-only root filesystem: `read_only: true` in `docker-compose.yml` for app container; writeable tmpfs for `/tmp`.
- `docker scan shopverse-web:latest` (Snyk) step in CI — fails build on HIGH/CRITICAL CVEs.
- No secrets in `ENV` — all secrets via Docker secrets or environment variable injection from CI.

### 16-08 · Container Registry
- `.github/workflows/release.yml`: builds and pushes to `ghcr.io/manishkumar/shopverse-web:${VERSION}`.
- Image tags: `latest` + semantic version `1.2.3` + git SHA `sha-abc1234`.
- `docs/registry.md`: image retention policy — keep last 10 tagged versions.

### 16-09 · Docker Without Kubernetes
- `docs/deployment-options.md`: running ShopVerse on a single VM with Docker Compose + Nginx + Let's Encrypt (simple production setup for small scale).
- `docker-compose.monitoring.yml`: Prometheus + Grafana + Loki stack for standalone monitoring.

### 16-10 · Debugging & Troubleshooting Containers
- `docs/docker-debugging.md`: `docker exec -it shopverse-web sh`, `docker logs --tail 100 -f`, `docker stats`, `docker inspect`.
- Remote debug: `docker-compose.override.yml` exposes port 5005 with `-agentlib:jdwp=transport=dt_socket,server=y,address=*:5005`.

---

## Chapter 17 — Kubernetes

### 17-01 · Kubernetes Architecture
- `docs/kubernetes.md`: control plane (API server, etcd, scheduler, controller manager) vs data plane (kubelet, kube-proxy, container runtime) explained with reference to ShopVerse's deployment.

### 17-02 · Core Workloads
- `k8s/deployment.yaml`: `Deployment` for `shopverse-web` — `replicas: 3`, `RollingUpdate` strategy, `readinessProbe` and `livenessProbe` on `/actuator/health`.
- `k8s/cronjob.yaml`: `CronJob` for `partition-manager` (runs the SQL partition creation monthly).

### 17-03 · Services & Cluster DNS
- `k8s/service.yaml`: `ClusterIP` service for internal access + `LoadBalancer` service for external (or use Ingress).
- Services reference each other by DNS: `shopverse-postgres.default.svc.cluster.local`.

### 17-04 · Ingress & TLS
- `k8s/ingress.yaml`: Nginx Ingress Controller — routes `shopverse.example.com/api` → `shopverse-web:8080`.
- TLS: `cert-manager` `ClusterIssuer` with Let's Encrypt; `tls` block in Ingress manifest.

### 17-05 · ConfigMaps & Secrets
- `k8s/configmap.yaml`: `spring.datasource.url`, `spring.redis.host`, `spring.kafka.bootstrap-servers`.
- `k8s/secret.yaml`: `spring.datasource.password`, `JWT_SECRET`, `STRIPE_API_KEY` (base64 encoded).
- `deployment.yaml` references ConfigMap via `envFrom` and Secret via `secretKeyRef`.
- `docs/secrets-management.md`: External Secrets Operator + AWS Secrets Manager for production.

### 17-06 · Autoscaling
- `k8s/hpa.yaml`: `HorizontalPodAutoscaler` — `minReplicas: 2`, `maxReplicas: 10`, scale on CPU > 70% and custom metric `http_requests_per_second > 100`.
- `k8s/pdb.yaml`: `PodDisruptionBudget` — `minAvailable: 1` to ensure zero-downtime during rolling updates.

### 17-07 · Kubernetes Storage
- `k8s/pvc.yaml`: `PersistentVolumeClaim` for PostgreSQL data — `storageClassName: standard`, `5Gi`.
- StatefulSet for PostgreSQL (dev/test only; prod uses managed DB).

### 17-08 · Helm Package Management
- `k8s/helm/shopverse/`: full Helm chart — `Chart.yaml`, `values.yaml`, `templates/deployment.yaml`, `templates/service.yaml`, `templates/ingress.yaml`, `templates/configmap.yaml`, `templates/secret.yaml`, `templates/hpa.yaml`.
- `helm install shopverse ./k8s/helm/shopverse -f values-prod.yaml`.

### 17-09 · GKE / EKS Managed Kubernetes
- `docs/gke-setup.md`: `gcloud container clusters create shopverse --num-nodes=3 --machine-type=e2-standard-2`.
- `docs/eks-setup.md`: `eksctl create cluster --name shopverse --nodegroup-name standard --node-type t3.medium`.
- `k8s/helm/shopverse/values-gke.yaml` and `values-eks.yaml` — cloud-specific overrides.

### 17-10 · Kubernetes Java API & Fabric8
- `KubernetesPartitionJob.java`: uses `io.fabric8:kubernetes-client` to create a `Job` resource programmatically when the monthly partition cron fires inside the cluster.
- `PodDiscoveryService`: `KubernetesClient.pods().inNamespace("default").withLabel("app","shopverse").list()` — lists running pods (used in admin health endpoint).

---

## Chapter 18 — Nginx

### 18-01 · Nginx Architecture & Event Model
- Comment block in `nginx/nginx.conf`: explains `worker_processes auto`, `worker_connections 1024`, epoll event model, and why Nginx handles 10k connections with minimal memory vs thread-per-connection Apache.

### 18-02 · Reverse Proxy Configuration
```nginx
# nginx/nginx.conf
upstream shopverse_backend {
    least_conn;
    server shopverse-web-1:8080;
    server shopverse-web-2:8080;
    keepalive 32;
}

server {
    listen 80;
    server_name shopverse.example.com;

    location /api/ {
        proxy_pass         http://shopverse_backend;
        proxy_set_header   Host $host;
        proxy_set_header   X-Real-IP $remote_addr;
        proxy_set_header   X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header   X-Forwarded-Proto $scheme;
        proxy_read_timeout 60s;
    }
}
```

### 18-03 · Load Balancing Algorithms
- `nginx.conf`: `upstream` block uses `least_conn` — document why this is better than `round_robin` for ShopVerse (variable request duration).
- `ip_hash` upstream documented as an alternative for session stickiness (though JWT makes it unnecessary).

### 18-04 · SSL/TLS Termination
```nginx
server {
    listen 443 ssl http2;
    ssl_certificate     /etc/nginx/certs/shopverse.crt;
    ssl_certificate_key /etc/nginx/certs/shopverse.key;
    ssl_protocols       TLSv1.2 TLSv1.3;
    ssl_ciphers         HIGH:!aNULL:!MD5;
    add_header          Strict-Transport-Security "max-age=31536000" always;
}
```

### 18-05 · Rate Limiting & Security
```nginx
limit_req_zone $binary_remote_addr zone=api:10m rate=60r/m;
limit_req_zone $binary_remote_addr zone=login:10m rate=5r/m;

location /api/v1/orders {
    limit_req zone=api burst=20 nodelay;
}
location /api/v1/auth/login {
    limit_req zone=login burst=3 nodelay;
}
```

### 18-06 · Location Matching & Routing
- `location /api/v1/products/` → proxy to backend.
- `location /images/` → `alias /var/www/images/` (serve static product images directly).
- `location ~* \.(js|css|png|jpg|gif|ico)$` → add far-future `Expires` header.
- `location = /health` → return `200 OK` (Nginx-level health check, no backend hit).

### 18-07 · Nginx Kubernetes Ingress
- `k8s/ingress.yaml` annotations:
  - `nginx.ingress.kubernetes.io/rate-limit: "100"`.
  - `nginx.ingress.kubernetes.io/proxy-body-size: "10m"`.
  - `nginx.ingress.kubernetes.io/enable-cors: "true"`.
  - `nginx.ingress.kubernetes.io/rewrite-target: /$2`.

### 18-08 · Nginx Caching
```nginx
proxy_cache_path /var/cache/nginx levels=1:2 keys_zone=product_cache:10m max_size=1g inactive=5m;

location /api/v1/products {
    proxy_cache         product_cache;
    proxy_cache_valid   200 5m;
    proxy_cache_bypass  $http_pragma;
    add_header          X-Cache-Status $upstream_cache_status;
}
```

### 18-09 · Nginx vs Alternatives
- `docs/nginx-vs-alternatives.md`: Nginx vs HAProxy vs Traefik vs Envoy — comparison table; ShopVerse rationale for Nginx (simplicity, static file serving, wide K8s support).

### 18-10 · Monitoring & Debugging Nginx
- `nginx_status` endpoint: `location /nginx_status { stub_status; allow 127.0.0.1; deny all; }`.
- Prometheus `nginx-prometheus-exporter` in `docker-compose.yml` — scrapes `/nginx_status`, exposes Prometheus metrics.
- `docs/nginx-debugging.md`: `nginx -t` (config test), access log format with `$request_time`, `$upstream_response_time`.

---

## Chapter 19 — DevOps & CI/CD

### 19-01 · CI/CD Pipeline Design
```yaml
# .github/workflows/ci.yml
name: CI
on: [push, pull_request]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '21', distribution: 'temurin' }
      - name: Cache Maven
        uses: actions/cache@v4
        with: { path: ~/.m2, key: "${{ hashFiles('**/pom.xml') }}" }
      - run: mvn -B verify
      - name: Upload coverage
        uses: codecov/codecov-action@v4
```
- Separate `release.yml` workflow: builds Docker image, pushes to GHCR, deploys to K8s via `helm upgrade`.

### 19-02 · Infrastructure as Code
- `infra/terraform/main.tf`: AWS provider — creates RDS PostgreSQL instance, ElastiCache Redis cluster, MSK Kafka cluster, EKS cluster.
- `infra/terraform/variables.tf`: parameterised for `dev`/`staging`/`prod` environments.
- `infra/terraform/outputs.tf`: outputs RDS endpoint, Redis endpoint (used by Helm values files).
- `docs/iac.md`: `terraform init → plan → apply` workflow; state stored in S3 + DynamoDB lock.

### 19-03 · AWS Core Services & Java
- `AwsS3Service.java`: `S3Client` (AWS SDK v2) — `putObject` for product images, `getPresignedUrl` for temporary download links.
- `AwsRdsConfig.java` (`@ConditionalOnProperty("shopverse.aws.rds.enabled")`): `DataSource` pointing to RDS PostgreSQL endpoint from Terraform output.
- `AwsSqsConsumer.java` (`@ConditionalOnProperty("shopverse.aws.sqs.enabled")`): `SqsAsyncClient` polling `order-events` SQS queue as an alternative to Kafka for AWS deployments.

### 19-04 · AWS Storage & Messaging
- `S3ProductImageService`: upload (`putObject`), delete (`deleteObject`), generate pre-signed URL (`presignGetObject`, 1 h TTL).
- `SqsOrderEventPublisher`: `sqsAsyncClient.sendMessage(SendMessageRequest.builder().queueUrl(url).messageBody(json).build())`.
- `DynamoDbSessionStore` (see Chapter 06-04): DynamoDB as serverless session store.

### 19-05 · AWS Serverless & Edge
- `LambdaConfig.java` comment block: describes how `PlaceOrderUseCase` could be wrapped as an AWS Lambda handler with `aws-lambda-java-core`.
- `docs/aws-serverless.md`: Lambda cold start mitigation (SnapStart for Java), API Gateway trigger, SAM template snippet.
- CloudFront: `docs/cdn-setup.md` (see Chapter 09-08) extended with CloudFront distribution pointing to S3 for product images + ALB for API.

### 19-06 · AWS Data Services
- `docs/aws-data-services.md`: how ShopVerse's data stores map to AWS managed services — RDS Aurora PostgreSQL, ElastiCache Redis, MSK Kafka, DocumentDB (MongoDB API), Amazon Neptune (Neo4j replacement), Timestream (InfluxDB replacement).
- `AuroraConfig.java` (see Chapter 06-10): switching to Aurora Serverless v2 with zero code change.

### 19-07 · GCP Core Services & Java
- `GcpStorageService.java` (`@ConditionalOnProperty("shopverse.gcp.storage.enabled")`): `Storage` client (GCP SDK) — alternative to S3 for product images using Google Cloud Storage.
- `docs/gcp-setup.md`: GKE deployment, Cloud SQL (PostgreSQL), Memorystore Redis, Pub/Sub as Kafka alternative.

### 19-08 · GCP Messaging & Data
- `PubSubOrderEventPublisher.java` (`@ConditionalOnProperty("shopverse.gcp.pubsub.enabled")`): `Publisher` client — publishes `OrderPlacedEvent` to GCP Pub/Sub topic as an alternative to Kafka.
- `PubSubOrderEventConsumer.java`: `Subscriber` with `MessageReceiver` — pull subscription.
- `docs/gcp-data.md`: Firestore as MongoDB alternative, BigQuery for analytics, Spanner for global SQL.

### 19-09 · Observability & SRE Practices
- `prometheus.yml` (in `infra/monitoring/`): scrape configs for Spring Boot (`/actuator/prometheus`), Nginx (`nginx-exporter`), Kafka (`kafka-exporter`), PostgreSQL (`postgres-exporter`).
- `grafana/dashboards/shopverse.json`: pre-built Grafana dashboard — order throughput, p99 latency, error rate, DB connection pool, Kafka consumer lag.
- **SLOs** defined in `docs/slo.md`: availability 99.9%, order placement p99 < 500 ms, search p99 < 200 ms.
- **Error budget**: SLO breach triggers PagerDuty alert (webhook configured in Grafana).
- **OpenTelemetry**: `opentelemetry-spring-boot-starter` — auto-instruments HTTP, JDBC, Redis, Kafka. Traces exported to Jaeger (in `docker-compose.yml`).

### 19-10 · Deployment Strategies & DevSecOps
- **Blue-green**: `release.yml` deploys to `shopverse-green` Helm release; smoke tests run; traffic shifted by updating Nginx upstream.
- **Canary**: Nginx `split_clients` routes 10% to `shopverse-canary` upstream; progresses if error rate stays < 0.5%.
- **Rolling update**: default K8s Deployment `RollingUpdate` strategy (see Chapter 17-02).
- **DevSecOps**:
  - `trivy-action` in CI scans Docker image for CVEs; blocks merge on HIGH/CRITICAL.
  - `spotbugs-maven-plugin` (see Chapter 07-03) for static analysis.
  - `OWASP dependency-check-maven` plugin: scans all dependencies against CVE database.
  - `docs/devsecops.md`: shift-left security checklist.

---

## Project Structure (Full)

```
shopverse/
├── shopverse-parent/          (Maven BOM + multi-module root)
├── shopverse-domain/          (Java records, value objects, domain events — zero framework deps)
├── shopverse-application/     (Use cases, ports, command/query handlers)
├── shopverse-infrastructure/  (JPA, Redis, Kafka, MongoDB, Cassandra, Neo4j, ES, gRPC adapters)
├── shopverse-web/             (Spring MVC controllers, DTOs, security, WebFlux reactive endpoint)
├── shopverse-gateway/         (Spring Cloud Gateway)
├── shopverse-e2e-tests/       (Testcontainers full integration tests, Pact contract tests)
├── benchmarks/                (JMH benchmarks)
├── load-tests/                (k6 + JMeter scripts)
├── infra/
│   ├── terraform/             (AWS + GCP IaC)
│   └── monitoring/            (prometheus.yml, grafana dashboards, alertmanager rules)
├── k8s/
│   ├── *.yaml                 (raw manifests)
│   └── helm/shopverse/        (Helm chart)
├── nginx/
│   └── nginx.conf
├── docs/
│   ├── system-design/
│   ├── adr/                   (Architecture Decision Records)
│   └── *.md
├── docker-compose.yml
├── docker-compose.override.yml
├── docker-compose.prod.yml
├── Dockerfile
└── .github/workflows/
    ├── ci.yml
    └── release.yml
```

---

## Incremental Build Order

Build one step at a time. Each step must compile and pass tests before proceeding.

| Step | Covers | Deliverable |
|------|--------|-------------|
| 1 | Ch01, Ch07 | Maven multi-module skeleton; all domain records/value objects/exceptions; JVM flags in Dockerfile |
| 2 | Ch02, Ch04 | JPA entities, relationships, Flyway V1-V2, all design pattern classes (skeletons) |
| 3 | Ch03, Ch08 | Spring Boot app starts; JWT security; IoC wiring; AOP aspects |
| 4 | Ch04, Ch05 | Full JPA mappings, JPQL, Criteria API, Flyway V3-V8 (indexes, partitioning, procedures, triggers) |
| 5 | Ch03, Ch05 | REST controllers, service layer, transactions, isolation, CQRS split, read-replica routing |
| 6 | Ch06 | Redis (all data structures), MongoDB, Cassandra, Neo4j integrations |
| 7 | Ch09 | Full caching layer: Caffeine, Redis Cache, HTTP ETag, Nginx proxy cache |
| 8 | Ch10 | Elasticsearch index, sync (Debezium or event listener), search API, autocomplete |
| 9 | Ch11 | Distributed locks, idempotency, ETags, saga, event sourcing |
| 10 | Ch12 | i18n message bundles, currency/date formatting, timezone, multi-tenancy |
| 11 | Ch13 | Async/reactive endpoints, thread pool tuning, JMH benchmarks, k6 load tests |
| 12 | Ch14, Ch15 | Kafka integration, CQRS buses, resilience patterns, gRPC, GraphQL |
| 13 | Ch16 | Multi-stage Dockerfile, docker-compose with all services |
| 14 | Ch17 | K8s manifests, Helm chart |
| 15 | Ch18 | nginx.conf: reverse proxy, TLS, rate limiting, caching |
| 16 | Ch19 | GitHub Actions CI/CD, Terraform snippets, Prometheus+Grafana, OpenTelemetry |
| 17 | All | Final integration test + README Chapter→Class mapping table |

---

## Final README Must Include

1. **Architecture diagram** (ASCII or Mermaid) showing all data stores, Kafka, Nginx, Spring Boot, and their connections.
2. **Chapter → Class Mapping Table** — every sub-chapter mapped to the exact file that demonstrates it.
3. **How to run locally**: `docker-compose up -d && mvn -pl shopverse-web spring-boot:run -Dspring-boot.run.profiles=dev`.
4. **API summary**: curl examples for login, place order, search, recommendations.
5. **SLOs and monitoring**: how to open Grafana and what to look at.
