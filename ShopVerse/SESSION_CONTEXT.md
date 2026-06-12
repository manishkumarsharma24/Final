# ShopVerse — Session Context

_Last updated: 2026-06-12_

---

## What We Built (this session)

### 1. Order Status Transitions + Cassandra Activity Logging (Previous Session)
- **`UpdateOrderStatusUseCase`** — confirms, processes, ships (with tracking number), delivers, cancels orders. Each transition logs an event to Cassandra.
- **`OrderController`** — 5 PATCH endpoints for full order lifecycle.

### 2. Bug Fix: `DataIntegrityViolationException` (Previous Session)
- **Root cause**: null `@Version` on `OrderEntity` update.
- **Fix**: split INSERT vs UPDATE path in `JpaOrderRepositoryAdapter`.

### 3. Kafka — Extended (This Session)

**New domain events:**
- `NotificationEvent` — order/payment notification variants (sealed interface)
- `InventoryEvent` — StockReserved, StockReleased, StockLow, StockExhausted
- `AnalyticsEvent` — ProductViewed, ProductSearched, AddedToCart, CheckoutStarted, OrderConverted

**New Kafka topics (auto-created):**
- `shopverse.notifications` — 2 partitions
- `shopverse.inventory` — 3 partitions
- `shopverse.analytics` — 6 partitions (high volume)

**New/updated producers:**
- `OrderKafkaProducer` — now handles all 5 event types with correct topic routing

**New consumers:**
- `InventoryKafkaConsumer` — handles StockReleased → restores PostgreSQL stock on cancellation
- `AnalyticsKafkaConsumer` — processes analytics pipeline events
- `OrderKafkaConsumer` (enhanced) — now triggers notifications + inventory events

**New REST endpoints (Kafka-driven):**
- `POST /api/analytics/track` — generic analytics event
- `POST /api/analytics/track/view` — product view shorthand
- `POST /api/analytics/track/search` — search shorthand
- `POST /api/analytics/track/cart` — add-to-cart shorthand

**Key files:**
| File | Purpose |
|---|---|
| `shopverse-domain/.../event/NotificationEvent.java` | New sealed notification events |
| `shopverse-domain/.../event/InventoryEvent.java` | New sealed inventory events |
| `shopverse-domain/.../event/AnalyticsEvent.java` | New sealed analytics events |
| `shopverse-infrastructure/.../kafka/KafkaTopicsConfig.java` | Declarative topic beans |
| `shopverse-infrastructure/.../kafka/InventoryKafkaConsumer.java` | Stock restoration |
| `shopverse-infrastructure/.../kafka/AnalyticsKafkaConsumer.java` | Analytics pipeline |
| `shopverse-application/.../usecase/analytics/TrackAnalyticsUseCase.java` | Analytics use case |
| `shopverse-web/.../controller/AnalyticsController.java` | Analytics REST API |

---

### 4. RabbitMQ — New Integration (This Session)

**Architecture:**
- `shopverse.notifications` (topic exchange) → `shopverse.queue.email` + DLQ
- `shopverse.payments` (direct exchange) → `shopverse.queue.payment.callback` + webhook
- `shopverse.webhooks` (fanout exchange) → `shopverse.queue.webhook.delivery`

**New domain port:** `NotificationPublisher`

**New infrastructure:**
- `RabbitMQConfig` — all exchanges, queues, bindings, DLQ wired
- `NotificationRabbitPublisher` — implements `NotificationPublisher` port
- `EmailNotificationConsumer` — sends emails, routes to DLQ on failure
- `PaymentCallbackConsumer` — processes Stripe/PayPal callbacks
- `WebhookDeliveryConsumer` — delivers events to merchant webhooks

**New REST endpoints:**
- `POST /api/notifications/send` — admin: dispatch any notification type
- `POST /api/notifications/payment/callback` — payment gateway webhook receiver
- `POST /api/notifications/webhook/register` — merchant webhook registration

**pom.xml:** added `spring-boot-starter-amqp` to `shopverse-infrastructure`

**application.yml:** added `spring.rabbitmq.*` config block

**Key files:**
| File | Purpose |
|---|---|
| `shopverse-infrastructure/.../rabbitmq/RabbitMQConfig.java` | Full RabbitMQ topology |
| `shopverse-infrastructure/.../rabbitmq/NotificationRabbitPublisher.java` | Port implementation |
| `shopverse-infrastructure/.../rabbitmq/EmailNotificationConsumer.java` | Email + DLQ handler |
| `shopverse-infrastructure/.../rabbitmq/PaymentCallbackConsumer.java` | Payment callbacks |
| `shopverse-infrastructure/.../rabbitmq/WebhookDeliveryConsumer.java` | Merchant webhooks |
| `shopverse-application/.../usecase/notification/SendNotificationUseCase.java` | Notification use case |
| `shopverse-web/.../controller/NotificationController.java` | Notification REST API |

---

### 5. Neo4j — Extended Recommendations (This Session)

**Graph schema:**
- `(Product)-[:FREQUENTLY_BOUGHT_TOGETHER {count, orderId}]->(Product)`
- `(Product)-[:VIEWED_AFTER {count, sessionId}]->(Product)`

**New domain model:** `Recommendation`, port: `RecommendationRepository`

