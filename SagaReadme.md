# 📘 README — Distributed Transactions & SAGA Pattern


## 📌 Overview

This project demonstrates **distributed transactions in microservices** using the **SAGA pattern**.

We intentionally **avoid global transactions (`@Transactional`) across services** and instead use **event-driven and orchestration-based SAGA patterns**, which are the **industry-standard solution** for microservices consistency.

### ✅ What this project teaches

* Why distributed transactions are hard in microservices
* Why **2PC / XA transactions are not recommended**
* What the **SAGA pattern** is
* Difference between:

    * **Orchestration Saga**
    * **Choreography Saga**
* How **both Saga types** are implemented in this project
* How **compensation, retries, and persistence** work
* How **RabbitMQ enables event-driven Saga**

---

## 🧠 Distributed Transactions Problem

### ❌ Monolithic Transactions (What DOES NOT work in microservices)

```java
@Transactional
public void placeOrder() {
    saveOrder();      // Order DB
    chargePayment();  // Payment DB
}
```

This works **only in monoliths**, because:

* One database
* One transaction manager

---

### ❌ Why This Fails in Microservices

In microservices:

* Each service owns its **own database**
* Transactions **cannot span multiple services**
* Failures leave systems in **inconsistent states**

Example failure:

```
Order saved ✔
Payment failed ❌
→ Data inconsistency
```

---

## ✅ Solution: SAGA Pattern

A **Saga** is a sequence of **local transactions** where:

1. Each service commits its own database changes
2. Services communicate via **events or commands**
3. Failures are handled using **compensating transactions**

---

## 🔁 SAGA Types

### 1️⃣ Orchestration Saga (Command-Based)

* A **central orchestrator** controls the entire workflow
* Easier to understand
* Clear business flow

### 2️⃣ Choreography Saga (Event-Based)

* No central controller
* Services react to events
* Loosely coupled
* More scalable, but harder to debug

---

## 🏗 Architecture Used in This Project

```
Client
  |
  v
API Gateway
  |
  v
Order Service (Saga Orchestrator)
  |
  +--> User Service
  |
  +--> Payment Service
```

Communication:

* REST (Feign) → Orchestration Saga
* RabbitMQ → Event-driven Saga

---

# 🟢 PART 1 — ORCHESTRATION SAGA (SYNCHRONOUS)

## 🎯 Business Rule

> An order can only be created if the user exists.

---

## 🔄 Flow (Orchestration Saga)

```
1. Order created (CREATED)
2. Order Service calls User Service (Feign)
3. If user exists → CONFIRMED
4. If user invalid → CANCELLED (compensation)
```

---

## 🧩 Code Walkthrough

### Step 1 — Create Order (Local Transaction)

```java
order.setStatus(OrderStatus.CREATED);
orderRepository.save(order);
```

---

### Step 2 — Validate User (Remote Call)

```java
@CircuitBreaker(name="userService", fallbackMethod="userFallback")
public void validateUser(Long userId) {
    userClient.getUser(userId);
}
```

* Uses **Resilience4j Circuit Breaker**
* Prevents cascading failures

---

### Step 3 — Confirm Order

```java
order.setStatus(OrderStatus.CONFIRMED);
orderRepository.save(order);
```

---

### Step 4 — Compensation (Failure Case)

```java
order.setStatus(OrderStatus.CANCELLED);
orderRepository.save(order);
```

✔ This is a **classic Orchestration Saga**
✔ Controlled entirely by `OrderService`

---

# 🔵 PART 2 — EVENT-DRIVEN SAGA (CHOREOGRAPHY)

This is the **production-grade Saga implementation** using **RabbitMQ**.

---

## 🎯 Business Rule (Extended)

> An order is CONFIRMED only if:
>
> 1. User exists
> 2. Payment succeeds

---

## 🔄 Event-Driven Saga Flow

```
OrderCreatedEvent
        ↓
UserValidatedEvent
        ↓
PaymentProcessedEvent
        ↓
OrderConfirmed OR Compensated
```

---

## 🧠 Saga State Machine

```text
STARTED
   ↓
USER_VALIDATED
   ↓
COMPLETED
```

Failure path:

```text
STARTED → COMPENSATING → FAILED
```

---

## 🧩 SAGA PERSISTENCE

Each order stores:

* `OrderStatus`
* `SagaStatus`

```java
public enum SagaStatus {
    STARTED,
    USER_VALIDATED,
    COMPLETED,
    COMPENSATING,
    FAILED
}
```

✔ Saga state survives restarts
✔ Enables retries and recovery

---

## 🟢 Step-by-Step Event Flow (Mapped to Code)

---

### 🔹 Step 1 — Start Saga (Order Service)

```java
order.setSagaStatus(SagaStatus.STARTED);
rabbit.convertAndSend("saga-exchange", "order.created", event);
```

📍 `OrderSagaService.startSaga()`

---

### 🔹 Step 2 — User Validation (User Service)

```java
@RabbitListener(queues = "user-queue")
public void validateUser(OrderCreatedEvent event)
```

✔ Emits `UserValidatedEvent`

---

### 🔹 Step 3 — Order Service Receives Validation

```java
@RabbitListener(queues = "order-queue")
public void handleUserValidated(UserValidatedEvent event)
```

* If invalid → compensate
* If valid → request payment

---

### 🔹 Step 4 — Payment Processing

```java
@RabbitListener(queues = "payment-queue")
public void processPayment(PaymentProcessedEvent event)
```

✔ Emits `PaymentProcessedEvent`

---

### 🔹 Step 5 — Final Decision

```java
order.setSagaStatus(SagaStatus.COMPLETED);
order.setStatus(OrderStatus.CONFIRMED);
```

---

## 🔴 Compensation Logic (Critical)

```java
order.setStatus(OrderStatus.CANCELLED);
order.setSagaStatus(SagaStatus.FAILED);
rabbit.convertAndSend("payment.refund", new RefundEvent(orderId));
```

✔ Payment refunded
✔ Order cancelled
✔ System consistency restored

---

# 🔁 Saga Retry Strategy

* Failed sagas are persisted
* Retry can be triggered by:

    * Scheduler
    * Admin endpoint
    * Dead-letter queue

This prevents **data loss on crashes**.

---

## 🛡 Best Practices Followed

✔ No distributed `@Transactional`

✔ Idempotent event handling

✔ Explicit Saga states

✔ Compensation always available

✔ Circuit Breaker protection

✔ Event-driven loose coupling

---

## 🆚 Orchestration vs Choreography (Comparison)

| Feature         | Orchestration | Choreography |
| --------------- | ------------- | ------------ |
| Control         | Centralized   | Distributed  |
| Complexity      | Low           | High         |
| Scalability     | Medium        | High         |
| Debugging       | Easy          | Hard         |
| Used in Project | ✅ Yes         | ✅ Yes        |

---
