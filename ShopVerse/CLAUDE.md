# ShopVerse — Project Context for Claude

## What is this?
ShopVerse is a full-stack e-commerce capstone application built with Spring Boot 3.2.5 + Java 21.
It follows **Clean/Hexagonal Architecture** across a Maven multi-module monorepo.

**Root:** `F:\Final\ShopVerse`

---

## Module Structure

| Module | Purpose |
|---|---|
| `shopverse-domain` | Pure domain model — no framework dependencies |
| `shopverse-application` | Use cases, CQRS commands/queries, cache services |
| `shopverse-infrastructure` | JPA adapters, Redis, Kafka, Mongo, Cassandra, Neo4j, Elasticsearch |
| `shopverse-web` | Spring Boot entry point, REST controllers, Security, AOP |
| `shopverse-gateway` | API Gateway (`GatewayApplication`) |
| `shopverse-ui` | React + Vite frontend |
| `shopverse-e2e-tests` | End-to-end tests |
| `benchmarks` | JMH performance benchmarks |

---

## Tech Stack

- **Java 21**, **Spring Boot 3.2.5**, Maven multi-module
- **PostgreSQL** — primary DB via Spring Data JPA + Hibernate, Flyway migrations
- **Redis 7** — caching (`@Cacheable`) + session/token store + rate limiting + pub/sub
- **MongoDB** — product reviews (`ReviewDocument`)
- **Cassandra** — order activity log (`OrderActivityEntity`)
- **Neo4j** — product graph / recommendations (`ProductNode`)
- **Elasticsearch** — product full-text search (`ProductDocument`)
- **Kafka** — async order events (producer + consumer)
- **JWT (jjwt 0.12.5)** — stateless auth, stored in Redis via `RedisSessionStore`
- **Resilience4j** — circuit breaker + rate limiter
- **MapStruct** — entity↔domain mapping
- **Lombok** — boilerplate reduction
- **Nginx** — reverse proxy
- **Docker Compose** — all services containerised
- **Prometheus + Grafana** — metrics/observability
- **React + Vite** — frontend SPA

---

## Key Files

### Entry Point
- `shopverse-web/src/main/java/com/shopverse/ShopVerseApplication.java`

### Configuration
- `shopverse-web/src/main/resources/application.yml` — all app config (DB, Redis, Kafka, JWT, etc.)
- `shopverse-web/src/main/java/com/shopverse/config/SecurityConfig.java` — JWT stateless security
- `shopverse-web/src/main/java/com/shopverse/config/CacheConfig.java` — Redis cache manager, per-cache TTLs
- `shopverse-infrastructure/src/main/java/com/shopverse/infrastructure/config/RedisConfig.java` — RedisTemplate with JSON serialization
- `shopverse-infrastructure/src/main/java/com/shopverse/infrastructure/config/RedissonConfig.java` — Redisson (distributed locks)

### Redis / Caching
- `shopverse-application/src/main/java/com/shopverse/application/service/cache/CachedProductService.java` — `@Cacheable`, `@CachePut`, `@CacheEvict` on products
- `shopverse-application/src/main/java/com/shopverse/application/service/cache/OrderCacheService.java` — manual `RedisTemplate` cache for orders (5 min TTL)
- `shopverse-infrastructure/src/main/java/com/shopverse/infrastructure/redis/RedisSessionStore.java` — JWT token store + rate limit counters
- `shopverse-infrastructure/src/main/java/com/shopverse/infrastructure/redis/RedisPubSubPublisher.java` — pub/sub publisher
- `shopverse-application/src/main/java/com/shopverse/application/service/concurrency/DistributedLockService.java` — Redisson distributed locks

### Domain Models
- `shopverse-domain/src/main/java/com/shopverse/domain/model/` — `Product`, `Order`, `OrderItem`, `Customer`, `OrderStatus`, `CustomerTier`
- `shopverse-domain/src/main/java/com/shopverse/domain/vo/` — `Money`, `Address` (value objects)
- `shopverse-domain/src/main/java/com/shopverse/domain/port/` — `ProductRepository`, `OrderRepository`, `CustomerRepository`, `EventPublisher` (interfaces)

### REST Controllers
- `AuthController` — `/api/auth/register`, `/api/auth/login` (returns JWT)
- `ProductController` — `/api/products` (GET public, POST admin-only)
- `OrderController` — `/api/orders` (POST authenticated)
- `CustomerController` — `/api/customers`
- `SearchController` — `/api/search` (Elasticsearch)
- `ReactiveProductController` — reactive (WebFlux-style)
- `ProductGraphQLController` — GraphQL endpoint

