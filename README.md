# StaySphere - Airbnb-Style Microservices Backend

StaySphere is a backend-only vacation rental platform built with Spring Boot microservices. It covers user management, JWT-based authentication, property publishing, property search with Redis caching, booking and payment workflows, reviews, notifications, Kafka event flow, API Gateway routing, and service-level observability.

The repository is organized as a set of independently deployable services, each with its own database and API boundary.

---

## Tech Stack

- Java 17
- Spring Boot
- Spring Web
- Spring Security
- Spring Data JPA
- Spring Cloud Gateway MVC
- Spring Kafka
- Spring Data Redis
- PostgreSQL
- Redis
- Prometheus
- Grafana
- Maven
- Docker and Docker Compose

---

## System Overview

```text
Client / Postman
      |
      v
API Gateway :8088
      |
      +-------------------- user-service :8081 -------- user_db
      |                             |
      |                             +---- /api/auth/login
      |
      +---------------- property-service :8082 -------- property_db
      |                         |
      |                         +---- validates host via user-service
      |                         +---- syncs searchable data to search-service
      |                         +---- blocks deletion when active bookings exist
      |
      +---------------- search-service :8083 ---------- search_db + Redis
      |                         |
      |                         +---- read model for property search
      |
      +---------------- booking-service :8084 --------- booking_db
      |                         |
      |                         +---- validates users and properties
      |                         +---- publishes booking-events to Kafka
      |
      +---------------- payment-service :8085 --------- payment_db
      |                         |
      |                         +---- validates bookings
      |                         +---- publishes payment-events to Kafka
      |
      +---------------- review-service :8086 ---------- review_db
      |                         |
      |                         +---- validates bookings and payments
      |                         +---- publishes review-events to Kafka
      |
      +---------------- notification-service :8087 ---- notification_db
                                |
                                +---- consumes Kafka events and stores notifications

Supporting infrastructure:
- PostgreSQL :5434
- Redis :6379
- Kafka :9092
- Prometheus :9090
- Grafana :3000
```

---

## Service Ports

| Service | Port |
| --- | ---: |
| API Gateway | 8088 |
| User Service | 8081 |
| Property Service | 8082 |
| Search Service | 8083 |
| Booking Service | 8084 |
| Payment Service | 8085 |
| Review Service | 8086 |
| Notification Service | 8087 |
| PostgreSQL | 5434 |
| Redis | 6379 |
| Kafka | 9092 |
| Prometheus | 9090 |
| Grafana | 3000 |

---

## Architecture Principles

Each service owns its data and business logic.

```text
user-service          -> user_db
property-service      -> property_db
search-service        -> search_db
booking-service       -> booking_db
payment-service       -> payment_db
review-service        -> review_db
notification-service  -> notification_db
```

Shared behavior across services:

- Stateless JWT authentication
- Service-specific Spring Security rules
- REST-based service-to-service validation
- Kafka events for downstream notifications
- Actuator endpoints for health and metrics

---

## Repository Structure

Most services follow the same layered layout:

```text
controller
service
repository
entity
dto
enums
exception
config
security
event
client
```

Responsibilities:

| Layer | Responsibility |
| --- | --- |
| Controller | Exposes REST APIs |
| Service | Business logic and cross-service orchestration |
| Repository | Database access |
| Entity | Persistent model |
| DTO | Request and response payloads |
| Enums | Status and type values |
| Exception | Global error handling |
| Config | Security, Kafka, Redis, Jackson, REST client setup |
| Security | JWT validation and request filtering |
| Event | Kafka payloads and producers/consumers |
| Client | Outbound service integrations |

---

## API Gateway Routes

All public APIs are exposed through the gateway on port `8088`.

```properties
/api/users/**           -> user-service
/api/auth/**            -> user-service
/api/properties/**      -> property-service
/api/search/**          -> search-service
/api/bookings/**        -> booking-service
/api/payments/**        -> payment-service
/api/reviews/**         -> review-service
/api/notifications/**   -> notification-service
```

Gateway health/demo endpoint:

```http
GET /hello
```

---

## Authentication and Roles

Authentication is handled by the user service.

Login endpoint:

```http
POST /api/auth/login
```

JWT claims include:

- user id
- email
- role

Roles used across the platform:

- `GUEST`
- `HOST`
- `ADMIN`

Current access rules:

- User registration and login are public
- Property creation requires `HOST`
- Booking creation requires `GUEST`
- Payment creation requires `GUEST`
- Review creation requires `GUEST`
- Notification deletion requires `ADMIN`
- Most read endpoints are public, while write operations are role-protected

