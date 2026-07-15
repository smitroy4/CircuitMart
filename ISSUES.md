# CircuitMart — Codebase Audit Report

> Generated: Thu Jul 16 2026
> Scope: All modules (api-gateway, config-server, discovery-service, inventory-service, order-service)

---

## Summary

| Severity | Count |
|---|---|
| Critical | 3 |
| High | 17 |
| Medium | 30 |
| Low | 12 |
| **Total** | **62** |

| Category | Count |
|---|---|
| Bug | 22 |
| Code Smell | 19 |
| Security | 5 |
| Configuration | 9 |
| Anti-Pattern | 4 |
| Documentation | 3 |
| Missing Implementation | 2 |
| Performance | 1 |
| Test Coverage | 1 |

---

## Project-Level / Cross-Cutting Issues

| # | File | Line | Description | Severity | Category |
|---|---|---|---|---|---|
| 1 | Root directory | — | **No parent POM (multi-module Maven project).** Each service is an independent Maven project with its own `spring-boot-starter-parent`. There is no root `pom.xml` to manage the project as a unified multi-module build, making coordinated version management, build orchestration, and CI/CD significantly harder. | High | Configuration |
| 2 | `config-server/pom.xml` | 8 | **Inconsistent Spring Boot version.** Config-server uses Spring Boot `4.0.7` while all other services use `4.1.0`. This can cause subtle compatibility issues with Spring Cloud dependencies and inter-service communication. | High | Configuration |
| 3 | All `pom.xml` files | — | **Empty POM metadata.** All POMs have empty `<name/>`, `<description/>`, `<url/>`, `<license/>`, `<developer/>`, and SCM elements. Maven best practice dictates these should be populated or removed entirely. | Low | Code Smell |
| 4 | `.gitignore` | 1 | **Insufficient .gitignore.** Only ignores `.env`. Missing: `target/`, `.idea/`, `*.iml`, `.mvn/`, `*.class`, `*.jar`, etc. The `.idea/` directory and `target/` directories may already be tracked. | Medium | Configuration |
| 5 | `README.md` | 116-117 | **README prerequisite inconsistency.** Prerequisites state "Java 21+" but all POMs specify Java 25 and the badge says Java 25. | Low | Documentation |
| 6 | All services | — | **No global exception handler.** No `@RestControllerAdvice` or `@ExceptionHandler` exists anywhere. All unhandled exceptions return generic 500 Internal Server Error with stack traces to the client. | High | Bug |
| 7 | All services | — | **No input validation.** No `@Valid`, `@NotNull`, `@NotBlank`, `@Positive` annotations exist anywhere. No Bean Validation starter (`spring-boot-starter-validation`) is included in any POM. Controllers accept arbitrary payloads without validation. | High | Bug |
| 8 | All services | — | **Minimal test coverage.** All five test classes contain only an empty `contextLoads()` test. There are zero unit tests, zero integration tests, and zero controller/service tests. | High | Test Coverage |
| 9 | All services | — | **No Docker/containerization support.** No `Dockerfile` or `docker-compose.yml` exists, despite this being a microservices architecture intended for cloud-native deployment. | Medium | Configuration |
| 10 | All services | — | **No `.env.example` file.** The `.gitignore` excludes `.env` and the project depends on environment variables (e.g., `GITHUB_ACCESS_TOKEN`, `jwt.SecretKey`), but no template or documentation of required environment variables is provided. | Medium | Documentation |
| 11 | All services | — | **No logging configuration file.** No `logback-spring.xml` or `log4j2.xml` exists. All services rely on Spring Boot defaults. Given that the project claims ELK stack integration, proper structured logging configuration (JSON format) is needed. | Medium | Configuration |
| 12 | All services | — | **Duplicate DTOs across services.** `OrderRequestDto` and `OrderRequestItemDto` are defined independently in both `order-service` and `inventory-service` with different fields. In a microservices architecture, this leads to drift and should use a shared contract or API-first approach. | Medium | Anti-Pattern |
| 13 | All services | — | **No actuator security.** Actuator endpoints (`spring-boot-starter-actuator`) are included in all services but no security configuration restricts access to sensitive endpoints like `/actuator/env`, `/actuator/configprops`, `/actuator/beans`. | High | Security |
| 14 | All services | — | **No CORS configuration.** No CORS configuration exists anywhere. The API Gateway as a single entry point should configure CORS, but it does not. | Medium | Configuration |