### Security
- `shopverse-web/.../security/JwtTokenProvider.java` — generate/validate JWT
- `shopverse-web/.../security/JwtAuthenticationFilter.java` — filter chain integration
- Session policy: **STATELESS** (JWT only, no HttpSession)

### Infrastructure Adapters
- `JpaProductRepositoryAdapter`, `JpaOrderRepositoryAdapter`, `JpaCustomerRepositoryAdapter` — domain port implementations
- `OrderKafkaProducer` / `OrderKafkaConsumer` — Kafka events
- `ProductSearchService` / `ProductSyncService` — Elasticsearch
- `InventoryService` — stock management

### Patterns Used
- **CQRS** — `CommandBus`, `QueryBus`, `CommandHandler` in `shopverse-application/cqrs`
- **Saga** — `OrderSaga` for distributed transaction
- **Strategy** — `DiscountStrategy` (No/Seasonal/Tier)
- **Factory** — `PaymentProcessorFactory` (Stripe/PayPal)
- **Observer** — `OrderEventListener`
- **Idempotency** — `IdempotencyService`

---

## Cache Configuration (Redis)

Cache name | TTL
---|---
`products` | 30 min
`customers` | 60 min
`orders` | 5 min
Default | 10 min

Redis config in `application.yml`:
```yaml
spring.data.redis.host: ${REDIS_HOST:localhost}
spring.data.redis.port: ${REDIS_PORT:6379}
spring.data.redis.lettuce.pool.max-active: 16
```

---

## Security Model

- Stateless JWT auth (`SessionCreationPolicy.STATELESS`)
- Public: `GET /api/products/**`, `GET /api/search/**`, `/api/auth/**`, actuator health
- Admin only: `/api/admin/**`, `POST /api/products`
- Authenticated: `POST /api/orders`, customer endpoints
- Tokens stored in Redis via `RedisSessionStore` with key prefix `session:`

---

## Running Locally

```bash
# Start all infrastructure
docker-compose up -d postgres redis kafka mongo elasticsearch

# Run app (dev profile active by default)
./mvnw spring-boot:run -pl shopverse-web

# Run full stack
docker-compose up -d
```

App runs on **port 8080**. Nginx proxy on port 80/443.

### Environment Variables
| Variable | Default |
|---|---|
| `DB_USER` / `DB_PASS` | `shopverse` |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` |
| `REDIS_PASS` | (none) |
| `JWT_SECRET` | `change-this-secret-in-production-minimum-32-chars` |
| `KAFKA_BROKERS` | `localhost:9092` |
| `MONGO_URI` | `mongodb://localhost:27017/shopverse` |
| `ES_URI` | `http://localhost:9200` |

---

## Build

```bash
# Build all modules
./mvnw clean install -DskipTests

# Run tests
./mvnw test

# Run benchmarks
./mvnw package -pl benchmarks
```

- Requires Java 21+, Maven 3.9+
- Flyway migrations at `shopverse-web/src/main/resources/db/migration/`
- JaCoCo coverage reports generated on `mvn test`

---

## Frontend (shopverse-ui)

React + Vite SPA at `shopverse-ui/`.

```bash
cd shopverse-ui
npm install
npm run dev     # dev server
npm run build   # build to dist/
```

Pages: `HomePage`, `ProductsPage`, `ProductDetailPage`, `CartPage`, `CheckoutPage`, `OrdersPage`, `LoginPage`
API client: `shopverse-ui/src/api/client.js`
Auth context: `shopverse-ui/src/context/AuthContext.jsx`

---

## API Reference
- Postman collection: `ShopVerse.postman_collection.json`
- PDF reference: `ShopVerse_Services_Reference_v2.pdf`
- Deployment guide: `ShopVerse_UseCase_Flow_and_Deployment_Guide.pdf`

---

## Infrastructure / DevOps
- `Dockerfile` — layered Spring Boot image
- `docker-compose.yml` — full stack (Postgres, Redis, Kafka, Mongo, Cassandra, Neo4j, ES, Nginx, Prometheus, Grafana)
- `k8s/` — Kubernetes manifests + Helm chart (`k8s/helm/shopverse/`)
- `infra/terraform/` — Terraform IaC
- `infra/monitoring/prometheus.yml` + `alert_rules.yml`
- `.github/workflows/ci.yml` — GitHub Actions CI