---

## Data and Infrastructure

### PostgreSQL

The repo uses one database per service. Local startup is handled by Docker Compose and the initialization script in `postgres-init/01-create-databases.sql`.

### Redis

Search results are cached in Redis with a 5 minute TTL.

### Kafka

Kafka is used for asynchronous notification workflows.

Topics used by the codebase:

- `booking-events`
- `payment-events`
- `review-events`

### Observability

Every service exposes Actuator endpoints:

- `/actuator/health`
- `/actuator/info`
- `/actuator/prometheus`
- `/actuator/metrics`

Prometheus scrapes all services, and Grafana can be pointed at Prometheus for dashboards.

---

## 1. User Service

Manages platform users and issues JWTs.

### Entity

```text
User
id
name
email
password
role
createdAt
updatedAt
```

### Roles

```text
GUEST
HOST
ADMIN
```

### Endpoints

| Method | Endpoint | Description |
| --- | --- | --- |
| POST | `/api/users` | Create user |
| GET | `/api/users` | Get all users |
| GET | `/api/users/{id}` | Get user by id |
| DELETE | `/api/users/{id}` | Delete user |
| POST | `/api/auth/login` | Authenticate and return JWT |

### Notes

- Passwords are encoded with BCrypt
- `POST /api/users` and `POST /api/auth/login` are public
- The service exposes Actuator metrics for monitoring

### Example Login

```http
POST http://localhost:8088/api/auth/login
```

```json
{
  "email": "emma.carter@example.com",
  "password": "password123"
}
```

---

## 2. Property Service

Manages rental properties and keeps the search index in sync.

### Entity

```text
Property
id
hostId
title
description
city
country
address
pricePerNight
currency
maxGuests
propertyType
status
amenities
createdAt
updatedAt
```

### Property Types

```text
APARTMENT
HOUSE
VILLA
CABIN
COTTAGE
CONDO
TOWNHOUSE
FARMHOUSE
BUNGALOW
GUEST_HOUSE
TREEHOUSE
BOAT
CAMPING_SITE
```

### Property Status

```text
PENDING
ACTIVE
INACTIVE
SUSPENDED
```

### Endpoints

| Method | Endpoint | Description |
| --- | --- | --- |
| GET | `/api/properties/hello` | Health/demo endpoint |
| POST | `/api/properties` | Create property |
| GET | `/api/properties/{id}` | Get property by id |
| GET | `/api/properties` | Get all properties |
| GET | `/api/properties/host/{hostId}` | Get properties by host |
| GET | `/api/properties/city/{city}` | Get properties by city |
| PUT | `/api/properties/{id}` | Update property |
| DELETE | `/api/properties/{id}` | Delete property |

### Business Rules

- `hostId` is validated against user-service
- create and update operations sync searchable data to search-service
- property deletion is blocked when active bookings exist
- active bookings are treated as `PENDING` or `CONFIRMED`
- delete and update operations are role-protected

### Example Create Property

```http
POST http://localhost:8088/api/properties
```

```json
{
  "hostId": 5,
  "title": "Modern Lakefront Cabin",
  "description": "A peaceful lakefront cabin with a private deck, fireplace, fast Wi-Fi, and mountain views.",
  "city": "Interlaken",
  "country": "Switzerland",
  "address": "24 Lakeview Road, Interlaken",
  "pricePerNight": 180,
  "currency": "CHF",
  "maxGuests": 4,
  "propertyType": "CABIN",
  "status": "ACTIVE",
  "amenities": "Wi-Fi, Fireplace, Lake View, Kitchen, Free Parking"
}
```

---

## 3. Search Service

Provides a read-optimized property search API backed by PostgreSQL and Redis.

### Entity

```text
SearchProperty
id
propertyId
title
city
country
pricePerNight
currency
maxGuests
propertyType
status
amenities
createdAt
updatedAt
```

### Search Filters

```text
city
country
propertyType
maxGuests
minPrice
maxPrice
```

### Redis Cache

- Search results are cached in Redis
- Cache TTL is 5 minutes
- Cache is cleared when properties are synced

### Endpoints

| Method | Endpoint | Description |
| --- | --- | --- |
| POST | `/api/search/properties/sync` | Sync property into search database |
| GET | `/api/search/properties` | Search properties |

### Example Searches

```http
GET http://localhost:8088/api/search/properties?city=Interlaken
```

```http
GET http://localhost:8088/api/search/properties?country=Switzerland
```

```http
GET http://localhost:8088/api/search/properties?propertyType=CABIN&maxGuests=4
```