---

## API-Gateway Module

| # | File | Line | Description | Severity | Category |
|---|---|---|---|---|---|
| 15 | `api-gateway/pom.xml` | 90-92 | **Unnecessary `feign-micrometer` dependency.** The API Gateway does not use OpenFeign to make outbound HTTP calls. This dependency is unused and bloats the artifact. | Low | Code Smell |
| 16 | `config/AppConfig.java` | 12-15 | **Dead bean: `MicrometerCapability`.** Defines a Feign `Capability` bean, but the gateway has no Feign clients. This bean is instantiated but never used. | Medium | Code Smell |
| 17 | `service/JwtService.java` | 31 | **`System.out.println` in production code.** `System.out.println("JWT SUBJECT = " + claims.getSubject())` should use a SLF4J logger. `System.out` bypasses the logging framework and cannot be filtered/leveled. | Medium | Code Smell |
| 18 | `service/JwtService.java` | 19-21 | **SecretKey recreated on every call.** `getSecretKey()` creates a new `SecretKey` instance on every request. This should be cached as a field (initialized once via `@PostConstruct` or constructor). | Medium | Performance |
| 19 | `service/JwtService.java` | 16 | **No default value for `jwt.SecretKey`.** If the property is not supplied, `jwtSecretKey` will be null, causing a `NullPointerException` in `Keys.hmacShaKeyFor(null.getBytes(...))` at runtime. | High | Bug |
| 20 | `service/JwtService.java` | 20 | **No key length validation.** `Keys.hmacShaKeyFor()` requires at least 256 bits (32 bytes). If the configured secret is shorter, it throws an `IllegalArgumentException` with no meaningful error message. | Medium | Bug |
| 21 | `service/JwtService.java` | 16 | **Non-standard property naming.** `jwt.SecretKey` uses PascalCase, violating Spring Boot's `kebab-case` convention (should be `jwt.secret-key`). | Low | Code Smell |
| 22 | `service/JwtService.java` | 36-45 | **Unused method `getUserRoleFromToken`.** This method is defined but never called anywhere in the codebase. Roles extracted from the JWT are never used for authorization. | Medium | Code Smell |
| 23 | `filters/AuthenticationGatewayFilterFactory.java` | 30-69 | **No role-based access control.** The filter extracts the JWT subject and forwards it as `X-User-Id` but never extracts or checks roles. All authenticated users have identical access. | High | Security |
| 24 | `filters/LoggingOrdersFilter.java` | 17-21 | **Incomplete filter: only pre-logging, no post-logging.** Logs the request URI before the chain but does not log the response status code. Inconsistent with `GlobalLoggingFilter` which logs both pre and post. | Low | Code Smell |
| 25 | `ApiGatewayApplication.java` | 20 | **`System.out.println` in production code.** `System.out.println("ROUTE FOUND => " + route.getId())` should use a SLF4J logger. | Low | Code Smell |

---

## Config-Server Module

| # | File | Line | Description | Severity | Category |
|---|---|---|---|---|---|
| 26 | `config-server/pom.xml` | 64-67 | **Unnecessary `feign-micrometer` dependency.** The Config Server does not use Feign clients. This dependency is unused. | Low | Code Smell |
| 27 | `application.yml` | 10 | **Hardcoded GitHub username.** `username: smitroy4` is hardcoded in the configuration file. While the token uses an environment variable, the username should also be externalized. | Medium | Security |
| 28 | `application.yml` | 11 | **GitHub token referenced but no .env documented.** The `${GITHUB_ACCESS_TOKEN}` environment variable is required but there is no documentation or `.env.example` showing this. | Medium | Documentation |
| 29 | `config-server/pom.xml` | 69-72 | **`spring-dotenv` dependency potentially unnecessary.** The Config Server includes `spring-dotenv` but there is no `.env` file or evidence it is being used. | Low | Code Smell |
| 30 | `config-server/pom.xml` | 8 | **Spring Boot version mismatch (4.0.7 vs 4.1.0).** Config-server uses a different Spring Boot version than all other modules. | High | Configuration |

