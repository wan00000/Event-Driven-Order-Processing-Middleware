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
<img width="1432" height="86" alt="image" src="https://github.com/user-attachments/assets/912aee43-5fab-4567-b2a2-23985cfe79fc" />

Kafka UI:
- http://localhost:8085

### 2) Create Kafka topics
Create topics (example using `docker exec` into the broker container).

```powershell
docker exec -it broker sh -c "/opt/kafka/bin/kafka-topics.sh --create --topic orders.created --partitions 3 --replication-factor 1 --bootstrap-server broker:29092"
docker exec -it broker sh -c "/opt/kafka/bin/kafka-topics.sh --create --topic orders.processed --partitions 3 --replication-factor 1 --bootstrap-server broker:29092"
docker exec -it broker sh -c "/opt/kafka/bin/kafka-topics.sh --create --topic orders.dlq --partitions 1 --replication-factor 1 --bootstrap-server broker:29092"
```
<img width="1645" height="482" alt="image" src="https://github.com/user-attachments/assets/97cb0cfa-5acf-446d-bf10-92c06596b78c" />

### 3) Build the project
From repo root:
```powershell
mvn clean package
```
<img width="748" height="309" alt="image" src="https://github.com/user-attachments/assets/45f143a8-f312-44bb-9cb9-169b5e93e7b8" />

WAR outputs:
- `api-service/target/api-service.war`
- `processor-service/target/processor-service.war`
- `dlq-admin/target/dlq-admin.war`
<img width="1198" height="393" alt="image" src="https://github.com/user-attachments/assets/23584694-c9e4-402c-9223-65caa0d41c47" />

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
<img width="1919" height="970" alt="image" src="https://github.com/user-attachments/assets/1b2db5b4-4830-4fee-8df3-997bd8d2f960" />
<img width="1920" height="967" alt="image" src="https://github.com/user-attachments/assets/7e770d78-b324-46fb-a0fa-5d03f578607b" />

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
<img width="1667" height="680" alt="image" src="https://github.com/user-attachments/assets/2a90eefd-c46d-4b65-86d9-ddb89c38ef93" />


### 2) Send a failing order (goes to DLQ after retries)
```powershell
curl.exe -X POST "http://localhost:8080/api-service/api/orders" `
  -H "Content-Type: application/json" `
  -d "{\"orderId\":\"O-FAIL\",\"customer\":\"Ali\",\"amount\":9.99,\"forceFail\":true}"
```

Expected:
- After processing retries, the message lands in `orders.dlq`
<img width="1656" height="732" alt="image" src="https://github.com/user-attachments/assets/facc4d38-ec34-4e00-b8b9-f824f14e3929" />
- DLQ UI will show it
<img width="1917" height="401" alt="image" src="https://github.com/user-attachments/assets/063bb402-e12d-4252-bb46-22e4af19807c" />

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
<img width="1387" height="67" alt="image" src="https://github.com/user-attachments/assets/a7bab819-5568-41e7-b00c-490ab1f8e85d" />

Consume DLQ messages:
```powershell
docker exec -it broker sh -c "/opt/kafka/bin/kafka-console-consumer.sh --topic orders.dlq --from-beginning --bootstrap-server broker:29092"
```
<img width="1393" height="86" alt="image" src="https://github.com/user-attachments/assets/d38b44ff-7e6e-4495-8ee6-f5e47b8b395b" />

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



