# StaySphere — Airbnb-Style Microservices Backend

StaySphere is a backend-only Airbnb-style vacation rental platform built using Spring Boot microservices. It supports users, properties, search, bookings, payments, reviews, notifications, Redis caching, API Gateway routing, inter-service validation, and separate database ownership per service.

This is the **V1 non-advanced implementation**. Advanced production features such as JWT/RBAC, Kafka, Docker Compose, concurrency benchmark, observability, and resilience patterns are planned as future improvements.

---

## Tech Stack

* Java
* Spring Boot
* Spring Web
* Spring Data JPA
* PostgreSQL
* Redis
* Spring Cloud Gateway MVC
* Maven
* Postman

---

## Architecture Overview

```text
Client / Postman
      |
      v
API Gateway :8088
      |
      +-------------------- user-service :8081 -------- user_db
      |
      +---------------- property-service :8082 -------- property_db
      |                         |
      |                         v
      |                 search-service :8083 -------- search_db
      |                         |
      |                         v
      |                       Redis
      |
      +---------------- booking-service :8084 -------- booking_db
      |                         |
      |                         v
      |               notification-service :8087 ----- notification_db
      |
      +---------------- payment-service :8085 -------- payment_db
      |                         |
      |                         v
      |               notification-service :8087
      |
      +---------------- review-service :8086 --------- review_db
                                |
                                v
                      notification-service :8087
```

---

## Service Ports

| Service              | Port |
| -------------------- | ---: |
| API Gateway          | 8088 |
| User Service         | 8081 |
| Property Service     | 8082 |
| Search Service       | 8083 |
| Booking Service      | 8084 |
| Payment Service      | 8085 |
| Review Service       | 8086 |
| Notification Service | 8087 |

---

## Database Strategy

Each service owns its own database. No service directly reads or writes another service's database.

```text
user-service          -> user_db
property-service      -> property_db
search-service        -> search_db
booking-service       -> booking_db
payment-service       -> payment_db
review-service        -> review_db
notification-service  -> notification_db
```

PostgreSQL runs in Docker:

```text
Container: staysphere_postgres_new
Host Port: 5434
Username: staysphere
Password: staysphere
```

Redis runs in Docker:

```text
Container: staysphere_redis
Host Port: 6379
```

---

## Microservice Design Pattern

Each service follows the same layered structure:

```text
controller
service
repository
entity
dto
enums
exception
config
```

Responsibilities:

| Layer      | Responsibility                                |
| ---------- | --------------------------------------------- |
| Controller | Exposes REST APIs                             |
| Service    | Business logic and inter-service calls        |
| Repository | Database access                               |
| Entity     | Database model                                |
| DTO        | Request/response objects                      |
| Enums      | Status/type values                            |
| Exception  | Global API error handling                     |
| Config     | RestTemplate, Redis, or service configuration |

---

## API Gateway Routes

All APIs can be accessed through the gateway on port `8088`.

```properties
/api/users/**          -> user-service
/api/properties/**     -> property-service
/api/search/**         -> search-service
/api/bookings/**       -> booking-service
/api/payments/**       -> payment-service
/api/reviews/**        -> review-service
/api/notifications/**  -> notification-service
```

---

## Implemented Services

---

# 1. User Service

Manages platform users.

## Entity

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

## Roles

```text
GUEST
HOST
ADMIN
```

## Endpoints

| Method | Endpoint          | Description    |
| ------ | ----------------- | -------------- |
| POST   | `/api/users`      | Create user    |
| GET    | `/api/users`      | Get all users  |
| GET    | `/api/users/{id}` | Get user by id |
| DELETE | `/api/users/{id}` | Delete user    |

## Example Create User

```http
POST http://localhost:8088/api/users
```

```json
{
  "name": "Emma Carter",
  "email": "emma.carter@example.com",
  "password": "password123",
  "role": "GUEST"
}
```

---

# 2. Property Service

Manages rental properties.

## Entity

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

## Property Types

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

## Property Status

```text
PENDING
ACTIVE
INACTIVE
SUSPENDED
```

## Endpoints

| Method | Endpoint                        | Description            |
| ------ | ------------------------------- | ---------------------- |
| POST   | `/api/properties`               | Create property        |
| GET    | `/api/properties`               | Get all properties     |
| GET    | `/api/properties/{id}`          | Get property by id     |
| GET    | `/api/properties/host/{hostId}` | Get properties by host |
| GET    | `/api/properties/city/{city}`   | Get properties by city |
| PUT    | `/api/properties/{id}`          | Update property        |
| DELETE | `/api/properties/{id}`          | Delete property        |

## Inter-Service Rules

Property Service calls User Service to validate:

```text
hostId must exist
```

Property deletion is blocked if the property has active bookings:

```text
PENDING or CONFIRMED bookings prevent deletion
```

## Example Create Property

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

# 3. Search Service

Provides fast property search using a separate searchable read model.

## Architecture

```text
property-service
      |
      v
search-service
      |
      v
search_db
      |
      v
Redis cache
```