---

## Discovery-Service Module

| # | File | Line | Description | Severity | Category |
|---|---|---|---|---|---|
| 31 | `discovery-service/pom.xml` | 56-59 | **Unnecessary `feign-micrometer` dependency.** The Eureka Server does not make outbound Feign calls. | Low | Code Smell |
| 32 | `discovery-service/pom.xml` | 61-64 | **`spring-dotenv` dependency potentially unnecessary.** No `.env` file or dotenv usage is evident in this service. | Low | Code Smell |

---

## Inventory-Service Module

| # | File | Line | Description | Severity | Category |
|---|---|---|---|---|---|
| 33 | `controller/ProductController.java` | 23 | **Unused injected field: `DiscoveryClient`.** The `discoveryClient` field is injected via constructor but never referenced in any method. Dead code. | Medium | Code Smell |
| 34 | `controller/ProductController.java` | 24 | **Unused injected field: `RestClient`.** The `restClient` field is injected via constructor but never referenced in any method. Dead code. | Medium | Code Smell |
| 35 | `config/AppConfig.java` | 17-19 | **Unused `RestClient` bean.** Defines a `RestClient` bean that is injected into `ProductController` but never actually used for any HTTP call. | Medium | Code Smell |
| 36 | `service/ProductService.java` | 40-58 | **Race condition in stock reduction.** `reduceStocks()` reads product stock, checks it, and writes it back without any concurrency control. No `@Version` optimistic locking on `Product`, no pessimistic lock, no `SELECT ... FOR UPDATE`. Two concurrent orders could oversell inventory. | Critical | Bug |
| 37 | `entity/Product.java` | 23 | **Missing `@Version` for optimistic locking.** The `Product` entity has no version field for JPA optimistic locking, making concurrent stock modifications unsafe. | High | Bug |
| 38 | `entity/Product.java` | 23 | **`Double` used for monetary value.** `price` is declared as `Double`. Floating-point arithmetic is unreliable for financial calculations due to precision issues (e.g., `0.1 + 0.2 != 0.3`). Should use `BigDecimal`. | High | Bug |
| 39 | `service/ProductService.java` | 42 | **`Double` used for total price calculation.** `totalPrice` is `Double` and accumulated via `+=`. This compounds floating-point precision errors across multiple line items. | High | Bug |
| 40 | `controller/ProductController.java` | 44-50 | **No input validation on `reduceStocks`.** The `OrderRequestDto` body is accepted without `@Valid`. Null items, negative quantities, or missing product IDs will cause unhandled `NullPointerException` or `RuntimeException`. | High | Bug |
| 41 | `service/ProductService.java` | 36 | **Generic `RuntimeException` for not-found.** `new RuntimeException("Inventory not found")` results in HTTP 500 instead of a proper 404 Not Found. Should use `ResponseStatusException` or a custom exception. | Medium | Bug |
| 42 | `service/ProductService.java` | 48 | **Generic `RuntimeException` for not-found product.** Same issue: `RuntimeException("Product not found with id: ...")` returns 500 instead of 404. | Medium | Bug |
| 43 | `service/ProductService.java` | 51 | **Generic `RuntimeException` for insufficient stock.** `RuntimeException("Product cannot be fulfilled for given quantity")` returns 500 instead of 409 Conflict or 400 Bad Request. | Medium | Bug |
| 44 | `service/ProductService.java` | 14 | **Unused import: `java.util.Optional`.** The `Optional` type is used but can be eliminated by using `findById().orElseThrow()` directly. | Low | Code Smell |
| 45 | `client/OrdersFeignClient.java` | 9 | **Feign client path mismatch (BUG).** The Feign method path is `@GetMapping("/core/helloOrders")`, making the full call path `/orders/core/helloOrders`. But the actual endpoint in `OrdersController` is `@GetMapping("/helloOrders")` under `@RequestMapping("/orders")`, which resolves to `/orders/helloOrders`. The `/core/` segment does not exist. This call will always fail with 404. | Critical | Bug |
| 46 | `controller/ProductController.java` | 27-30 | **Test/debug endpoint exposed in production.** `fetchFromOrdersService()` calls `ordersFeignClient.helloOrders()` which is a hello-world test endpoint. This should not be in a production controller. | Medium | Code Smell |
| 47 | `resources/data.sql` | 1-21 | **`data.sql` likely never executes.** In Spring Boot 3.x+, `data.sql` scripts are only run for embedded databases by default. For PostgreSQL, `spring.sql.init.mode=always` and `spring.jpa.defer-datasource-initialization=true` are required. Neither is configured in `application.properties`. Seed data will silently never load. | High | Bug |
| 48 | `controller/ProductController.java` | 44 | **`PUT` for stock reduction is semantically incorrect.** `@PutMapping("/reduce-stocks")` should be `@PostMapping` since it is an action/command, not an idempotent resource replacement. | Low | Anti-Pattern |
| 49 | `controller/ProductController.java` | — | **Missing POST endpoint for creating products.** No way to add new products to inventory through the API. | Medium | Missing Implementation |
| 50 | `controller/ProductController.java` | — | **Missing DELETE endpoint for products.** No way to remove products from inventory through the API. | Medium | Missing Implementation |
| 51 | `inventory-service/pom.xml` | — | **Missing `spring-boot-starter-test` base dependency.** Only has specialized test starters (`actuator-test`, `data-jpa-test`, `webmvc-test`) but not the base `spring-boot-starter-test` which provides JUnit 5, Mockito, AssertJ, etc. | High | Configuration |
| 52 | `service/ProductService.java` | 39-58 | **Missing rollback handling.** If an exception occurs mid-loop in `reduceStocks()` (e.g., product not found on the 3rd item), previously reduced stocks in the same transaction will be rolled back. However, the Feign call to `inventory-service` in `order-service` has already been made — the order-service has no way to know the reduction was rolled back, leading to inconsistent state. | High | Bug |

