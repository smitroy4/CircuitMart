# CircuitMart — System Architecture

CircuitMart is a microservices-based e-commerce backend built with **Spring Boot 4.1 + Spring Cloud 2025.1.2** and **Java 25**. It demonstrates distributed system patterns including service discovery, API gateway, centralized config, circuit breakers, event-driven messaging via Kafka, and distributed tracing.

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [Tech Stack](#2-tech-stack)
3. [Services Breakdown](#3-services-breakdown)
   - 3.1 [Discovery Service](#31-discovery-service)
   - 3.2 [Config Server](#32-config-server)
   - 3.3 [API Gateway](#33-api-gateway)
   - 3.4 [Order Service](#34-order-service)
   - 3.5 [Inventory Service](#35-inventory-service)
4. [Communication Patterns](#4-communication-patterns)
   - 4.1 [Synchronous (Feign)](#41-synchronous-feign)
   - 4.2 [Asynchronous (Kafka)](#42-asynchronous-kafka)
5. [Resilience & Reliability](#5-resilience--reliability)
6. [Database Design](#6-database-design)
7. [Security](#7-security)
8. [Monitoring & Observability](#8-monitoring--observability)
9. [Prerequisites & Running the Project](#9-prerequisites--running-the-project)
10. [Known Issues](#10-known-issues)
11. [FAQs](#11-faqs)

---

## 1. Architecture Overview

```
Client (REST)
    |
    v
API Gateway  (JWT Auth, Logging, Routing)
    |
    +-----------^-------------^-----------+
    |           |             |           |
    v           |             v           v
Order Svc  ----+---->  Inventory Svc  Config Server (Git-backed)
    |           |             |           |
    |  Kafka    |    Kafka    |           |  Git Repo
    |  Producer |    Consumer  |           |  (circuitmart-config-server)
    |           |             |           |
    v           |             v           v
PostgreSQL  Eureka Server  PostgreSQL
(orders)    (:8761)        (products)
```

**Key Design Decisions:**
- **Why microservices?** Independent deployability, scaling, failure isolation.
- **Why Eureka?** Dynamic service discovery so services find each other without hardcoded addresses.
- **Why Config Server?** Centralized configuration baked into a Git repo — auditable, versioned, environment-specific.
- **Why API Gateway?** Single entry point; cross-cutting concerns (auth, logging, rate limiting) handled once.
- **Why Kafka?** Async, event-driven stock reservation decouples order creation from inventory updates.
- **Why Feign?** Declarative HTTP clients for synchronous inter-service calls with minimal boilerplate.
- **Why Resilience4J?** Prevent cascading failures; guard against downstream service unavailability.
- **Why Zipkin?** Distributed tracing across service boundaries to debug latency and failures.

---

## 2. Tech Stack

| Component              | Technology                              | Version / Notes                          |
|------------------------|-----------------------------------------|------------------------------------------|
| Language               | Java                                    | 25                                       |
| Framework              | Spring Boot                             | 4.1.0                                    |
| Cloud                  | Spring Cloud                            | 2025.1.2                                 |
| Service Discovery      | Eureka (Netflix)                        | `spring-cloud-starter-netflix-eureka-*`  |
| API Gateway            | Spring Cloud Gateway (WebFlux, reactive)| `spring-cloud-starter-gateway`           |
| Config Server          | Spring Cloud Config (Git-backed)        | `spring-cloud-config-server`             |
| Sync Inter-service     | OpenFeign                               | `spring-cloud-starter-openfeign`         |
| Async Messaging        | Apache Kafka 3.7.1 via Spring Cloud Stream | `spring-cloud-stream-binder-kafka`    |
| Circuit Breaker        | Resilience4J                            | `spring-cloud-starter-circuitbreaker-resilience4j` |
| Database               | PostgreSQL                              | via `org.postgresql` runtime driver      |
| ORM                    | Spring Data JPA / Hibernate             | `spring-boot-starter-data-jpa`           |
| DTO Mapping            | ModelMapper                             | 3.2.0                                    |
| JWT                    | jjwt 0.12.6                             | `io.jsonwebtoken`                        |
| Tracing                | Micrometer + Brave + Zipkin             | `micrometer-tracing-bridge-brave`        |
| Build                  | Maven (with wrappers)                   | Each service is an independent Maven project |
| Monitoring             | Spring Boot Actuator                    | Endpoints: `health`, `info` only         |
| Boilerplate Reduction  | Lombok                                  | `@Data`, `@Slf4j`, `@RequiredArgsConstructor` |
| Containerization       | Docker Compose (Kafka only)             | No Dockerfiles for the services          |

---

## 3. Services Breakdown

### 3.1 Discovery Service

**Port:** `8761`  
**Dependencies:** `spring-cloud-starter-netflix-eureka-server`

A standard Eureka server. All other services register here. The dashboard at `http://localhost:8761` shows registered instances, health status, and metadata.

**Config:**
```properties
eureka.client.register-with-eureka=false
eureka.client.fetch-registry=false
```

- **Why standalone?** The discovery service itself doesn't need to discover others — it's the registry.
- **Significance:** Without Eureka, every service would need hardcoded host:port for every other service. Eureka enables dynamic routing and load balancing.

---

### 3.2 Config Server

**Port:** `8888`  
**Dependencies:** `spring-cloud-config-server`, `spring-cloud-starter-netflix-eureka-client`

Serves configuration from a **Git repository** (`github.com/smitroy4/circuitmart-config-server`) authenticated via `GITHUB_ACCESS_TOKEN`.

**Config:**
```yaml
spring.cloud.config.server.git:
  uri: https://github.com/smitroy4/circuitmart-config-server
  username: "${GITHUB_USERNAME:smitroy4}"
  password: "${GITHUB_ACCESS_TOKEN}"
  default-label: main
```

**Why Git-backed config?**
- **Audit trail:** Every config change is a Git commit with a message and author.
- **Environment separation:** Branches or labels for dev/staging/prod.
- **Runtime refresh:** Clients use `@RefreshScope` to reload config without restart.

**How clients consume config:**
```properties
spring.config.import=configserver:http://localhost:8888
```
Services (api-gateway, order-service, inventory-service) fetch their database URLs, JWT secrets, feature flags, and routing rules from this server at startup.

**Significance:** Eliminates config duplication across services; enables fleet-wide config changes with a single Git push.

---

### 3.3 API Gateway

**Dependencies:** `spring-cloud-starter-gateway`, `spring-cloud-starter-netflix-eureka-client`, `spring-cloud-starter-config`, `jjwt`

The single entry point for all client requests. It:

1. **Routes requests** to downstream services (routes defined in Config Server Git repo).
2. **Validates JWT tokens** via `AuthenticationGatewayFilterFactory` — extracts subject and forwards as `X-User-Id` header. Returns `401` if invalid.
3. **Logs all requests** via `GlobalLoggingFilter` (URI + response status).
4. **Prints all routes** at startup via `ApplicationRunner` for debugging.

**Why reactive (WebFlux)?**
- Non-blocking I/O handles many concurrent connections with fewer threads.
- Better suited for gateway workloads that are I/O-bound (proxy, filter, route).

**Filter Chain:**
```
Request → GlobalLoggingFilter (pre) → AuthenticationGatewayFilter → Route to Service → GlobalLoggingFilter (post) → Response
```

**Significance:**
- Single security enforcement point — individual services trust the `X-User-Id` header rather than re-validating JWT.
- Centralized routing logic — adding/moving services requires only a config change.
- Cross-cutting concerns (logging, auth, rate limiting) handled once, not per service.

---

### 3.4 Order Service

**Dependencies:** `spring-boot-starter-data-jpa`, `spring-boot-starter-web`, `postgresql`, `modelmapper`, `openfeign`, `resilience4j`, `spring-cloud-stream-binder-kafka`, `micrometer-tracing`

Responsible for order creation and lifecycle.

#### API Endpoints

| Method | Path               | Description                                           |
|--------|--------------------|-------------------------------------------------------|
| GET    | `/orders`          | List all orders                                       |
| GET    | `/orders/{id}`     | Get order by ID                                       |
| POST   | `/orders/create-order` | Create order (sync stock reduction + async Kafka event) |
| GET    | `/orders/helloOrders` | Test endpoint with feature flag                     |

#### Entity Model

**`Orders`** (table: `orders`)
| Field        | Type           | Notes                         |
|--------------|----------------|-------------------------------|
| id           | Long           | PK, auto-generated            |
| orderStatus  | OrderStatus    | CONFIRMED / CANCELLED / PENDING / DELIVERED |
| totalPrice   | BigDecimal     | precision=10, scale=2         |
| items        | List\<OrderItem\> | `@OneToMany` cascade ALL, orphan removal |
| version      | Integer        | `@Version` — optimistic locking active |

**`OrderItem`** (table: `order_item`)
| Field     | Type    | Notes                          |
|-----------|---------|--------------------------------|
| id        | Long    | PK                             |
| productId | Long    | References Product (no FK)     |
| quantity  | Integer | -                              |
| order     | Orders  | `@ManyToOne` → `orders.id`     |

#### Order Creation Flow (`OrdersService.createOrder`)

```
1. Receive OrderRequestDto (list of items with productId + quantity)
2. Call inventory-service via Feign → POST /inventory/products/reduce-stocks
   │  Circuit breaker wraps this call (sliding window: 6, failure threshold: 50%)
   │  On failure → fallback method throws RuntimeException
3. Map DTO → Orders entity, set items back-reference
4. Set totalPrice from Feign response, status = CONFIRMED
5. Save order to PostgreSQL
6. Publish OrderCreatedEvent to Kafka topic "order.created"
   │  Event: { orderId, items: [{productId, quantity}] }
```

**Why both sync AND async?**
- **Sync (Feign):** Immediately validates stock and gets total price — the response body is the total, which is needed before saving the order. The order cannot proceed without knowing the total.
- **Async (Kafka):** The actual stock deduction is a separate concern. The inventory service consumes the event, deducts stock, and maintains eventual consistency. This separation means the order service doesn't block if inventory is slow.

**Why optimistic locking (`@Version` on `Orders`)?**
- Prevents concurrent updates to the same order from overwriting each other.
- Throws `OptimisticLockException` on conflict — caller can retry.

**Feature Flag (`features.user-tracking-enabled`):**
- Read from Config Server at runtime via `@RefreshScope`.
- If disabled, the `helloOrders` endpoint returns "Feature not enabled".
- Demonstrates runtime feature toggling without redeployment.

---

### 3.5 Inventory Service

**Dependencies:** `spring-boot-starter-data-jpa`, `spring-boot-starter-web`, `postgresql`, `modelmapper`, `openfeign`, `spring-cloud-stream-binder-kafka`, `micrometer-tracing`

Responsible for products, stock levels, and inventory reservation.

#### API Endpoints

| Method | Path                               | Description                        |
|--------|------------------------------------|------------------------------------|
| GET    | `/inventory/products`              | List all products                  |
| GET    | `/inventory/products/{id}`         | Get product by ID                  |
| POST   | `/inventory/products/reduce-stocks`| Reduce stock, return total price   |
| POST   | `/inventory/products`              | Create product (stub)              |
| DELETE | `/inventory/products/{id}`         | Delete product (stub)              |

#### Entity Model

**`Product`** (table: `products`)
| Field   | Type       | Notes                              |
|---------|------------|------------------------------------|
| id      | Long       | PK                                 |
| name    | String     | -                                  |
| price   | BigDecimal | -                                  |
| stock   | Integer    | -                                  |
| version | Integer    | ⚠️ NOT annotated `@Version` — no optimistic locking |

#### Stock Reduction (`ProductService.reduceStocks`)

```
1. Iterate over requested items
2. For each item:
   - Find product by productId (404 if missing)
   - Check stock >= quantity (409 CONFLICT if insufficient)
   - Decrement stock
   - Accumulate total price (price × quantity)
3. Save all updated products (within @Transactional)
4. Return total BigDecimal price
```

**Why @Transactional?** Ensures all stock decrements are all-or-nothing. If one item fails, no stock is deducted.

**⚠️ Known Issue — Race Condition:** There is no pessimistic lock (`@Lock(PESSIMISTIC_WRITE)`) or optimistic lock (`@Version`) on `Product`. Under concurrent load, two orders can read the same stock simultaneously, both pass the check, and both decrement — resulting in overselling.

#### Kafka Consumer

```java
@Bean
public Consumer<OrderRequestDto> reserveStock() {
    return orderRequest -> productService.reduceStocks(orderRequest);
}
```

- **Binding:** `reserveStock-in-0` → topic `order.created`, group `inventory-service`
- **Why consumer group?** Ensures at-least-once delivery. If the service restarts, unprocessed events are replayed. Multiple instances share the load (each event goes to one consumer).
- **Why functional bean instead of `@StreamListener`?** Spring Cloud Stream's functional model is simpler, more testable, and avoids annotation-heavy configuration.

**Seed Data (`data.sql`):** 20 sample electronic products with stock counts (e.g., Smartphone: 50000, Laptop: 30000).

---

## 4. Communication Patterns

### 4.1 Synchronous (Feign)

```
Order Service ──POST /inventory/products/reduce-stocks──→ Inventory Service
                ←────────── BigDecimal totalPrice ────────
```

- **Library:** OpenFeign (`@FeignClient`)
- **Discovery:** Resolves via Eureka (load-balanced by Spring Cloud)
- **Circuit Breaker:** Resilience4J wraps the call
- **Why synchronous here?** The total price from inventory is required to save the order. This is a read-verify + compute operation that demands a response.

**Feign Client example (order-service):**
```java
@FeignClient(name = "inventory-service", path = "/inventory")
public interface InventoryFeignClient {
    @PostMapping("/products/reduce-stocks")
    BigDecimal reduceStocks(@RequestBody OrderRequestDto orderRequestDto);
}
```

**Significance:**
- Declarative — no HTTP boilerplate.
- Integrated with Eureka for service discovery.
- Integrates with Resilience4J for fault tolerance.
- Integrates with Micrometer for tracing.

---

### 4.2 Asynchronous (Kafka)

```
Order Service (Producer) ── topic "order.created" ──→ Inventory Service (Consumer)

Event: OrderCreatedEvent {
    orderId: Long,
    items: [{ productId: Long, quantity: Integer }]
}
```

**Why asynchronous / event-driven?**
1. **Decoupling:** Order service doesn't wait for inventory processing. It saves the order immediately and publishes an event.
2. **Resilience:** If inventory service is down, events queue in Kafka. When it comes back up, it processes them.
3. **Scalability:** Multiple inventory service instances can consume from the same group — workload is distributed.
4. **Extensibility:** Other services (notification, analytics) can subscribe to the same topic without modifying order service.

**Why Spring Cloud Stream instead of raw Kafka client?**
- Binder abstraction — same code works with Kafka, RabbitMQ, etc.
- Binding configuration is externalized (properties files).
- Functional programming model (`Consumer`/`Function`/`Supplier` beans) instead of low-level consumer APIs.

**Configuration:**
```properties
# Producer (order-service)
spring.cloud.stream.bindings.orderCreatedEvent-out-0.destination=order.created
spring.cloud.stream.bindings.orderCreatedEvent-out-0.content-type=application/json

# Consumer (inventory-service)
spring.cloud.stream.bindings.reserveStock-in-0.destination=order.created
spring.cloud.stream.bindings.reserveStock-in-0.group=inventory-service
spring.cloud.stream.bindings.reserveStock-in-0.content-type=application/json
```

**Significance:** Event-driven architecture enables loose coupling, failure isolation, and async processing — core to microservices resilience.

---

## 5. Resilience & Reliability

| Pattern            | Where                        | What It Does                                      |
|--------------------|------------------------------|---------------------------------------------------|
| Circuit Breaker    | `OrdersService.createOrder()`| Prevents cascading failures when inventory is down |
| Fallback Method    | `OrdersService.createOrderFallback()` | Provides degraded behavior (throws error)         |
| Optimistic Locking | `Orders` entity (`@Version`) | Prevents concurrent order mutation conflicts       |
| Consumer Group     | Inventory Kafka consumer     | At-least-once delivery, load-balanced consumption  |
| @Transactional     | `ProductService.reduceStocks`| All-or-nothing stock deduction                    |

**Circuit Breaker Config:**
```
sliding-window-size: 6       → evaluates last 6 calls
failure-rate-threshold: 50   → opens circuit if ≥50% fail
wait-duration-in-open-state: 10s → retry after 10 seconds
```

**Failure Scenarios:**
1. **Inventory service down:** Circuit breaker opens → `createOrderFallback` throws `RuntimeException` → client gets 500.
2. **Concurrent order on same product:** No optimistic lock on Product → race condition (known issue).
3. **Kafka broker down:** `StreamBridge.send()` may fail → event lost. (No retry/error handling configured on the producer side.)
4. **Database constraint:** Transaction rollback → client gets 500.

**What's missing:**
- Retry mechanism (`@Retry` is commented out in OrdersService)
- Fallback returning cached/safe response (currently throws exception)
- Dead letter queue (DLQ) for Kafka failures
- Bulkhead isolation for thread pools

---

## 6. Database Design

### PostgreSQL — Order Service (`orders`, `order_item`)

```
orders
├── id              BIGINT  PK
├── order_status    VARCHAR(255)  [CONFIRMED|CANCELLED|PENDING|DELIVERED]
├── total_price     NUMERIC(10,2)
└── version         INTEGER  @Version (optimistic lock)

order_item
├── id              BIGINT  PK
├── product_id      BIGINT  (references Product, no FK constraint)
├── quantity        INTEGER
└── order_id        BIGINT  FK → orders.id
```

### PostgreSQL — Inventory Service (`products`)

```
products
├── id              BIGINT  PK
├── name            VARCHAR(255)
├── price           NUMERIC(38,2)
├── stock           INTEGER
└── version         INTEGER  ⚠️ NOT a JPA version field
```

**Schema Notes:**
- Hibernate DDL auto generates tables (no explicit Flyway/Liquibase migrations).
- `spring.sql.init.mode=always` + `defer-datasource-initialization=true` allows `data.sql` to seed product data after table creation.
- `order_item.product_id` has no FK constraint — intentional? The product ID could point to a non-existent product (though the flow ensures it exists).

**Why PostgreSQL?**
- Production-grade ACID compliance.
- JSONB support for future extensibility.
- Widely used in Spring Boot ecosystems.

---

## 7. Security

**Authentication:** JWT-based token validation in the API Gateway.

**Flow:**
```
Client → Authorization: Bearer <JWT> → API Gateway
  → AuthenticationGatewayFilterFactory validates token
  → Extracts subject (user ID)
  → Forwards as X-User-Id header to downstream services
  → Returns 401 if token missing, expired, or invalid
```

**JWT Configuration:**
- Secret key injected via `@{jwt.SecretKey}` from Config Server.
- Claims extracted: `subject` (user ID), `roles` (unused in current code).
- Library: jjwt 0.12.6 (io.jsonwebtoken).

**Actuator Security:** Only `health` and `info` endpoints are exposed. `env`, `configprops`, `beans` are explicitly disabled.

**What's missing:**
- Role-based access control (RBAC) — `roles` claim is extracted but never checked.
- Downstream service-to-service auth — once past the gateway, services trust the `X-User-Id` header (no inter-service token).

**Significance:**
- Centralized auth means downstream services don't each need their own JWT validation.
- The JWT secret lives only in the config server (Git repo), not in service code.

---

## 8. Monitoring & Observability

| Concern       | Technology                               | Notes                                     |
|---------------|------------------------------------------|-------------------------------------------|
| Distributed Tracing | Micrometer Tracing + Brave + Zipkin | Every request gets a trace ID propagated across services |
| Logging       | SLF4J + Logback (default)                | `@Slf4j` on all services                  |
| Health Checks | Actuator `/actuator/health`              | Exposed (configuration: `health,info`)    |
| Metrics       | Micrometer (actuator `/actuator/metrics`)| Not exposed in configuration              |
| Route Logging | Gateway `GlobalLoggingFilter`            | Logs URI and status code for all requests  |

**Tracing Flow:**
```
Request → API Gateway (creates trace) → Order Service → Feign (propagates trace) → Inventory Service
                                                                                    → Kafka (propagates trace) → Inventory Consumer
```

- Zipkin collector receives spans from all services.
- Trace ID correlates all operations in a single request across service boundaries.

---

## 9. Prerequisites & Running the Project

**Prerequisites:**
- Java 25
- Maven (or use `mvnw` wrappers in each service)
- PostgreSQL running locally with databases: `order-service`, `inventory-service`
- Docker (for Kafka)
- Git access to `circuitmart-config-server` repo (with `GITHUB_ACCESS_TOKEN` env var)
- Eureka URL: `http://localhost:8761/eureka`
- Config Server URL: `http://localhost:8888`

**Startup order:**
```
1. docker compose up -d              (Kafka)
2. discovery-service                 (Eureka)
3. config-server                     (Config Server)
4. api-gateway                       (Gateway)
5. inventory-service                 (Inventory)
6. order-service                     (Orders)
```

---

## 10. Known Issues

**Critical:**
- **Race condition in stock reduction** — `ProductService.reduceStocks()` has no locking. Concurrent requests can oversell inventory. Fix: add `@Version` or `@Lock(PESSIMISTIC_WRITE)`.
- **Feign client path mismatch** — `OrdersFeignClient` in inventory-service calls `/core/helloOrders` instead of `/helloOrders`. Always returns 404.

**High:**
- Circuit breaker fallback throws an exception rather than returning a degraded yet sensible response.
- No dead letter queue configuration for Kafka consumer failures.
- `JwtService` recreates the HMAC key on every call (no caching).
- No input validation on DTO fields (`@NotBlank`, `@Positive`, etc.) despite `spring-boot-starter-validation` dependency.
- `ModelMapper` may cause infinite recursion with bidirectional `@OneToMany` → `@ManyToOne` mappings.

**Medium:**
- No integration tests or contract tests.
- No Dockerfiles for the five services (only Kafka is containerized).
- Config server requires a real Git access token — no local fallback profile.
- No API documentation (Swagger/OpenAPI).

---

## 11. FAQs

**Q1: What problem does Eureka solve?**  
Dynamic service registration and discovery. Services find each other by logical name (`order-service`) instead of hardcoded host:port. Enables load balancing and graceful handling of instance failures.

**Q2: Why both Feign and Kafka for order-inventory communication?**  
Feign (sync) provides the total price needed to save the order — this is a request-response operation. Kafka (async) handles the downstream stock reservation — non-blocking, fault-tolerant, eventual consistency. They solve different timing and coupling needs.

**Q3: How does the circuit breaker work here?**  
When `OrdersService.createOrder()` calls inventory via Feign, Resilience4J tracks the last 6 calls. If ≥50% fail, the circuit opens. Subsequent calls skip the Feign call entirely and invoke `createOrderFallback()` for 10 seconds, then half-open to probe recovery.

**Q4: What does `@RefreshScope` do?**  
Allows beans to be re-created without restarting the application. When a `POST /actuator/refresh` is sent, beans annotated with `@RefreshScope` reload their `@Value` properties from Config Server. Used here for the feature flag and test endpoint.

**Q5: How does Config Server work with Git?**  
At startup, Config Server clones the Git repo. When a client requests config for `order-service`, it looks for `order-service.properties` or `order-service.yml` in the repo. Changes pushed to Git can be consumed by clients via `POST /actuator/refresh`.

**Q6: What would happen if Kafka goes down?**  
Order creation would still succeed because the Kafka publish happens after the DB save. The event would be lost (no retry configured). Stock deduction would not happen. Fix: configure a Kafka producer retry or an outbox pattern.

**Q7: How is distributed tracing propagated?**  
Micrometer Tracing adds trace IDs to HTTP headers (via Brave). Feign and Kafka propagate these automatically. Zipkin collects spans from all services, allowing you to visualize the full request path.

**Q8: Why is there no root pom.xml?**  
Each service is an independent Maven project rather than a multi-module build. This allows each service to be built, versioned, and deployed independently — a true microservices approach.

**Q9: Why WebFlux for Gateway but WebMVC for services?**  
Gateway is I/O-bound (proxy, filter, route) and benefits from reactive non-blocking. Business services are compute/DB-bound where WebMVC is simpler and more familiar.

**Q10: What does `spring.cloud.stream.bindings.reserveStock-in-0` mean?**  
`reserveStock` is the bean/function name, `-in-0` means input (consumer) binding index 0. The destination `order.created` is the actual Kafka topic name. The `group` ensures at-least-once delivery within the inventory-service consumer group.

**Q11: How does the Gateway filter know which routes need auth?**  
The `AuthenticationGatewayFilterFactory` has a `.apply(config)` that can be configured per route in the Config Server's gateway config using `filters: - Authentication=true`. Routes without this filter skip JWT validation.

**Q12: What is the outbox pattern and why might this project need it?**  
The outbox pattern saves events in a DB table within the same transaction as the business operation, then a separate process publishes them to Kafka. This project publishes the event after the DB commit — if the publish fails, the event is lost. An outbox table would guarantee delivery.

**Q13: How would you scale the inventory service?**  
Start multiple instances on different ports. They all register with Eureka. Kafka consumer group ensures each event goes to only one instance (workload distribution). The database becomes the bottleneck — read replicas for reads, connection pooling tuning for writes.

**Q14: What is the difference between `@Version` on `Orders` vs missing `@Version` on `Product`?**  
`Orders` has optimistic locking — concurrent updates to the same order throw `OptimisticLockException`, preventing silent overwrites. `Product` has no versioning — concurrent stock deductions can race and oversell. Adding `@Version` to Product would fix this.

**Q15: Why does `createOrderFallback` throw an exception instead of returning a cached response?**  
Design choice (or initial implementation). In production, fallbacks should return a sensible degraded response (e.g., "Service temporarily unavailable, please retry") rather than propagating errors to the client. This is flagged as a high-severity issue.