---

## 4. Booking Service

Handles reservations and prevents double booking.

### Entity

```text
Booking
id
propertyId
guestId
checkInDate
checkOutDate
totalAmount
currency
status
createdAt
updatedAt
version
```

### Booking Status

```text
PENDING
CONFIRMED
CANCELLED
EXPIRED
```

### Endpoints

| Method | Endpoint | Description |
| --- | --- | --- |
| POST | `/api/bookings` | Create booking |
| GET | `/api/bookings/{id}` | Get booking by id |
| GET | `/api/bookings/guest/{guestId}` | Get bookings by guest |
| GET | `/api/bookings/property/{propertyId}` | Get bookings by property |
| PUT | `/api/bookings/{id}` | Update booking |
| DELETE | `/api/bookings/{id}` | Cancel booking |

### Business Rules

- guest id is validated against user-service
- property id is validated against property-service
- property must be `ACTIVE`
- overlapping bookings are rejected
- booking overlap logic uses:

```text
existingCheckIn < requestedCheckOut
AND
existingCheckOut > requestedCheckIn
```

- PostgreSQL advisory locking is used with `pg_advisory_xact_lock(propertyId)`
- optimistic locking is enabled with `@Version`
- booking creation publishes a Kafka booking event

### Example Create Booking

```http
POST http://localhost:8088/api/bookings
```

```json
{
  "propertyId": 9,
  "guestId": 4,
  "checkInDate": "2026-12-10",
  "checkOutDate": "2026-12-15",
  "totalAmount": 900,
  "currency": "CHF"
}
```

---

## 5. Payment Service

Handles mock and external-style payment flows.

### Entity

```text
Payment
id
bookingId
userId
amount
currency
paymentStatus
paymentProvider
providerTransactionId
createdAt
updatedAt
```

### Payment Status

```text
PENDING
SUCCESS
FAILED
REFUNDED
```

### Payment Provider

```text
MOCK
STRIPE
RAZORPAY
```

### Endpoints

| Method | Endpoint | Description |
| --- | --- | --- |
| POST | `/api/payments` | Create payment |
| GET | `/api/payments/{id}` | Get payment by id |
| GET | `/api/payments/booking/{bookingId}` | Get payment by booking |
| GET | `/api/payments/user/{userId}` | Get payments by user |
| PATCH | `/api/payments/{id}/refund` | Refund payment |

### Business Rules

- booking is validated against booking-service
- booking must belong to the same user
- booking must be `CONFIRMED`
- amount must match booking total
- currency must match booking currency
- only one payment is allowed per booking
- successful payments publish a Kafka payment event

### Example Create Payment

```http
POST http://localhost:8088/api/payments
```

```json
{
  "bookingId": 6,
  "userId": 4,
  "amount": 900,
  "currency": "CHF",
  "paymentProvider": "MOCK"
}
```

---

## 6. Review Service

Handles property reviews after a valid booking and successful payment.

### Entity

```text
Review
id
propertyId
userId
bookingId
rating
comment
createdAt
updatedAt
```

### Endpoints

| Method | Endpoint | Description |
| --- | --- | --- |
| POST | `/api/reviews` | Create review |
| GET | `/api/reviews/{id}` | Get review by id |
| GET | `/api/reviews/property/{propertyId}` | Get reviews by property |
| GET | `/api/reviews/user/{userId}` | Get reviews by user |
| DELETE | `/api/reviews/{id}` | Delete review |

### Business Rules

- booking is validated against booking-service
- booking must belong to the same user
- booking must belong to the same property
- booking must be `CONFIRMED`
- payment is validated against payment-service
- payment must exist for the booking
- payment must be `SUCCESS`
- only one review is allowed per booking
- successful reviews publish a Kafka review event

### Example Create Review

```http
POST http://localhost:8088/api/reviews
```

```json
{
  "propertyId": 9,
  "userId": 4,
  "bookingId": 6,
  "rating": 5,
  "comment": "Beautiful lakefront cabin with a peaceful view and smooth booking experience."
}
```

---

## 7. Notification Service

Stores and manages user notifications.

### Entity

```text
Notification
id
userId
type
message
status
createdAt
updatedAt
```

### Notification Type

```text
BOOKING_CREATED
BOOKING_CANCELLED
PAYMENT_SUCCESS
PAYMENT_FAILED
REVIEW_CREATED
GENERAL
```

### Notification Status

```text
PENDING
SENT
FAILED
READ
```

### Endpoints