---

## Order-Service Module

| # | File | Line | Description | Severity | Category |
|---|---|---|---|---|---|
| 53 | `controller/OrdersController.java` | 25 | **Unused injected field: `InventoryFeignClient`.** The `inventoryFeignClient` is injected into the controller but never used. It is used in `OrdersService`, not in the controller. Dead code. | Medium | Code Smell |
| 54 | `service/OrdersService.java` | 58-61 | **Fallback returns empty DTO on failure.** `createOrderFallback()` returns `new OrderRequestDto()` (all null fields) with HTTP 200 OK. This is misleading — the client receives a success response for a failed operation. Should return 503 or 500. | Critical | Bug |
| 55 | `service/OrdersService.java` | 39-41 | **Commented-out annotations.** `@Retry` and `@RateLimiter` are commented out with `//`. Dead code left in place. | Low | Code Smell |
| 56 | `controller/OrdersController.java` | 32-34 | **Commented-out code.** Lines with `@RequestHeader("X-User-Id")` and a `log.info` call are commented out. Should be removed or restored. | Low | Code Smell |
| 57 | `controller/OrdersController.java` | 28-29 | **No default for `@Value("${my.variable}")`.** If this property is not provided by the Config Server, the application will fail to start with an `IllegalArgumentException`. | High | Bug |
| 58 | `controller/OrdersController.java` | 21 | **`@RefreshScope` on controller.** While functional, `@RefreshScope` on a controller creates a CGLIB proxy that is recreated on config refresh. This can cause issues with request-scoped beans and is generally recommended only on `@Configuration` or `@Component` classes. | Medium | Anti-Pattern |
| 59 | `entity/Orders.java` | 26 | **`Double` used for monetary value.** Same issue as inventory-service: `totalPrice` uses `Double` instead of `BigDecimal`. | High | Bug |
| 60 | `entity/Orders.java` | — | **Missing `@Version` for optimistic locking.** No version field exists on the `Orders` entity. | Medium | Bug |
| 61 | `dto/OrderRequestDto.java` | 10 | **`id` field in request DTO.** The `OrderRequestDto` has a `private Long id` field. For a create-order request, the client should not be setting the order ID. This could be exploited to inject arbitrary IDs. | Medium | Security |
| 62 | `dto/OrderRequestItemDto.java` | 7 | **`id` field in request DTO.** Same issue: `private Long id` in `OrderRequestItemDto` allows clients to inject arbitrary item IDs. | Medium | Security |
| 63 | `dto/OrderRequestDto.java` | 12 | **Type mismatch between DTO and entity.** `OrderRequestDto.totalPrice` is `BigDecimal` but `Orders.totalPrice` is `Double`. ModelMapper may silently lose precision during conversion or fail. | Medium | Bug |
| 64 | `service/OrdersService.java` | 46 | **Potential ModelMapper infinite recursion.** Mapping `Orders` -> `OrderRequestDto` via ModelMapper will try to map `Orders.items` (List of OrderItem) to `OrderRequestDto.items` (List of OrderRequestItemDto). Since `OrderItem` has a back-reference to `Orders`, this could cause infinite recursion without proper ModelMapper configuration. | High | Bug |
| 65 | `service/OrdersService.java` | 44 | **No null check on Feign response.** `inventoryFeignClient.reduceStocks(orderRequestDto)` could return null (especially from the fallback). The subsequent `orders.setTotalPrice(totalPrice)` would set null, and arithmetic operations would throw NPE. | Medium | Bug |
| 66 | `service/OrdersService.java` | — | **Missing `spring-boot-starter-test` base dependency.** Same as inventory-service: only has specialized test starters. | High | Configuration |
| 67 | `service/OrdersService.java` | 40 | **Circuit breaker with no configuration.** `@CircuitBreaker(name = "inventoryCircuitBreaker")` references a named config, but no Resilience4j configuration is in any local properties file. Configuration is presumably on the Config Server, but if unavailable, defaults may not be appropriate. | Medium | Configuration |
| 68 | `controller/OrdersController.java` | 50 | **Unused parameter: `HttpServletRequest`.** The `getAllOrders` method accepts `HttpServletRequest httpServletRequest` but never uses it. | Low | Code Smell |
| 69 | `config/FeaturesEnableConfig.java` | 8-10 | **`@RefreshScope` on `@Configuration` class.** `@RefreshScope` on a `@Configuration` class can cause issues with bean lifecycle. The beans defined in the class are re-created on refresh, but `@Configuration` classes have specific proxy behaviors. `@ConfigurationProperties` with `@RefreshScope` would be cleaner. | Medium | Anti-Pattern |

