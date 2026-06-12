# ShopVerse — Session Context

_Last updated: 2026-06-11_

---

## What We Built (this session)

### 1. Order Status Transitions + Cassandra Activity Logging
- **`UpdateOrderStatusUseCase`** — confirms, processes, ships (with tracking number), delivers, cancels orders. Each transition logs an event to Cassandra (try-catch so Cassandra failure never rolls back PostgreSQL).
- **`OrderController`** — 5 new PATCH endpoints:
  - `PATCH /api/orders/{id}/confirm` — admin, PENDING → CONFIRMED
  - `PATCH /api/orders/{id}/process` — admin, CONFIRMED → PROCESSING
  - `PATCH /api/orders/{id}/ship` — admin, body `{"trackingNumber":"..."}`, PROCESSING → SHIPPED
  - `PATCH /api/orders/{id}/deliver` — admin, SHIPPED → DELIVERED
  - `PATCH /api/orders/{id}/cancel` — any authenticated user, PENDING/CONFIRMED/PROCESSING → CANCELLED
- **Postman** — 6 new requests in "🗃️ Order Activity (Cassandra)" folder (①–⑤ lifecycle + ⑥ invalid transition error case)

### 2. Bug Fix: `DataIntegrityViolationException` — null version on OrderEntity update
- **Root cause**: `JpaOrderRepositoryAdapter.save()` always called `toEntity()` which created a fresh `OrderEntity` with `id` set but `version = null`. Hibernate rejects detached entities with null `@Version`.
- **Fix**: Split into INSERT path (`order.getId() == null` → `toEntity()`) and UPDATE path (`order.getId() != null` → load existing managed entity, call `applyDomainChanges()` which only updates `status` and `trackingNumber`).
- **File**: `shopverse-infrastructure/.../adapter/JpaOrderRepositoryAdapter.java`

### 3. Unit + Component Tests (H2, no Docker needed)
- **Test infrastructure**: `application-test.yml` (H2 in PostgreSQL mode, Flyway disabled, `cache.type=simple`, all non-JPA auto-configs excluded) + `BaseIntegrationTest.java` (10 `@MockBean` stubs, JWT helpers, `@Transactional` auto-rollback)
- **Domain unit tests** (3): `OrderTest`, `ProductTest`, `CustomerTest` — pure JUnit5
- **Use case unit tests** (7): `PlaceOrderUseCaseTest`, `UpdateOrderStatusUseCaseTest`, `RegisterCustomerUseCaseTest`, `CreateProductUseCaseTest`, `SubmitReviewUseCaseTest`, `GetReviewsUseCaseTest`, `GetOrderActivityUseCaseTest` — Mockito
- **Component tests** (6): `AuthControllerTest`, `ProductControllerTest`, `OrderControllerTest`, `CustomerControllerTest`, `ReviewControllerTest`, `OrderActivityControllerTest` — Spring + H2 + MockMvc

---

## Pending / Next Steps

### Tests not yet run
The 18 test files are written but have NOT been compiled or executed yet. First run may surface:
1. `PlaceOrderUseCaseTest` — anonymous inner class for mock order setup
2. `ReviewControllerTest` — `ReviewRepository` injection chain via `MongoReviewRepositoryAdapter`
3. H2 DDL generation from JPA entities — potential type mismatches
4. `CustomerResponse.fullName` field name assertion in `CustomerControllerTest`

**To run tests:**
```bash
./mvnw test -pl shopverse-domain,shopverse-application,shopverse-web -Dspring.profiles.active=test
```

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
| Unit tests (domain + use cases) | ✅ Written, not run |
| Component tests (controllers, H2) | ✅ Written, not run |
| Postman collection | ✅ Up to date |

---

## Key Files

| File | Purpose |
|---|---|
| `shopverse-application/.../usecase/order/UpdateOrderStatusUseCase.java` | Status transitions + Cassandra log |
| `shopverse-web/.../controller/OrderController.java` | All order endpoints |
| `shopverse-infrastructure/.../adapter/JpaOrderRepositoryAdapter.java` | INSERT vs UPDATE path (version fix) |
| `shopverse-web/src/test/resources/application-test.yml` | Test profile config |
| `shopverse-web/src/test/java/.../test/BaseIntegrationTest.java` | Base class for all component tests |
| `ShopVerse.postman_collection.json` | Full API collection |

---

## How to Run the App

```bash
# Start infrastructure
docker-compose up -d postgres redis kafka mongo elasticsearch cassandra

# Run app
./mvnw spring-boot:run -pl shopverse-web

# Run tests (no Docker needed)
./mvnw test -pl shopverse-domain,shopverse-application,shopverse-web
```