Property Service remains the source of truth. Search Service keeps a denormalized copy of searchable property data.

## Entity

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

## Search Filters

```text
city
country
propertyType
maxGuests
minPrice
maxPrice
```

## Redis Caching

Search results are cached in Redis.

```text
TTL = 5 minutes
```

Cache is cleared whenever property data syncs into Search Service.

## Endpoints

| Method | Endpoint                      | Description                        |
| ------ | ----------------------------- | ---------------------------------- |
| POST   | `/api/search/properties/sync` | Sync property into search database |
| GET    | `/api/search/properties`      | Search properties                  |

## Example Search

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

# 4. Booking Service

Handles reservations and prevents double booking.

## Entity

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

## Booking Status

```text
PENDING
CONFIRMED
CANCELLED
EXPIRED
```

## Endpoints

| Method | Endpoint                              | Description              |
| ------ | ------------------------------------- | ------------------------ |
| POST   | `/api/bookings`                       | Create booking           |
| GET    | `/api/bookings/{id}`                  | Get booking by id        |
| GET    | `/api/bookings/guest/{guestId}`       | Get bookings by guest    |
| GET    | `/api/bookings/property/{propertyId}` | Get bookings by property |
| PUT    | `/api/bookings/{id}`                  | Update booking           |
| DELETE | `/api/bookings/{id}`                  | Cancel booking           |

## Inter-Service Rules

Booking Service validates:

```text
guestId exists in user-service
propertyId exists in property-service
property status is ACTIVE
```

After successful booking:

```text
booking-service creates BOOKING_CREATED notification
```

## Double Booking Protection

Booking Service prevents overlapping bookings using:

### Date overlap check

```text
existingCheckIn < requestedCheckOut
AND
existingCheckOut > requestedCheckIn
```

### PostgreSQL advisory lock

```sql
pg_advisory_xact_lock(propertyId)
```

### Optimistic locking

```java
@Version
```

## Example Create Booking

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

# 5. Payment Service

Handles mock payment flow.

## Entity

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

## Payment Status

```text
PENDING
SUCCESS
FAILED
REFUNDED
```

## Payment Provider

```text
MOCK
STRIPE
RAZORPAY
```

## Endpoints

| Method | Endpoint                            | Description            |
| ------ | ----------------------------------- | ---------------------- |
| POST   | `/api/payments`                     | Create payment         |
| GET    | `/api/payments/{id}`                | Get payment by id      |
| GET    | `/api/payments/booking/{bookingId}` | Get payment by booking |
| GET    | `/api/payments/user/{userId}`       | Get payments by user   |
| PATCH  | `/api/payments/{id}/refund`         | Refund payment         |

## Inter-Service Rules

Payment Service validates booking by calling Booking Service:

```text
booking exists
booking belongs to same user
booking status is CONFIRMED
payment amount matches booking totalAmount
payment currency matches booking currency
```

Payment Service also prevents duplicate payments:

```text
only one payment per booking
```

After successful payment:

```text
payment-service creates PAYMENT_SUCCESS notification
```

## Example Create Payment

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

# 6. Review Service

Handles property reviews.

## Entity

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

## Endpoints

| Method | Endpoint                             | Description             |
| ------ | ------------------------------------ | ----------------------- |
| POST   | `/api/reviews`                       | Create review           |
| GET    | `/api/reviews/{id}`                  | Get review by id        |
| GET    | `/api/reviews/property/{propertyId}` | Get reviews by property |
| GET    | `/api/reviews/user/{userId}`         | Get reviews by user     |
| DELETE | `/api/reviews/{id}`                  | Delete review           |

## Inter-Service Rules

Review Service validates booking by calling Booking Service:

```text
booking exists
booking belongs to same user
booking belongs to same property
booking status is CONFIRMED
```

Review Service validates payment by calling Payment Service:

```text
payment exists for booking
payment status is SUCCESS
```

Review Service also prevents duplicate reviews:

```text
only one review per booking
```

After successful review:

```text
review-service creates REVIEW_CREATED notification
```

## Example Create Review

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

# 7. Notification Service

Stores user notifications.

## Entity

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

## Notification Type

```text
BOOKING_CREATED
BOOKING_CANCELLED
PAYMENT_SUCCESS
PAYMENT_FAILED
REVIEW_CREATED
GENERAL
```

## Notification Status

```text
PENDING
SENT
FAILED
READ
```

## Endpoints

| Method | Endpoint                                  | Description               |
| ------ | ----------------------------------------- | ------------------------- |
| POST   | `/api/notifications`                      | Create notification       |
| GET    | `/api/notifications/{id}`                 | Get notification by id    |
| GET    | `/api/notifications/user/{userId}`        | Get notifications by user |
| GET    | `/api/notifications/user/{userId}/unread` | Get unread notifications  |
| PATCH  | `/api/notifications/{id}/read`            | Mark notification as read |
| DELETE | `/api/notifications/{id}`                 | Delete notification       |

## Example Get User Notifications

```http
GET http://localhost:8088/api/notifications/user/4
```

---