---

## Top 10 Issues Requiring Immediate Attention

| Priority | Issue | Location |
|---|---|---|
| 1 | **Race condition in stock reduction** — No concurrency control allows overselling inventory under concurrent load | `inventory-service/.../ProductService.java:40-58` |
| 2 | **Feign client path mismatch** — Calls `/core/helloOrders` but endpoint is at `/helloOrders` (always 404) | `inventory-service/.../OrdersFeignClient.java:9` |
| 3 | **Circuit breaker fallback returns empty 200** — Silent failure sends misleading success response | `order-service/.../OrdersService.java:58-61` |
| 4 | **`data.sql` never executes** — PostgreSQL requires explicit config that is missing | `inventory-service/resources/data.sql` |
| 5 | **No global exception handler** — All exceptions leak as 500 errors | All services |
| 6 | **No input validation** — All endpoints accept unvalidated input | All services |
| 7 | **Floating-point for money** — `Double` used instead of `BigDecimal` | `inventory-service/.../Product.java:23`, `order-service/.../Orders.java:26` |
| 8 | **Inconsistent Spring Boot versions** — config-server 4.0.7 vs 4.1.0 | `config-server/pom.xml:8` |
| 9 | **No actuator security** — Sensitive endpoints publicly accessible | All services |
| 10 | **Potential ModelMapper infinite recursion** — Entity-to-DTO mapping with bidirectional relationships | `order-service/.../OrdersService.java:46` |
