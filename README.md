# Spring Boot Microservices Project

A working microservices setup built with Spring Boot: service discovery, centralized configuration,
an API gateway with JWT auth, and three business services talking to each other over REST and Feign.
I built this to learn how the pieces fit together in practice — and to have something concrete
to discuss in interviews beyond textbook definitions.

## Services

| Service       | Port | What it does                                                              |
|---------------|------|---------------------------------------------------------------------------|
| EUREKA-SERVER | 8761 | Service discovery. Every service registers here; check the dashboard at `http://localhost:8761` |
| CONFIG-SERVER | 8888 | Centralized configuration backed by a Git repo                             |
| AUTH          | 8083 | User registration, login, JWT token generation (Spring Security + JJWT)   |
| EMPLOYEE      | 8081 | Employee CRUD. Calls ADDRESS over Feign to attach addresses to responses   |
| ADDRESS       | 8082 | Address CRUD. Optionally validates `empId` against EMPLOYEE before saving  |
| API-GATEWAY   | 9090 | Single entry point. JWT auth filter, routing, circuit breakers + fallbacks |

## Tech stack

- **Java 17**, **Spring Boot 3.5.6**, **Spring Cloud 2025.0.0**
- **Spring Cloud Netflix Eureka** — service registration and discovery
- **Spring Cloud Config Server** — externalized configuration from Git
- **Spring Cloud Gateway (WebFlux/Netty)** — routing, `AuthFilter` (JWT validation on every
  `/employees/**` and `/addresses/**` request), Resilience4j circuit breakers with fallback endpoints
- **OpenFeign** — `EMPLOYEE ↔ ADDRESS` calls, with a custom `ErrorDecoder` mapping downstream
  errors (including a 503 "service is down" path)
- **Resilience4j** — per-service circuit breakers (`EMPLOYEE-SERVICE`, `ADDRESS-SERVICE`),
  5s time limiters, health indicators exposed via Actuator
- **Spring Security + JJWT** — username/password auth in AUTH, `Bearer` tokens enforced at the gateway
- **Spring Data JPA + Hibernate** — MySQL (`testDb`) in normal use; H2 available for quick local runs
- **ModelMapper** — entity ↔ DTO mapping
- **Spring Boot Actuator** — health, metrics, circuit-breaker state

## How it fits together

```
Client → API-GATEWAY (9090, JWT check)
           ├─ /auth/**       → AUTH (8083)
           ├─ /employees/**  → EMPLOYEE (8081)  ──Feign──▶ ADDRESS (8082)
           └─ /addresses/**  → ADDRESS (8082)   ──Feign──▶ EMPLOYEE (8081, when validation is on)

All services register with EUREKA (8761). Config comes from CONFIG-SERVER (8888).
If EMPLOYEE or ADDRESS is down/slow, the gateway's circuit breaker returns a fallback
message ("Employee Service is down. Please try again later.") instead of hanging.
```

## Running it

Prerequisites: JDK 17, Maven (or use the included `mvnw` wrappers), MySQL running locally.

1. Create the database (or let Hibernate create it):
   ```sql
   CREATE DATABASE IF NOT EXISTS testDb;
   ```
   Tables needed: `employees`, `address`, `users` — with `ddl-auto=update` Hibernate will
   create/adjust them for you on first boot.
2. Start in this order and wait for each to finish booting:
   1. `EUREKA-SERVER` (8761)
   2. `CONFIG-SERVER` (8888)
   3. `AUTH` (8083)
   4. `EMPLOYEE` (8081) and `ADDRESS` (8082)
   5. `API-GATEWAY` (9090)
3. Open `http://localhost:8761` — you should see `AUTH`, `EMPLOYEE`, `ADDRESS`, `API-GATEWAY`
   under Applications.
4. A Postman collection (`Microservice Project.postman_collection.json`) is included
   with requests for every endpoint.

Typical flow: `POST /auth/register-user` → `POST /auth/generate-token` → use the token as
`Authorization: Bearer <token>` for gateway calls like `GET localhost:9090/employees/all`.

## Key endpoints

- Auth: `POST /auth/register-user`, `POST /auth/generate-token`
- Employee: `POST /employees/save`, `GET /employees/all`, `GET /employees/{id}`,
  `PUT /employees/update/{id}`, `DELETE /employees/delete/{id}`
- Address: `POST /addresses/save`, `GET /addresses/all-address`, `GET /addresses/empId/{empId}`,
  `GET /addresses/{addressId}`, `PUT /addresses/update`, `DELETE /addresses/delete/{addressId}`
- Gateway fallbacks: `GET /employeeServiceFallback`, `GET /addressServiceFallback`

## If you fork this

Things that are specific to my machine and you will probably want to change:

1. **Database credentials** — `AUTH`, `EMPLOYEE`, and `ADDRESS` each have a
   `spring.datasource.url/username/password` in their `application.properties`.
   Point them at your own MySQL (or switch back to H2 for a zero-setup run).
2. **Config Server Git URI** — `CONFIG-SERVER/src/main/resources/application.yml`
   points at my config repo. Replace it with yours.
3. **JWT secret** — `JwtUtil` in AUTH and API-GATEWAY share a secret key.
   Generate your own and keep it out of source control.
4. **Service URLs** — `employee.service.url` (ADDRESS), `address.service.url` (EMPLOYEE),
   and the gateway `uri` values in `application.yml` assume everything runs on localhost
   with the ports in the table above. Adjust if you deploy elsewhere.
5. **Eureka / gateway coupling** — services register with Eureka, but gateway routes
   currently use direct `http://localhost:port` URIs. To go full discovery-based,
   switch them to `lb://SERVICE-NAME` and put a load balancer in front.
6. **`employee.validation.enabled`** (ADDRESS) — `false` by default so ADDRESS works
   standalone; set it to `true` if you want every address write to verify the employee exists.
   Note the Feign error path for a down EMPLOYEE currently surfaces as a generic error —
   worth tightening up with a proper 503 mapping and a test for it.

## What I'd do next

- Docker Compose for the whole stack (plus MySQL) so it boots with one command
- Distributed tracing (Micrometer + Zipkin) — right now a request crossing
  gateway → employee → address is hard to follow in logs
- Contract/integration tests around the Feign calls and the fallback behavior
- Move secrets and DB credentials to environment variables / a secrets manager
