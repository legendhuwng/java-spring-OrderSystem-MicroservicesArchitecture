# 🛒 Order System – Microservices Architecture

> A production-ready microservices system implementing **Saga Orchestration**, **Transactional Outbox**, **Idempotency**, **Circuit Breaker**, and more.

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Architecture](#-architecture)
- [Services](#-services)
- [Design Patterns](#-design-patterns)
- [Tech Stack](#-tech-stack)
- [Getting Started](#-getting-started)
- [API Reference](#-api-reference)
- [Order Flow](#-order-flow)
- [Saga Rollback](#-saga-rollback)
- [Security](#-security)
- [Monitoring](#-monitoring)
- [Project Structure](#-project-structure)

---

## 🎯 Overview

This project demonstrates a **microservices-based order management system** built with Spring Boot 3, Kafka, and Oracle XE. It covers the full lifecycle of an order — from creation to delivery — with proper handling of failures, rollbacks, and resilience patterns.

**Key highlights:**
- Full **Saga Orchestration** with compensating transactions
- **Transactional Outbox Pattern** to guarantee message delivery
- **Idempotent consumers** to prevent duplicate processing
- **Circuit Breaker** at the API Gateway for fault tolerance
- **JWT Security** protecting all endpoints
- **Dead Letter Queue (DLQ)** for failed message recovery
- **Database per Service** for true service independence

---

## 🏗 Architecture

```
                        ┌─────────────────────────────────────────────┐
                        │              CLIENT (curl / Postman)         │
                        └────────────────────┬────────────────────────┘
                                             │ HTTP + JWT
                        ┌────────────────────▼────────────────────────┐
                        │               API GATEWAY :8080              │
                        │   • JWT Authentication Filter                │
                        │   • Circuit Breaker (Resilience4j)           │
                        │   • Correlation ID Filter                    │
                        │   • Load Balancing (Eureka)                  │
                        └──┬───────────┬──────────────┬───────────────┘
                           │           │              │
           ┌───────────────▼──┐  ┌─────▼──────┐  ┌──▼─────────────┐
           │  ORDER SERVICE   │  │ INVENTORY  │  │    PAYMENT     │
           │     :8081        │  │  SERVICE   │  │    SERVICE     │
           │                  │  │   :8082    │  │     :8083      │
           │ • Saga           │  │            │  │                │
           │   Orchestrator   │  │ • Reserve  │  │ • Mock payment │
           │ • Outbox Pattern │  │   stock    │  │ • 30% failure  │
           │ • REST API       │  │ • Optimis- │  │   simulation   │
           │                  │  │   tic Lock │  │                │
           └────────┬─────────┘  └────────────┘  └────────────────┘
                    │
           ┌────────▼─────────┐  ┌────────────┐  ┌────────────────┐
           │  SHIPPING SERVICE│  │NOTIFICATION│  │    SERVICE     │
           │     :8084        │  │  SERVICE   │  │   REGISTRY     │
           │                  │  │   :8085    │  │  (Eureka):8761 │
           │ • Create         │  │            │  │                │
           │   shipment       │  │ • Log all  │  │ • Service      │
           │ • Tracking #     │  │   events   │  │   discovery    │
           └──────────────────┘  └────────────┘  └────────────────┘

                    ┌──────────────────────────────────┐
                    │           KAFKA :9092            │
                    │  order-created                   │
                    │  inventory-reserved              │
                    │  inventory-failed                │
                    │  payment-success                 │
                    │  payment-failed                  │
                    │  shipping-created                │
                    │  inventory-release-command       │
                    │  *.DLT (Dead Letter Topics)      │
                    └──────────────────────────────────┘

                    ┌──────────────────────────────────┐
                    │         ORACLE XE :1521          │
                    │  orderuser     → ORDERS DB       │
                    │  inventoryuser → INVENTORY DB    │
                    │  paymentuser   → PAYMENTS DB     │
                    │  shippinguser  → SHIPMENTS DB    │
                    └──────────────────────────────────┘
```

---

## 🧩 Services

| Service | Port | Responsibility | Database User |
|---|---|---|---|
| **Service Registry** | 8761 | Eureka – service discovery | — |
| **API Gateway** | 8080 | JWT auth, circuit breaker, routing | — |
| **Order Service** | 8081 | Saga orchestrator, order lifecycle | `orderuser` |
| **Inventory Service** | 8082 | Stock reservation & release | `inventoryuser` |
| **Payment Service** | 8083 | Payment processing (mock) | `paymentuser` |
| **Shipping Service** | 8084 | Shipment creation & tracking | `shippinguser` |
| **Notification Service** | 8085 | Event logging / email (mock) | — |

---

## 🎨 Design Patterns

### 1. Saga Orchestration Pattern

Order Service acts as the **central orchestrator** — it controls the entire flow and decides what happens next after each step.

```
Order Service (Orchestrator)
    │
    ├──► publishes order-created
    │        └──► Inventory Service reserves stock
    │                 ├── success → publishes inventory-reserved
    │                 └── fail   → publishes inventory-failed
    │
    ├──► on inventory-reserved → triggers Payment Service
    │        └──► Payment Service processes payment
    │                 ├── success → publishes payment-success
    │                 └── fail   → publishes payment-failed
    │
    └──► on payment-success → Shipping Service creates shipment
             └──► publishes shipping-created → Order COMPLETED
```

### 2. Transactional Outbox Pattern

Solves the **dual-write problem**: saving to DB and publishing to Kafka in one atomic operation.

```
┌────────────────────────────────────────────────────┐
│                  @Transactional                     │
│                                                     │
│   1. INSERT INTO orders (...)                       │
│   2. INSERT INTO outbox_events (status=PENDING)     │
│                                                     │
└────────────────────────────────────────────────────┘
                        │
          ┌─────────────▼─────────────┐
          │   OutboxScheduler         │
          │   @Scheduled every 5s     │
          │                           │
          │   SELECT * FROM outbox    │
          │   WHERE status = PENDING  │
          │                           │
          │   kafkaTemplate.send(...) │
          │   UPDATE status=PUBLISHED │
          └───────────────────────────┘
```

**Benefit:** If Kafka is temporarily down, orders are not lost. The scheduler will retry automatically.

### 3. Idempotency Pattern

Every Kafka consumer checks a `PROCESSED_EVENTS` table before processing to **prevent duplicate execution**.

```java
// Before processing any event:
if (processedEventRepository.existsByEventIdAndEventType(orderId, eventType)) {
    log.warn("Duplicate event, skipping: orderId={}", orderId);
    return;  // safe to skip
}
```

Applied in: **Inventory**, **Payment**, **Shipping** services.

### 4. Compensating Transaction (Saga Rollback)

When payment fails, the system automatically **rolls back** inventory reservation.

```
payment-failed event received
    │
    ▼
Order Service
    ├── Update order status → CANCELLED
    └── Publish inventory-release-command
              │
              ▼
         Inventory Service
              └── Release reserved stock
```

### 5. Optimistic Locking

Inventory Service uses `@Version` on the `Inventory` entity to **prevent race conditions** when multiple requests try to reserve the same stock simultaneously.

```java
@Version
private Long version;  // Hibernate checks version on every UPDATE
```

### 6. Database per Service

Each service has its own **isolated Oracle schema** — no shared tables, no shared connections.

```
orderuser     ──► ORDERS, ORDER_ITEMS, OUTBOX_EVENTS
inventoryuser ──► INVENTORY, PROCESSED_EVENTS
paymentuser   ──► PAYMENTS, PROCESSED_EVENTS
shippinguser  ──► SHIPMENTS, PROCESSED_EVENTS
```

### 7. Circuit Breaker

API Gateway uses **Resilience4j** to stop cascading failures.

```
Normal state (CLOSED):   All requests pass through
                                  │
              50% failure rate exceeded
                                  │
Open state (OPEN):        All requests → fallback response (503)
                          No requests hit the failing service
                                  │
              After 10 seconds wait
                                  │
Half-open state:          3 test requests allowed
                          ├── Success → back to CLOSED
                          └── Fail   → back to OPEN
```

### 8. Dead Letter Queue (DLQ)

Failed Kafka messages are automatically retried **3 times** (2 seconds apart), then moved to a `.DLT` topic for manual inspection.

```
Message processing fails
    │
    ├── Retry 1 (after 2s)
    ├── Retry 2 (after 2s)
    ├── Retry 3 (after 2s)
    │
    └── All retries exhausted → publish to order-created.DLT
```

---

## 🛠 Tech Stack

| Category | Technology |
|---|---|
| Framework | Spring Boot 3.5.x, Spring Cloud 2025.0.1 |
| Language | Java 17 |
| Messaging | Apache Kafka 3.9 (via Confluent CP 7.6.1) |
| Database | Oracle XE 21c |
| Service Discovery | Netflix Eureka |
| API Gateway | Spring Cloud Gateway (WebFlux) |
| Circuit Breaker | Resilience4j |
| Security | JWT (jjwt 0.12.6) |
| ORM | Spring Data JPA + Hibernate 6 |
| Build | Maven |
| Container | Docker + Docker Compose |
| Kafka UI | Redpanda Console |

---

## 🚀 Getting Started

### Prerequisites

- Docker + Docker Compose
- Java 17
- Maven

### 1. Clone & Start Infrastructure

```bash
git clone <your-repo-url>
cd order-system

# Start Kafka, Zookeeper, Oracle XE, Redpanda Console
docker compose up -d

# Wait for Oracle to be ready (~2-3 minutes)
docker logs oracle-xe -f
# Look for: "DATABASE IS READY TO USE!"
```

### 2. Create Oracle Users

```bash
docker exec -it oracle-xe sqlplus system/Admin123@XEPDB1
```

```sql
CREATE USER inventoryuser IDENTIFIED BY inventorypass123;
GRANT CONNECT, RESOURCE, UNLIMITED TABLESPACE TO inventoryuser;

CREATE USER paymentuser IDENTIFIED BY paymentpass123;
GRANT CONNECT, RESOURCE, UNLIMITED TABLESPACE TO paymentuser;

CREATE USER shippinguser IDENTIFIED BY shippingpass123;
GRANT CONNECT, RESOURCE, UNLIMITED TABLESPACE TO shippinguser;
EXIT
```

### 3. Start All Services

Open 7 terminals, run each service:

```bash
# Terminal 1 – Service Registry
cd service-registry/service-registry && ./mvnw spring-boot:run

# Terminal 2 – API Gateway
cd api-gateway/api-gateway && ./mvnw spring-boot:run

# Terminal 3 – Order Service
cd order-service/order-service && ./mvnw spring-boot:run

# Terminal 4 – Inventory Service
cd inventory-service/inventory-service && ./mvnw spring-boot:run

# Terminal 5 – Payment Service
cd payment-service/payment-service && ./mvnw spring-boot:run

# Terminal 6 – Shipping Service
cd shipping-service/shipping-service && ./mvnw spring-boot:run

# Terminal 7 – Notification Service
cd notification-service/notification-service && ./mvnw spring-boot:run
```

### 4. Seed Inventory

```bash
curl -X POST "http://localhost:8080/inventory-service/inventory/PROD-001/stock?quantity=100"
curl -X POST "http://localhost:8080/inventory-service/inventory/PROD-002/stock?quantity=50"
```

### 5. Get JWT Token

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password123"}' \
  | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

echo "Token: $TOKEN"
```

---

## 📡 API Reference

All APIs go through the **API Gateway** at `http://localhost:8080`.

### Authentication

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/auth/login` | Get JWT token |

**Request:**
```json
{
  "username": "admin",
  "password": "password123"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "admin"
}
```

### Orders

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/order-service/orders` | ✅ JWT | Create new order |
| `GET` | `/order-service/orders/{id}` | ✅ JWT | Get order by ID |

**Create Order Request:**
```json
{
  "items": [
    { "productId": "PROD-001", "quantity": 2, "price": 150000 },
    { "productId": "PROD-002", "quantity": 1, "price": 300000 }
  ]
}
```

**Create Order Response:**
```json
{
  "id": "fc0ff503-d3ef-480c-bd50-c276f4294771",
  "status": "PENDING",
  "totalAmount": 600000,
  "createdAt": "2026-03-16T02:04:55.091866",
  "items": [
    { "productId": "PROD-001", "quantity": 2, "price": 150000 },
    { "productId": "PROD-002", "quantity": 1, "price": 300000 }
  ]
}
```

### Inventory

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/inventory-service/inventory/{productId}/stock?quantity=N` | ✅ JWT | Add stock |

### Order Status Values

| Status | Description |
|---|---|
| `PENDING` | Order created, waiting for processing |
| `PAYMENT_PROCESSING` | Inventory reserved, processing payment |
| `SHIPPING` | Payment successful, creating shipment |
| `COMPLETED` | Shipment created, order done |
| `CANCELLED` | Payment failed, inventory rolled back |

---

## 🔄 Order Flow

### Happy Path

```
Client
  │ POST /order-service/orders
  ▼
Order Service
  ├── Save order (status: PENDING)
  ├── Save outbox event (status: PENDING)
  └── [Outbox Scheduler] publish → order-created
              │
              ▼
        Inventory Service
          ├── Check idempotency
          ├── Reserve stock
          └── publish → inventory-reserved
                        │
                        ▼
                  Order Service (consumer)
                    └── Update status → PAYMENT_PROCESSING
                                │
                                ▼
                          Payment Service
                            ├── Check idempotency
                            ├── Process payment (mock)
                            └── publish → payment-success
                                          │
                                          ▼
                                    Order Service (consumer)
                                      └── Update status → SHIPPING
                                                  │
                                                  ▼
                                            Shipping Service
                                              ├── Create shipment
                                              ├── Generate tracking #
                                              └── publish → shipping-created
                                                            │
                                                            ▼
                                                      Order Service (consumer)
                                                        └── Update status → COMPLETED ✅
```

### Rollback Path (Payment Failed)

```
Payment Service
  └── publish → payment-failed
                │
                ▼
          Order Service (consumer)
            ├── Update status → CANCELLED
            └── publish → inventory-release-command
                          │
                          ▼
                    Inventory Service
                      └── Release reserved stock ✅
```

---

## 🔐 Security

JWT tokens are validated at the **API Gateway** before any request reaches downstream services.

```
Request with Authorization: Bearer <token>
              │
              ▼
        JwtAuthFilter (Order = -2, runs first)
              │
              ├── No token → 401 {"status":"error","message":"Missing or invalid token"}
              ├── Invalid token → 401
              └── Valid token → inject X-Auth-User header → forward to service
```

**Public endpoints (no JWT required):**
- `POST /auth/login`
- `GET /actuator/**`

---

## 📊 Monitoring

### Kafka UI – Redpanda Console
```
http://localhost:8090
```
View topics, messages, consumer groups, and DLT topics.

### Eureka Dashboard
```
http://localhost:8761
```
View all registered services and their health status.

### Circuit Breaker Status
```bash
curl http://localhost:8080/actuator/health | python3 -m json.tool
```

### Oracle DB
```bash
# Connect as orderuser
docker exec -it oracle-xe sqlplus orderuser/orderpass123@XEPDB1

# View orders
SELECT id, status, total_amount FROM orders;
SELECT aggregate_id, event_type, status FROM outbox_events;
```

---

## 📁 Project Structure

```
order-system/
├── docker-compose.yml          # Infrastructure (Kafka, Oracle, Redpanda)
├── redpanda-config.yaml        # Kafka UI config
│
├── service-registry/           # Eureka Server (:8761)
├── api-gateway/                # Gateway + JWT + Circuit Breaker (:8080)
├── order-service/              # Saga Orchestrator (:8081)
│   └── src/main/java/.../
│       ├── controller/         # REST API
│       ├── service/            # Business logic + Saga
│       ├── kafka/              # Event consumers
│       ├── scheduler/          # Outbox publisher
│       ├── entity/             # Order, OrderItem, OutboxEvent
│       └── repository/
│
├── inventory-service/          # Stock management (:8082)
│   └── src/main/java/.../
│       ├── entity/             # Inventory (with @Version), ProcessedEvent
│       ├── service/            # Reserve + Release logic
│       └── kafka/              # order-created consumer
│
├── payment-service/            # Payment processing (:8083)
│   └── src/main/java/.../
│       ├── entity/             # Payment, ProcessedEvent
│       └── kafka/              # inventory-reserved consumer
│
├── shipping-service/           # Shipment creation (:8084)
│   └── src/main/java/.../
│       ├── entity/             # Shipment, ProcessedEvent
│       └── kafka/              # payment-success consumer
│
└── notification-service/       # Event notifications (:8085)
    └── src/main/java/.../
        └── kafka/              # Listens to ALL events (fire-and-forget)
```

---

## 🗄 Database Schema

### Order Service (`orderuser`)

```sql
ORDERS          (id, status, total_amount, created_at, updated_at)
ORDER_ITEMS     (id, order_id, product_id, quantity, price)
OUTBOX_EVENTS   (id, aggregate_type, aggregate_id, event_type, payload, status, created_at)
```

### Inventory Service (`inventoryuser`)

```sql
INVENTORY       (product_id, quantity, version)   -- version = optimistic locking
PROCESSED_EVENTS (event_id, event_type, processed_at)
```

### Payment Service (`paymentuser`)

```sql
PAYMENTS        (id, order_id, amount, status, created_at)
PROCESSED_EVENTS (event_id, event_type, processed_at)
```

### Shipping Service (`shippinguser`)

```sql
SHIPMENTS       (id, order_id, status, tracking_number, created_at)
PROCESSED_EVENTS (event_id, event_type, processed_at)
```

---

## 🧪 Quick Test

```bash
# 1. Get token
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password123"}' \
  | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

# 2. Create order
ORDER_ID=$(curl -s -X POST http://localhost:8080/order-service/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"items":[{"productId":"PROD-001","quantity":1,"price":150000}]}' \
  | grep -o '"id":"[^"]*"' | cut -d'"' -f4)

echo "Created order: $ORDER_ID"

# 3. Wait and check status
sleep 10
curl -s http://localhost:8080/order-service/orders/$ORDER_ID \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
```

Expected final status: `COMPLETED` (70%) or `CANCELLED` (30% — payment failure simulation).

---

## 📝 Notes

- Payment Service has a **30% failure rate** configured via `payment.failure-rate: 0.3` for testing Saga rollback. Set to `0.0` for always-success.
- Outbox Scheduler runs **every 5 seconds** — allow up to 10 seconds for order status to update.
- Oracle XE takes **2-3 minutes** to fully start on first run.
- All services use **`auto-offset-reset: earliest`** — restarting a service will reprocess old messages (idempotency handles this safely).