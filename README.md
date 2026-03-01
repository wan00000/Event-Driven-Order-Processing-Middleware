# Event-Driven Order Processing Middleware (Kafka + WildFly + MicroProfile)

A reference implementation of an **event-driven pipeline** for order processing. It shows how to decouple services using Kafka while maintaining **operational resilience** through **retries, Dead Letter Queue (DLQ) handling, and clear runbook-driven troubleshooting**.

Tech stack:

- **Java 17**
- **WildFly** (Jakarta EE + MicroProfile)
- **MicroProfile Reactive Messaging (SmallRye Kafka)**
- **Apache Kafka** (via Docker Compose)
- **Kafka UI**: For topic visibility

---

## Problem & Goal

**Problem:** In many enterprise environments, synchronous point-to-point integrations become fragile as load grows: a temporary downstream outage can cascade into user-facing failures, and troubleshooting becomes difficult without consistent correlation and replay mechanisms.

**Goal:** Provide a local, reproducible setup that demonstrates:

- **Asynchronous decoupling** via Kafka topics
- **Deterministic handling of failures** using retries and DLQ
- **Operational visibility** (what failed, why, and how to recover)
- **A clear path to enterprise patterns** (retry topics + backoff + DLQ reprocessing UI)

## Architecture

```
Client (curl/Postman)
   |
   v
[ api-service (WildFly) ]  --publish-->  Kafka topic: orders.created
                                         |
                                         v
[ processor-service (WildFly) ] --success--> orders.processed
           |
           +--fail after retry--> orders.dlq
                                         |
                                         v
[ dlq-admin (WildFly) ]  (view DLQ in UI / via REST)
```

---

## Modules

- **common/**  
  Shared models + JSON utilities

- **api-service/** (WAR)  
  REST endpoint `POST /api/orders` → publishes `OrderCreated` event to Kafka

- **processor-service/** (WAR)  
  Consumes `orders.created` → processes → publishes `orders.processed`  
  On failure: retries then routes to DLQ

- **dlq-admin/** (WAR)  
  Consumes `orders.dlq` → stores latest N DLQ items in memory → exposes REST + simple UI

- **infra/**  
  Docker Compose for Kafka + Kafka UI, plus topic creation helpers

---

## Prerequisites

- **JDK 17**
- **Maven 3.8+**
- **Docker Desktop** (with `docker compose`)
- **WildFly** (tested with 39.x)

Quick checks (Windows PowerShell):
```powershell
java -version
mvn -v
docker version
docker compose version
```

---

## Quick Start

### 1) Start Kafka + Kafka UI
From the repo root:
```powershell
cd infra
docker compose up -d
```

Kafka UI:
- http://localhost:8085

### 2) Create Kafka topics
Create topics (example using `docker exec` into the broker container).

```powershell
docker exec -it broker sh -c "/opt/kafka/bin/kafka-topics.sh --create --topic orders.created --partitions 3 --replication-factor 1 --bootstrap-server broker:29092"
docker exec -it broker sh -c "/opt/kafka/bin/kafka-topics.sh --create --topic orders.processed --partitions 3 --replication-factor 1 --bootstrap-server broker:29092"
docker exec -it broker sh -c "/opt/kafka/bin/kafka-topics.sh --create --topic orders.dlq --partitions 1 --replication-factor 1 --bootstrap-server broker:29092"
```

### 3) Build the project
From repo root:
```powershell
mvn clean package
```

WAR outputs:
- `api-service/target/api-service.war`
- `processor-service/target/processor-service.war`
- `dlq-admin/target/dlq-admin.war`

### 4) Start WildFly
```powershell
cd C:\path\to\wildfly-39.x\bin
.\standalone.bat -c standalone-microprofile.xml 
```

### 5) Deploy the WARs
Copy the WARs into:
```
C:\path\to\wildfly-39.x\standalone\deployments\
```

After deployment, you should see “Deployed …” messages in the WildFly console.

---

## Usage / Testing

### 1) Send a successful order
```powershell
curl.exe -X POST "http://localhost:8080/api-service/api/orders" `
  -H "Content-Type: application/json" `
  -d "{\"orderId\":\"O-1001\",\"customer\":\"Ali\",\"amount\":123.45,\"forceFail\":false}"
```

Expected:
- A message appears in `orders.processed`

### 2) Send a failing order (goes to DLQ after retries)
```powershell
curl.exe -X POST "http://localhost:8080/api-service/api/orders" `
  -H "Content-Type: application/json" `
  -d "{\"orderId\":\"O-FAIL\",\"customer\":\"Ali\",\"amount\":9.99,\"forceFail\":true}"
```

Expected:
- After processing retries, the message lands in `orders.dlq`
- DLQ UI will show it

### 3) View DLQ UI
Open:
- http://localhost:8080/dlq-admin/

### 4) View DLQ via REST
```powershell
curl.exe "http://localhost:8080/dlq-admin/api/dlq"
```

---

## Inspect Kafka topics (CLI)

Consume processed messages:
```powershell
docker exec -it broker sh -c "/opt/kafka/bin/kafka-console-consumer.sh --topic orders.processed --from-beginning --bootstrap-server broker:29092"
```

Consume DLQ messages:
```powershell
docker exec -it broker sh -c "/opt/kafka/bin/kafka-console-consumer.sh --topic orders.dlq --from-beginning --bootstrap-server broker:29092"
```

---

## Project Structure

```
event-driven-integration-hub/
├─ common/
├─ api-service/
├─ processor-service/
├─ dlq-admin/
└─ infra/
```

---

## Roadmap Ideas

- Prometheus + Grafana dashboards (consumer lag, DLQ rate, throughput)
- OpenTelemetry tracing (API → Kafka → Processor)
- Auth for admin endpoints (Keycloak / JWT)
- Persist DLQ store (instead of in-memory)

---



