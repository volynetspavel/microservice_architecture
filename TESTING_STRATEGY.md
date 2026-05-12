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


### How it works

1. The **provider** defines a contract as a Groovy DSL file in `src/test/resources/contracts/`.
2. The `spring-cloud-contract-maven-plugin` reads each contract and **generates a JUnit 5 test class** in `target/generated-test-sources/contracts/`. The generated class extends a hand-written base class that sets up the controller mock, and contains one `@Test` per contract that sends the specified request and asserts the specified response.
3. These generated tests run during the `test` phase (Maven Surefire). If a generated test fails, `mvn verify` fails — the provider's implementation is out of sync with its contract.
4. The plugin also packages a **stubs JAR** (`*-stubs.jar`) containing WireMock mappings derived from the contracts and installs it to the local Maven repository.
5. The **consumer** loads the stubs JAR at test time via `@AutoConfigureStubRunner`. This starts WireMock servers pre-loaded with the provider's contract stubs. The consumer's client beans call those WireMock servers, verifying they send requests that the contract stubs recognise.

---

### Contracts implemented

#### Contract 1 — `DELETE /songs?id=1` (song-service is the provider)

**File:** `song-service/src/test/resources/contracts/songs/shouldDeleteSongsAndReturnIds.groovy`

Defines that when resource-service calls `DELETE /songs?id=1`, song-service responds with `200 {"ids":[1]}`.

**Provider verifier** (`song-service`):
- Generated class: `SongsTest` (extends `SongServiceContractBase`)
- Base class `SongServiceContractBase` loads a minimal Spring context with only `SongController` + `GlobalExceptionHandler`. `SongService` is mocked with `@MockitoBean`; `deleteSongs("1")` is stubbed to return `DeleteSongsResponseDto([1])`.
- No database or infrastructure needed — contract tests verify HTTP shape, not business logic.

**Consumer** (`resource-service`):
- `SongServiceClient.deleteMetadata(Integer id)` calls `DELETE /songs?id={id}` — covered by the provider verifier ensuring the endpoint exists and returns the declared shape. No separate consumer test exists because `deleteMetadata` swallows errors (fire-and-forget), making consumer-side stub assertions meaningless.

---

#### Contract 2 — `GET /resources/1` → `audio/mpeg` binary (resource-service is the provider)

**File:** `resource-service/src/test/resources/contracts/resources/shouldReturnAudioMpegForExistingResource.groovy`

Defines that when resource-processor calls `GET /resources/1`, resource-service responds with `200`, `Content-Type: audio/mpeg`, `Content-Disposition: attachment; filename="resource_1.mp3"`, and binary body matching `test-audio.mp3` (3-byte minimal MP3 header: `0xFF 0xFB 0x00`).

**Provider verifier** (`resource-service`):
- Generated class: `ResourcesTest` (extends `ResourceServiceHttpContractBase`)
- Base class uses **no Spring context at all** — `ResourceController` uses constructor injection, so a plain `Mockito.mock(ResourceService.class)` is constructed directly and wired via `RestAssuredMockMvc.standaloneSetup(new ResourceController(mockService))`. `getResourceById("1")` returns `ResourceDataResponseDto` wrapping the 3-byte fixture.

**Consumer** (`resource-processor`):
- `ResourceProcessorContractTest.getResource_requestMatchesResourceServiceContract()` — loads the resource-service stubs JAR (WireMock on port 8091), points `DiscoveryClient` mock at that port, calls `ResourceServiceClient.getResource(1)`, and asserts the returned `byte[]` is non-empty.

---

#### Contract 3 — `POST /songs` with `SongCreateRequestDto` (song-service is the provider)

**File:** `song-service/src/test/resources/contracts/songs/shouldCreateSongAndReturnId.groovy`

Defines that when resource-processor posts a valid `SongCreateRequestDto` to `POST /songs`, song-service responds with `200 {"id": <integer>}`. The request body matchers use regexes (`[1-9][0-9]*` for id, `.{1,100}` for string fields, `\d{2}:[0-5]\d` for duration, `\d{4}` for year) so the stub matches any valid DTO, not just the hard-coded fixture values.

