# StreamFlow — Real-Time E-Commerce Analytics Pipeline

A production-ready, event-driven analytics platform built with Java 17, Spring Boot 3, Apache Kafka Streams, Redis, PostgreSQL, and Grafana. StreamFlow ingests user interaction events (clicks, purchases, cart additions) and processes them through tumbling, hopping, and session windows to deliver real-time dashboards and leaderboards.

---

## Architecture Overview

```
┌─────────────────┐     ┌───────────────┐     ┌──────────────────────┐
│  event-producer │────▶│     Kafka      │────▶│  stream-processor    │
│   REST API      │     │  user-events  │     │  Kafka Streams       │
└─────────────────┘     └───────────────┘     │  Tumbling  5 min     │
                                               │  Hopping   1 min     │
                                               │  Session   30 min    │
                                               └──────────┬───────────┘
                                                          │
                                              ┌───────────┴───────────┐
                                              │                       │
                                         ┌────▼────┐           ┌──────▼─────┐
                                         │  Redis  │           │ PostgreSQL  │
                                         │Leaderbd │           │  History    │
                                         └────┬────┘           └──────┬──────┘
                                              │                       │
                                         ┌────▼───────────────────────▼──┐
                                         │       analytics-api-service    │
                                         │  Dashboard · Leaderboard · KPIs│
                                         └───────────────────────────────┘
                                                          │
                                              ┌───────────▼───────────┐
                                              │  Prometheus + Grafana  │
                                              │     10-panel Dashboard │
                                              └───────────────────────┘
```

---

## Services

### 1. `event-producer-service` (port 8080)
REST API that accepts user events and publishes them to Kafka.
- `POST /api/v1/events` — publish a single event
- `POST /api/v1/events/batch` — publish a list of events
- Built-in **LoadSimulator** for automatic event generation (weighted: CLICK×5, CART×3, PURCHASE×1, EXIT×1)
- Micrometer counters per event type for Prometheus

### 2. `stream-processor-service` (port 8081)
Kafka Streams processor with three window topologies:
- **Tumbling 5-minute** — fixed non-overlapping product metric windows
- **Hopping 1-minute** — overlapping windows (step: 30s)
- **Session** — activity-based windows with 30-minute inactivity gap
- Writes aggregated results to both Redis (real-time) and PostgreSQL (historical)
- TTL-based active user tracking in Redis

### 3. `analytics-api-service` (port 8082)
REST API for querying aggregated analytics:
- `GET /api/v1/analytics/dashboard` — full dashboard summary
- `GET /api/v1/analytics/leaderboard?topN=10` — top clicked products
- `GET /api/v1/analytics/products/{id}/history` — product metric history
- `GET /api/v1/analytics/active-users` — current active user count
- Redis-first with automatic DB fallback

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17 (records, sealed interfaces, switch expressions) |
| Framework | Spring Boot 3.2 |
| Messaging | Apache Kafka + Kafka Streams 3.6 |
| Cache | Redis 7 (ZSet leaderboard, TTL active users) |
| Database | PostgreSQL 15 |
| Observability | Micrometer + Prometheus + Grafana |
| Containerization | Docker + Docker Compose |
| Testing | JUnit 5, Mockito, TopologyTestDriver |

---

## Getting Started

### Prerequisites
- Docker Desktop
- Java 17+
- Maven 3.9+

### Run Everything with Docker Compose

```bash
git clone https://github.com/<your-username>/streamflow.git
cd streamflow
docker compose up -d
```

All services start automatically. Kafka topics are created on first run.

### Service URLs

| Service | URL |
|---------|-----|
| Event Producer API | http://localhost:8080 |
| Stream Processor | http://localhost:8081/actuator/health |
| Analytics API | http://localhost:8082 |
| Grafana Dashboard | http://localhost:3001 (admin / admin) |
| Prometheus | http://localhost:9090 |
| Kafka UI | http://localhost:8090 |

### Run Locally in IntelliJ