**New Cypher queries in `ProductGraphRepository`:**
- `findCombinedRecommendations` — merges FREQUENTLY_BOUGHT_TOGETHER + VIEWED_AFTER
- `findViewedAfterRecommendations` — pure view-based
- `createOrIncrementBoughtTogether` — idempotent MERGE with count
- `createOrIncrementViewedAfter` — session-level view tracking
- `upsertProduct` — safe MERGE for node creation
- `findTrendingProducts` — most co-purchased

**New infrastructure adapter:** `Neo4jRecommendationAdapter`

**New REST endpoints:**
- `GET /api/recommendations/{productId}` — combined recommendations (public)
- `GET /api/recommendations/category/{category}` — top-rated in category (public)
- `POST /api/recommendations/track/view` — record VIEWED_AFTER
- `POST /api/recommendations/track/purchase` — record FREQUENTLY_BOUGHT_TOGETHER

**Key files:**
| File | Purpose |
|---|---|
| `shopverse-domain/.../model/Recommendation.java` | Domain model |
| `shopverse-domain/.../port/RecommendationRepository.java` | Port interface |
| `shopverse-infrastructure/.../neo4j/ProductGraphRepository.java` | Enhanced Cypher queries |
| `shopverse-infrastructure/.../neo4j/Neo4jRecommendationAdapter.java` | Port implementation |
| `shopverse-application/.../usecase/recommendation/GetRecommendationsUseCase.java` | Query use case |
| `shopverse-application/.../usecase/recommendation/TrackProductInteractionUseCase.java` | Graph update use case |
| `shopverse-web/.../controller/RecommendationController.java` | Recommendation REST API |

---

## Full Feature Status

| Feature | Status |
|---|---|
| Redis caching (two-cache split) | ✅ Done |
| MongoDB product reviews | ✅ Done |
| Cassandra order activity | ✅ Done |
| Order status transitions (CONFIRM/SHIP/DELIVER/CANCEL) | ✅ Done |
| JPA bidirectional fix (null order_id) | ✅ Done |
| JPA version fix (null @Version on update) | ✅ Done |
| Kafka — extended (NotificationEvent, InventoryEvent, AnalyticsEvent) | ✅ Done |
| Kafka — InventoryKafkaConsumer (stock restore on cancel) | ✅ Done |
| Kafka — AnalyticsKafkaConsumer (analytics pipeline) | ✅ Done |
| RabbitMQ — full integration (exchanges, queues, DLQ) | ✅ Done |
| RabbitMQ — EmailNotificationConsumer + DLQ | ✅ Done |
| RabbitMQ — PaymentCallbackConsumer | ✅ Done |
| RabbitMQ — WebhookDeliveryConsumer (fanout) | ✅ Done |
| Neo4j — extended graph (VIEWED_AFTER, MERGE queries) | ✅ Done |
| Neo4j — RecommendationController REST API | ✅ Done |
| Unit tests (domain + use cases) | ✅ Written, not run |
| Component tests (controllers, H2) | ✅ Written, not run |
| Postman collection | ✅ Up to date (18 new requests) |

---

## Pending / Next Steps

### Tests not yet run
All test files written but NOT compiled/executed yet. First run may surface:
1. `BaseIntegrationTest` — new RabbitMQ `@MockBean` fields may need import adjustments
2. `RecommendationControllerTest` — verify `productGraphRepository` mock wiring
3. `AnalyticsControllerTest` — `EventPublisher` mock (via `orderKafkaProducer`) should be checked
4. `NotificationControllerTest` — `notificationRabbitPublisher` mock must be active

**To run all tests:**
```bash
./mvnw test -pl shopverse-domain,shopverse-application,shopverse-web -Dspring.profiles.active=test
```

### Environment variables to add (docker-compose / .env)
```
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USER=guest
RABBITMQ_PASS=guest
```

### docker-compose.yml — add RabbitMQ service
```yaml
rabbitmq:
  image: rabbitmq:3.13-management
  ports:
    - "5672:5672"
    - "15672:15672"  # Management UI
  environment:
    RABBITMQ_DEFAULT_USER: guest
    RABBITMQ_DEFAULT_PASS: guest
```

---

## How to Run the App

```bash
# Start all infrastructure (including RabbitMQ)
docker-compose up -d postgres redis kafka mongo elasticsearch cassandra rabbitmq

# Run app
./mvnw spring-boot:run -pl shopverse-web

# Run tests (no Docker needed)
./mvnw test -pl shopverse-domain,shopverse-application,shopverse-web
```

---

## New API Summary

### Kafka Analytics
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/analytics/track` | None | Generic analytics event |
| POST | `/api/analytics/track/view` | None | Product view |
| POST | `/api/analytics/track/search` | None | Search event |
| POST | `/api/analytics/track/cart` | None | Add to cart |

### RabbitMQ Notifications
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/notifications/send` | Admin | Manual notification dispatch |
| POST | `/api/notifications/payment/callback` | None | Payment gateway webhook |
| POST | `/api/notifications/webhook/register` | User | Register merchant webhook |

### Neo4j Recommendations
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/recommendations/{productId}` | None | Combined recommendations |
| GET | `/api/recommendations/category/{category}` | None | Category recommendations |
| POST | `/api/recommendations/track/view` | None | Record VIEWED_AFTER |
| POST | `/api/recommendations/track/purchase` | None | Record BOUGHT_TOGETHER |
