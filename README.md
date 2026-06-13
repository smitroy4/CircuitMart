# CircuitMart

CircuitMart is a cloud-native e-commerce backend built using Spring Boot and a Microservices Architecture. The project demonstrates service discovery, centralized configuration management, API gateway routing, and inter-service communication for scalable enterprise applications.

## Architecture

CircuitMart follows a distributed microservices architecture consisting of:

- API Gateway
- Discovery Service (Eureka Server)
- Config Server
- Inventory Service
- Order Service

```text
                    +----------------+
                    |   API Gateway  |
                    +-------+--------+
                            |
          -------------------------------------
          |                                   |
          ▼                                   ▼
+-------------------+             +-------------------+
| Inventory Service |             |   Order Service   |
+---------+---------+             +---------+---------+
          |                                   |
          -------------------------------------
                            |
                            ▼
                  +------------------+
                  | Discovery Server |
                  |   (Eureka)       |
                  +------------------+

                            ▲
                            |
                  +------------------+
                  |  Config Server   |
                  +------------------+
```

---

## Features

- Microservices-based architecture
- Spring Cloud Gateway
- Eureka Service Discovery
- Centralized Configuration Server
- Inter-service Communication using OpenFeign
- PostgreSQL Integration
- Externalized Configuration Management
- Scalable and Maintainable Design
- Production-Oriented Project Structure

---

## Tech Stack

### Backend

- Java 21
- Spring Boot 3
- Spring Cloud
- Spring Data JPA
- OpenFeign
- Maven

### Cloud & Infrastructure

- Eureka Discovery Server
- Spring Cloud Config Server
- Spring Cloud Gateway

### Database

- PostgreSQL

---

## Project Structure

```text
CircuitMart
│
├── api-gateway
├── discovery-service
├── config-server
├── inventory-service
└── order-service
```

---

## Services Overview

### API Gateway

Acts as the single entry point for all client requests.

Responsibilities:
- Request Routing
- Load Balancing
- Service Discovery Integration
- Centralized API Access

---

### Discovery Service

Netflix Eureka Server responsible for:

- Service Registration
- Service Discovery
- Dynamic Service Lookup

---

### Config Server

Provides centralized configuration management.

Responsibilities:
- External Configuration Storage
- Environment-Specific Properties
- Centralized Configuration Updates

---

### Inventory Service

Manages product inventory.

Responsibilities:
- Inventory Tracking
- Stock Validation
- Product Availability Checks

---

### Order Service

Handles order management.

Responsibilities:
- Order Creation
- Inventory Verification
- Business Logic Execution

---

## Getting Started

### Prerequisites

- Java 21+
- Maven 3.9+
- PostgreSQL
- Git

---

## Clone Repository

```bash
git clone https://github.com/yourusername/CircuitMart.git

cd CircuitMart
```

---

## Start Services

Start services in the following order:

### 1. Discovery Service

```bash
cd discovery-service
mvn spring-boot:run
```

Runs on:

```text
http://localhost:8761
```

---

### 2. Config Server

```bash
cd config-server
mvn spring-boot:run
```

Runs on:

```text
http://localhost:8888
```

---

### 3. Inventory Service

```bash
cd inventory-service
mvn spring-boot:run
```

---

### 4. Order Service

```bash
cd order-service
mvn spring-boot:run
```

---

### 5. API Gateway

```bash
cd api-gateway
mvn spring-boot:run
```

---

## Learning Objectives

This project was built to gain hands-on experience with:

- Microservices Architecture
- Spring Cloud Ecosystem
- API Gateway Patterns
- Service Discovery
- Distributed Configuration Management
- Enterprise Application Development
- Cloud-Native Backend Development

---

## Future Improvements

- Product Service
- User Service
- Authentication & Authorization (JWT)
- Docker Containerization
- Kubernetes Deployment
- Distributed Tracing
- Resilience4j Circuit Breakers
- Kafka Event-Driven Communication
- CI/CD Pipeline

---

## Author

**Smit**

Aspiring Java Full Stack Developer focused on building scalable backend systems using Java, Spring Boot, Microservices, and Cloud Technologies.

---

## License

This project is intended for educational and portfolio purposes.
