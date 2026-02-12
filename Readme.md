# 🧩 **Spring Boot Microservices**

## 📌 **Overview**

This project is a **complete end-to-end microservices system** built using **Spring Boot & Spring Cloud**.
It demonstrates modern distributed system patterns, including:

* Service Discovery (Eureka)
* API Gateway (Spring Cloud Gateway)
* Centralized Config (Spring Cloud Config)
* Inter-service communication using **Feign**
* Resilience (Circuit Breaker, Retry)
* Distributed Tracing with **Spring Cloud Sleuth & Zipkin**
* Lightweight database using **H2**

This setup is designed for **learning**, and can also be evolved into a production-grade microservices architecture.

---


# 🏗 **Architecture**

```
                  +------------------------+
                  |   Config Server        |
                  |  (Centralized YAML)    |
                  +-----------+------------+
                              |
                    loads configs for all
                              |
   +--------------------------v-----------------------------+
   |                     Eureka Server                      |
   |             (Service Discovery + Registry)             |
   +----------+---------------------+------------------------+
              |                     |               
              |                     |               
   +----------v----+      +---------v---------+
   |  user-service |      |  order-service    |       
   | (H2 DB, Feign)| <--->| (H2 DB, Feign)    | 
   +----------+----+      +---------+---------+       
              ^                     ^
              |                     |
              +----------+----------+
                         |
               +---------v---------+
               |   API Gateway     |
               | (Routing + JWT)   |
               +---------+---------+
                         |
                     Client / UI
```

---

# 🧰 **Microservices Included**

| Service           | Port        | Description                                     |
| ----------------- | ----------- | ----------------------------------------------- |
| **Eureka Server** | `8761`      | Service registry / discovery center             |
| **API Gateway**   | `8080`      | Single entry point for all clients              |
| **User Service**  | Random port | Manages users + H2 DB                           |
| **Order Service** | Random port | Manages orders + calls User Service using Feign |
| **Zipkin**        | `9411`      | Distributed tracing UI                          |

> All internal communication uses **service names**, not host/port.

---

# 🔧 **Technologies Used**

### Backend

* Java 17+
* Spring Boot
* Spring Web
* Spring Data JPA + H2 Database
* Spring Cloud Netflix Eureka
* Spring Cloud Gateway
* Spring Cloud Config
* Spring Cloud OpenFeign
* Spring Cloud Sleuth
* Zipkin
* Resilience4j CircuitBreaker + Retry

### Tooling

* Maven
* Docker (optional for Zipkin)

---

# 📂 **Project Structure**

```
spring-microservices/
│
├── config-server/
├── eureka-server/
├── gateway/
├── user-service/
├── order-service/
│
├── config-repo/
│   ├── application.yml
│   ├── user-service.yml
│   ├── order-service.yml
│   ├── gateway.yml
│
└── README.md

```


Each service contains:

```
src/main/java/com/example/...
src/main/resources/application.yml
pom.xml
```

---

# 🚀 **HOW TO RUN THE PROJECT**

## **1️⃣ Start Eureka Server**

```
cd eureka-server
mvn spring-boot:run
```

Access UI:

👉 [http://localhost:8761](http://localhost:8761)

---

## **2️⃣ Start Config Server**

```
cd config-server
mvn spring-boot:run
```

Access:

👉 [http://localhost:8888](http://localhost:8888)


---

## **3️⃣ Start API Gateway**

```
cd gateway
mvn spring-boot:run
```

Access:

👉 [http://localhost:8080](http://localhost:8080)


---

## **4️⃣ Start User Service**

```
cd user-service
mvn spring-boot:run
```

H2 Console:

👉 [http://localhost](http://localhost):<randomPort>/h2-console
JDBC URL: `jdbc:h2:mem:userdb`

---

## **5️⃣ Start Order Service**

```
cd order-service
mvn spring-boot:run
```

H2 Console:

👉 [http://localhost](http://localhost):<randomPort>/h2-console
JDBC URL: `jdbc:h2:mem:orderdb`

---

# 🧪 **API Documentation**

## 🔹 **User Service**

### ➤ Create User

**POST /api/users**

```json
{
  "name": "Alice",
  "email": "alice@example.com"
}
```

### ➤ Get User

**GET /api/users/{id}**

---

## 🔹 **Order Service**

### ➤ Create Order

Order Service calls User Service via **Feign Client**.

**POST /api/orders**

```json
{
  "userId": 1,
  "product": "Laptop",
  "price": 1499.99
}
```

### ➤ Get Order

**GET /api/orders/{id}**

---

## 🔹 **Gateway Routes**

| API        | Gateway URL      | Internal Route       |
| ---------- | ---------------- | -------------------- |
| User APIs  | `/api/users/**`  | `lb://USER-SERVICE`  |
| Order APIs | `/api/orders/**` | `lb://ORDER-SERVICE` |

---

# 🛡️ **Microservice Concepts Explained**

## ✔ 1. **Centralized Config**

Central configuration server that loads YAML files from a Git repository or local filesystem.

Benefits:

* Keep all service configuration centralized
* Dynamically refresh config
  Example:
```
config-repo/
   user-service.yml
   order-service.yml
   gateway.yml
```

---

## ✔ 2. **Service Discovery (Eureka)**

Services register automatically with Eureka and discover others dynamically.

Benefits:

* No hardcoded URLs
* Load balancing
* Dynamic scaling

---

## ✔ 3. **API Gateway**

Gateway acts as:

* Single entry point
* Router
* Request validator, authenticator
* Circuit breaker handler (optional)

Features used:

* Discovery-based routing
* Path-based routing

---

## ✔ 4. **OpenFeign (Communication)**

Declarative REST Client:

```java
@FeignClient(name="user-service")
public interface UserClient {
    @GetMapping("/api/users/{id}")
    UserDTO getUser(@PathVariable Long id);
}
```

---

## ✔ 5. **Resilience4j**

Implemented:

✓ Circuit Breaker
✓ Retry
✓ Fallback methods

Example:

```java
@CircuitBreaker(name="userService", fallbackMethod="fallback")
public Order createOrder(OrderRequest req) {
    return orderRepository.save(...);
}
```

---

## ✔ 6. **Distributed Tracing (Zipkin + Sleuth)**

Automatically adds:

* traceId
* spanId

View traces:

👉 [http://localhost:9411](http://localhost:9411)

---

## ✔ 7. **H2 In-Memory Databases**

* Fast, simple
* No installation required
* Useful for dev learning

User Service database: `userdb`
Order Service database: `orderdb`

---


# 🔍 **Testing the System**

## 1️⃣ Create User

```
POST http://localhost:8080/api/users
```

---

## 2️⃣ Create Order (userId must exist)

```
POST http://localhost:8080/api/orders
```

This will:

* Call Order Service
* Order Service calls User Service (Feign)
* Saves order in H2
* Returns order response

---