The `event-producer-service` has a `local` Spring profile that:
- Starts on port **8083** (avoids conflict with Docker)
- Enables the **LoadSimulator** (generates events every 2s)

```
VM Options: -Dspring.profiles.active=local
```

---

## Grafana Dashboard

10 panels out of the box:

| Panel | Description |
|-------|-------------|
| Events Per Second | Published / Failed / Simulated rate |
| Active Users | Real-time count from Redis TTL tracker |
| Windows Processed | Total tumbling + hopping windows |
| Sessions Processed | Total session windows |
| Event Type Distribution | Donut chart — CLICK / CART / PURCHASE / EXIT |
| Window Processing Rate by Type | TUMBLING_5M vs HOPPING_1M over time |
| DB Write Duration (p50/p99) | Latency percentiles per window type |
| Conversion Funnel | Bar gauge — full funnel visualization |
| Redis Write Failures | Resilience monitor |
| JVM Heap Usage | All 3 services on one graph |

---

## API Examples

### Publish a Click Event
```bash
curl -X POST http://localhost:8080/api/v1/events \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "sessionId": "session-abc",
    "eventType": "CLICK",
    "productId": 42,
    "price": 99.99
  }'
```

### Get Dashboard Summary
```bash
curl http://localhost:8082/api/v1/analytics/dashboard
```

### Get Click Leaderboard
```bash
curl "http://localhost:8082/api/v1/analytics/leaderboard?topN=5"
```

---

## Running Tests

```bash
# event-producer-service — 11 tests
cd event-producer-service && mvn test

# stream-processor-service — 18 tests (includes TopologyTestDriver)
cd stream-processor-service && mvn test

# analytics-api-service — 6 tests
cd analytics-api-service && mvn test
```

**35 tests total, 0 failures.**

Test highlights:
- `TumblingWindowTopologyTest` — in-process Kafka Streams testing with `TopologyTestDriver` (no broker needed)
- `KafkaEventPublisherTest` — dead-letter queue fallback on Kafka failure
- `ActiveUserTrackerImplTest` — TTL-based new vs. returning user detection
- `MetricsWriterServiceImplTest` — Redis failure does not block DB write

---

## Project Structure

```
streamflow/
├── docker-compose.yml
├── prometheus.yml
├── init-db.sql
├── grafana/
│   └── provisioning/
│       ├── dashboards/
│       └── datasources/
├── event-producer-service/
│   └── src/
│       ├── main/java/com/streamflow/producer/
│       │   ├── controller/        # REST endpoints
│       │   ├── service/           # EventPublisher interface + Kafka impl
│       │   ├── mapper/            # UserEventMapper
│       │   └── model/             # UserEvent record, EventType enum
│       └── test/
├── stream-processor-service/
│   └── src/
│       ├── main/java/com/streamflow/processor/
│       │   ├── streams/topology/  # Tumbling, Hopping, Session topologies
│       │   ├── streams/serde/     # JsonSerde, SerdeFactory
│       │   ├── service/           # MetricsWriter, ActiveUserTracker
│       │   └── model/             # Entities, events, enums
│       └── test/
└── analytics-api-service/
    └── src/
        ├── main/java/com/streamflow/analytics/
        │   ├── controller/        # Analytics REST endpoints
        │   ├── service/           # AnalyticsService + Redis/DB impl
        │   ├── repository/        # JPA repositories
        │   └── dto/               # Response records
        └── test/
```

---

## Design Principles

- **Interface Segregation** — `RedisMetricsWriter`, `DatabaseMetricsWriter`, `MetricsWriterService` are separate interfaces
- **Dependency Inversion** — all services depend on interfaces, constructor injection only
- **Resilience** — Redis failure never blocks PostgreSQL write; dead-letter queue for Kafka failures
- **Observability** — every critical path has a Micrometer counter or timer
- **Exactly-once semantics** — Kafka Streams configured with `exactly_once_v2`

---

## License

MIT