## End-to-End Workflow

The full V1 platform workflow is:

```text
1. Create host user
2. Create guest user
3. Host creates property
4. Property syncs to search-service
5. Guest searches property
6. Guest creates booking
7. Booking validates user and property
8. Booking sends notification
9. Guest creates payment
10. Payment validates booking
11. Payment sends notification
12. Guest creates review
13. Review validates booking and payment
14. Review sends notification
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

### 3. Create Property

```http
POST /api/properties
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

### 4. Search Property

```http
GET /api/search/properties?city=Interlaken
```

### 5. Create Booking

```http
POST /api/bookings
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

### 6. Create Payment

```http
POST /api/payments
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

### 7. Create Review

```http
POST /api/reviews
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

### 8. Check Notifications

```http
GET /api/notifications/user/4
```

Expected notification types:

```text
BOOKING_CREATED
PAYMENT_SUCCESS
REVIEW_CREATED
```

---

## Business Rules Implemented

### Property Rules

* Property must belong to an existing user.
* Property creation triggers sync to Search Service.
* Property deletion is blocked if active bookings exist.

### Search Rules

* Search Service stores a denormalized property model.
* Search only returns searchable property data.
* Redis caches repeated search queries.
* Cache is invalidated when property data changes.

### Booking Rules

* Guest must exist.
* Property must exist.
* Property must be ACTIVE.
* Dates must be valid.
* Overlapping bookings are rejected.
* Booking cancellation changes status to CANCELLED.
* Booking creation triggers notification.

### Payment Rules

* Booking must exist.
* Payment user must match booking guest.
* Booking must be CONFIRMED.
* Amount must match booking total.
* Currency must match booking currency.
* Only one payment is allowed per booking.
* Successful payment triggers notification.

### Review Rules

* Booking must exist.
* Review user must match booking guest.
* Review property must match booking property.
* Booking must be CONFIRMED.
* Payment must exist and be SUCCESS.
* Only one review is allowed per booking.
* Review creation triggers notification.

### Notification Rules

* Notifications are created for booking, payment, and review events.
* Notifications can be marked as READ.
* Notifications can be fetched by user.

---

## Error Handling

All services use structured API errors.

Example:

```json
{
  "timestamp": "2026-06-03T13:26:00.8845299",
  "status": 400,
  "error": "Bad Request",
  "message": "Payment already exists for booking id: 6",
  "path": "/api/payments",
  "validationErrors": null
}
```

Validation errors return field-level messages using `validationErrors`.

---

## Redis Validation

Redis search cache was tested by:

```text
1. Searching a property
2. Confirming Redis key creation
3. Checking TTL
4. Repeating the same query
5. Confirming cached response
6. Stopping PostgreSQL
7. Confirming cached search still returns
```

Example Redis key:

```text
search:properties:city=Interlaken:country=null:type=null:maxGuests=null:minPrice=null:maxPrice=null
```

---

## Postman Collection

The project includes a Postman collection covering:

```text
User APIs
Property APIs
Search APIs
Booking APIs
Payment APIs
Review APIs
Notification APIs
End-to-End Flow
Negative Test Cases
```

Tested flows include:

```text
property sync
search cache
booking validation
overlap rejection
payment validation
duplicate payment rejection
review payment validation
duplicate review rejection
notification generation
gateway routing
```

---

## Current V1 Status

Completed:

```text
API Gateway
User Service
Property Service
Search Service
Booking Service
Payment Service
Review Service
Notification Service
PostgreSQL per service
Redis caching
Property-to-search sync
Inter-service validations
Notification workflows
Booking overlap prevention
Payment and review business rules
Postman tested APIs
```

---

## Known Limitations

The current version intentionally keeps some production-grade features out of scope.

```text
No JWT authentication yet
No role-based authorization yet
No Kafka/event bus yet
No Docker Compose for all services yet
No distributed tracing yet
No centralized logging yet
No circuit breakers/retries yet
No real payment gateway integration yet
No Elasticsearch yet
No concurrency benchmark report yet
```

---

## Future Improvements

Planned advanced improvements:

```text
JWT Authentication
Role-Based Access Control
Docker Compose for full platform startup
Kafka-based event-driven architecture
Booking concurrency benchmark
OpenTelemetry + Zipkin tracing
Resilience4j circuit breakers and retries
Elasticsearch for advanced search
Stripe/Razorpay integration
CI/CD pipeline
Centralized logging
Service discovery
```

---

## Resume Summary

StaySphere is a distributed Airbnb-style backend platform built with Spring Boot microservices, PostgreSQL, Redis, and API Gateway. It implements independent database ownership, service-to-service validation, denormalized search with caching, booking overlap prevention, mock payment workflows, reviews, notifications, and structured API error handling.

Resume bullet example:

```text
Built StaySphere, an Airbnb-style Spring Boot microservices backend with 7 independent services, API Gateway routing, PostgreSQL-per-service architecture, Redis-backed property search, inter-service validation, booking overlap prevention, mock payment workflows, reviews, and notification pipelines.
```