**Provider verifier** (`song-service`):
- Generated class: `SongsTest` (extends `SongServiceContractBase`, same base as Contract 1)
- `SongService.createSong(dto)` is stubbed to return `SongIdResponseDto(5)` when `dto.id == 5`.

**Consumer** (`resource-processor`):
- `ResourceProcessorContractTest.sendMetadata_requestMatchesSongServiceContract()` — loads the song-service stubs JAR (WireMock on port 8090), points `DiscoveryClient` mock at that port, calls `SongServiceClient.sendMetadata(dto)` with a DTO whose fields satisfy the contract's regex matchers. WireMock rejects the request with 404 if the body does not match, causing the `RestClient` to throw.

---

#### Contract 4 — `ResourceUploadedEvent` RabbitMQ message (resource-service is the provider)

**File:** `resource-service/src/test/resources/contracts/messaging/shouldPublishResourceUploadedEvent.groovy`

Defines that resource-service publishes a message to the `resource-uploaded` exchange with body `{"resourceId": <positive integer>}` and `contentType: application/json`. The `triggeredBy("publishResourceUploadedEvent()")` DSL tells the generated test which method on the base class fires the message.

**Provider verifier** (`resource-service`):
- Generated class: `MessagingTest` (extends `ResourceServiceMessagingContractBase`)
- Base class loads a minimal context — only `ResourceUploadedEventPublisher` via `@Configuration @Import(ResourceUploadedEventPublisher.class)`. `StreamBridge` is provided by the **Spring Cloud Stream test binder** (`spring-cloud-stream-test-binder`) instead of real RabbitMQ. `@AutoConfigureMessageVerifier` wires an in-memory `MessageVerifier` that intercepts messages sent through `StreamBridge`. No broker needed.
- The generated test calls `publishResourceUploadedEvent()` → `publisher.publish(1)` → `StreamBridge.send("resource-uploaded-out-0", ResourceUploadedEvent(1))`. The in-memory verifier captures the message and asserts its body and headers match the contract.

**Consumer** (`resource-processor`):
- `ResourceUploadedEventContractTest.resourceUploadedEvent_consumerCanDeserialize()` — loads the resource-service stubs JAR, uses `StubTrigger` to fire the messaging contract, and uses `MessageVerifierReceiver` to receive the message from the in-memory test binder channel `resource-uploaded`. Asserts the message payload is non-null, confirming the consumer can receive and parse the event shape the provider declares.

---

### Provider base class design decisions

| Provider | Base class | Context strategy | Why |
|---|---|---|---|
| song-service | `SongServiceContractBase` | `@SpringBootTest(classes={SongController, GlobalExceptionHandler})` + `@MockitoBean SongService` | `SongController` uses `@Autowired` field injection — must be wired by Spring |
| resource-service (HTTP) | `ResourceServiceHttpContractBase` | No Spring context — pure `RestAssuredMockMvc.standaloneSetup(new ResourceController(mock))` | `ResourceController` uses constructor injection — no Spring needed |
| resource-service (messaging) | `ResourceServiceMessagingContractBase` | `@SpringBootTest(classes=TestConfig)` with `@Configuration @Import(ResourceUploadedEventPublisher)` + test binder | Only `StreamBridge` needed; minimal context avoids pulling in S3/JPA/RabbitMQ |

**Why 10% and not more:** These four contracts cover every cross-service HTTP and messaging interface. Additional contracts would duplicate assertions already present in component and integration tests.

---

## 5. End-to-End Tests — 5%

**Goal:** Smoke-test the fully assembled system on the three core business flows. Runs against the real `compose.yaml` environment.

**E2E approach:** Run script run-e2e-test.sh which is running docker containers from compose.yaml and then triggering ResourceServiceE2E.java from Resource Service.

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