| Method | Endpoint | Description |
| --- | --- | --- |
| POST | `/api/notifications` | Create notification |
| GET | `/api/notifications/{id}` | Get notification by id |
| GET | `/api/notifications/user/{userId}` | Get notifications by user |
| GET | `/api/notifications/user/{userId}/unread` | Get unread notifications |
| PATCH | `/api/notifications/{id}/read` | Mark notification as read |
| DELETE | `/api/notifications/{id}` | Delete notification |

### Event Consumption

Notification service consumes:

- `BookingCreatedEvent`
- `PaymentSuccessEvent`
- `ReviewCreatedEvent`

Those events are turned into stored notifications with matching types.

### Example Get Notifications

```http
GET http://localhost:8088/api/notifications/user/4
```

---

## End-to-End Workflow

Typical platform flow:

```text
1. Create host user
2. Create guest user
3. Host creates property
4. Property syncs to search-service
5. Guest searches properties
6. Guest creates booking
7. Booking validates user and property
8. Booking publishes booking event
9. Notification service stores booking notification
10. Guest creates payment
11. Payment validates booking
12. Payment publishes payment event
13. Notification service stores payment notification
14. Guest creates review
15. Review validates booking and payment
16. Review publishes review event
17. Notification service stores review notification
```

---

## Example Complete Flow

### 1. Create Host

```http
POST /api/users
```

```json
{
  "name": "Liam Anderson",
  "email": "liam.anderson@example.com",
  "password": "password123",
  "role": "HOST"
}
```

### 2. Create Guest

```http
POST /api/users
```

```json
{
  "name": "Emma Carter",
  "email": "emma.carter@example.com",
  "password": "password123",
  "role": "GUEST"
}
```

### 3. Login Guest

```http
POST /api/auth/login
```

```json
{
  "email": "emma.carter@example.com",
  "password": "password123"
}
```

### 4. Use JWT on protected requests

```http
Authorization: Bearer <token>
```

---

## Monitoring

Prometheus is configured in `monitoring/prometheus/prometheus.yml` to scrape:

- api-gateway
- user-service
- property-service
- search-service
- booking-service
- payment-service
- review-service
- notification-service

Grafana runs alongside Prometheus in Docker Compose and can be used for dashboards and service visibility.

---

## Benchmark

The repo includes a concurrency benchmark in `benchmarks/booking_currency_tests.py`.

What it does:

- logs in a guest user
- sends 50 concurrent booking requests for the same property and date range
- expects exactly 1 success and the rest to fail safely

This script is useful for validating overlap protection and race-condition handling in the booking flow.

---

## Local Setup

### Prerequisites

- Java 17
- Maven
- Docker and Docker Compose

### Run Everything with Docker

```bash
docker compose up --build
```

This starts:

- PostgreSQL
- Redis
- Kafka
- Prometheus
- Grafana
- all eight Spring Boot services

### Default Local Ports

- API Gateway: `8088`
- User Service: `8081`
- Property Service: `8082`
- Search Service: `8083`
- Booking Service: `8084`
- Payment Service: `8085`
- Review Service: `8086`
- Notification Service: `8087`

### Common Environment Variables

```text
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
JWT_SECRET
JWT_EXPIRATION_MS
USER_SERVICE_URL
PROPERTY_SERVICE_URL
SEARCH_SERVICE_URL
BOOKING_SERVICE_URL
PAYMENT_SERVICE_URL
REVIEW_SERVICE_URL
NOTIFICATION_SERVICE_URL
KAFKA_BOOTSTRAP_SERVERS
SPRING_DATA_REDIS_HOST
SPRING_DATA_REDIS_PORT
```

---

## Notes

- The project is backend-only. There is no frontend in this repository.
- Search is backed by a separate read model, not direct queries against the property database.
- All services expose Actuator endpoints for monitoring.
- The gateway routes all external traffic through a single entry point.

---

## API Reference Summary

| Service | Key Public APIs |
| --- | --- |
| User | `/api/users`, `/api/auth/login` |
| Property | `/api/properties`, `/api/properties/host/{hostId}`, `/api/properties/city/{city}` |
| Search | `/api/search/properties`, `/api/search/properties/sync` |
| Booking | `/api/bookings`, `/api/bookings/guest/{guestId}`, `/api/bookings/property/{propertyId}` |
| Payment | `/api/payments`, `/api/payments/booking/{bookingId}`, `/api/payments/user/{userId}` |
| Review | `/api/reviews`, `/api/reviews/property/{propertyId}`, `/api/reviews/user/{userId}` |
| Notification | `/api/notifications`, `/api/notifications/user/{userId}` |

