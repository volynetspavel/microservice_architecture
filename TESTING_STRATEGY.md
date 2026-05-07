# Testing Strategy

## Overview

This document describes the testing strategy for the microservice architecture project. The system consists of four services — **Resource Service**, **Song Service**, **Resource Processor**, and **Eureka Server** — connected through REST APIs, RabbitMQ messaging, PostgreSQL databases, and S3 storage.

The strategy follows a **modified testing pyramid** tuned for microservices. Because services are small but heavily wired to external systems (databases, message queues, object storage, peer services), the weight shifts away from pure unit tests toward integration and component tests.

```
         ┌──────────┐
         │  E2E (5%)│
        ┌┴──────────┴┐
        │Contract(10%)│
       ┌┴─────────────┴┐
       │ Component (25%)│
      ┌┴───────────────┴┐
      │ Integration (30%)│
     ┌┴─────────────────┴┐
     │   Unit Tests (30%) │
     └────────────────────┘
```

---

## 1. Unit Tests — 30%

**Goal:** Verify isolated business logic with all external dependencies mocked.

**Tools:** JUnit 5 + Mockito (via `spring-boot-starter-test`)

**What to test:**

- `SongService` — ID uniqueness validation, CSV string parsing, 200-character limit enforcement
- `ResourceService` — upload/delete orchestration logic, CSV parsing, ID format validation
- `Mp3MetadataExtractor` — duration conversion to `MM:SS` format, year sanitization to 4-digit `YYYY`, graceful degradation to defaults on parse failure
- `EurekaServiceResolver` — service URL construction, exception when no instances are registered
- Controllers — `@Valid` constraint enforcement, correct HTTP status codes per scenario

**Why this weight:** Pure logic bugs are best caught here at maximum speed. However, unit tests alone cannot cover infrastructure interactions, so this layer is deliberately not the majority.

---

## 2. Integration Tests — 30%

**Goal:** Verify that each service wires correctly to its own infrastructure — database, object storage, and message broker — using real containers.

**Tools:** `spring-boot-starter-test` + **Testcontainers** (PostgreSQL, LocalStack, RabbitMQ modules)

**What to test:**

| Scope | Container | What to verify |
|---|---|---|
| `ResourceRepository` + PostgreSQL | `postgres:17-alpine` | CRUD operations, auto-generated IDs, constraint violations |
| `SongRepository` + PostgreSQL | `postgres:17-alpine` | CRUD operations, duplicate primary key rejection |
| `CloudStorageService` + S3 | LocalStack | File upload, download, delete; bucket creation lifecycle |
| `ResourceUploadedEventPublisher` + RabbitMQ | RabbitMQ | Message published with correct routing key and payload |
| `ResourceUploadedConsumer` + RabbitMQ | RabbitMQ | Consumer receives event and triggers the processing pipeline |

**Why Testcontainers over H2:** PostgreSQL has different behavior from H2 on sequence semantics, constraint names, and data types. Using the same database engine as production eliminates an entire class of false-positive test passes.

---

## 3. Component Tests — 25%

**Goal:** Test a single service as a black box through its HTTP API with the full Spring context loaded. Peer services are replaced with stubs; the service's own infrastructure runs in containers.

**Tools:** `@SpringBootTest` + `MockMvc` + **WireMock** + Testcontainers

**What to test:**

| Service | Scenario |
|---|---|
| Resource Service | `POST /resources` — MP3 stored in S3, record saved to DB, event published to RabbitMQ, downstream Song Service call stubbed |
| Resource Service | `DELETE /resources?id=1,2` — files removed from S3, records deleted from DB, Song Service delete stubbed |
| Song Service | Full CRUD lifecycle via HTTP; validation failures return `400`; not-found returns `404`; duplicate returns `409` |
| Resource Processor | Incoming RabbitMQ event → stubbed Resource Service `GET` → metadata extracted → stubbed Song Service `POST` |

**Why 25%:** These tests provide the highest confidence per test because they exercise the full Spring wiring — serialization, exception handlers, content negotiation, and service orchestration — without the complexity and fragility of a full cluster.

---

## 4. Contract Tests — 10%

**Goal:** Verify that the API shape a consumer expects matches exactly what the provider delivers. This prevents silent breaking changes when services are deployed independently.

**Tool:** **Spring Cloud Contract** (provider-side generated verifiers + consumer stub JARs)

**Contracts to define:**

| Consumer | Provider | Interface |
|---|---|---|
| Resource Service | Song Service | `DELETE /songs?id={csv}` — response body shape and HTTP status |
| Resource Processor | Resource Service | `GET /resources/{id}` — binary `audio/mpeg` response |
| Resource Processor | Song Service | `POST /songs` with `SongCreateRequestDto` — `200` with `SongIdResponseDto` |
| Resource Processor | Resource Service | `ResourceUploadedEvent` RabbitMQ message — field names and types |

**How it works:**
1. The provider defines contracts as Groovy or YAML DSL files in `src/test/resources/contracts/`.
2. Spring Cloud Contract generates and runs a `@SpringBootTest`-based verifier on the provider during `mvn verify`.
3. A stubs JAR is published to the local Maven repository; consumers reference it via `@AutoConfigureStubRunner` in their component tests.

**Why 10% and not more:** These four contracts cover every cross-service interface. Additional contracts would describe behavior already covered by component and integration tests.

---

## 5. End-to-End Tests — 5%

**Goal:** Smoke-test the fully assembled system on the three core business flows. Runs against the real `compose.yaml` environment.

**Tools:** REST Assured (or `RestTemplate`) in a dedicated `e2e-tests` Maven module

**Flows to cover:**

1. **Upload:** `POST /resources` with an MP3 file → assert resource ID returned → poll until `GET /songs/{id}` returns extracted metadata
2. **Retrieval:** `GET /resources/{id}` → assert binary `audio/mpeg` response with correct Content-Type
3. **Delete:** `DELETE /resources?id={id}` → assert `GET /resources/{id}` returns `404` and `GET /songs/{id}` returns `404`

**Why only 5%:** End-to-end tests are slow (Docker startup time), sensitive to timing, and expensive to maintain. Three flows covering the main happy path are sufficient; all edge cases belong in lower layers where they are cheaper to run and easier to debug.

---

## Why This Combination

No single layer is redundant — each catches a class of failure the others cannot:

| Layer | Failure class caught |
|---|---|
| Unit | Incorrect business rules, edge-case logic, input validation |
| Integration | DB constraint violations, S3 lifecycle errors, MQ routing issues |
| Component | Spring wiring gaps, serialization errors, exception handler coverage |
| Contract | Breaking API changes introduced independently by provider teams |
| E2E | System-level integration failures, Docker networking, async timing |

A 100% unit-test approach is explicitly rejected: the most likely failure modes in this system occur at infrastructure and cross-service boundaries, not in pure algorithmic logic. Mocking all infrastructure in unit tests would give a false sense of coverage while missing the exact failures that cause production incidents.

---

## Tooling Summary

| Tool | Layers |
|---|---|
| JUnit 5 + Mockito | Unit |
| Testcontainers (PostgreSQL, LocalStack, RabbitMQ) | Integration, Component |
| WireMock | Component |
| Spring Cloud Contract | Contract |
| REST Assured | E2E |
| `spring-boot-starter-test` | All layers |
