# E-commerce Microservices

```
                         ┌──────────────────────────┐
                         │ discovery-server  :8761  │  Eureka registry (dashboard: http://localhost:8761)
                         └──────────▲───────────────┘
                 register + heartbeat │  "where is user-service?"
           ┌──────────────────────────┼───────────────────────────┐
           │                          │                           │
Client ──► api-gateway :8080 ──lb://user-service──►  user-service  :8081  ── MySQL user_service_db
(Postman)          │
                   └───────lb://admin-service─────►  admin-service :8082  ── MySQL admin_service_db
```

| Service | Port | Role |
|---|---|---|
| `discovery-server` | 8761 | Eureka server: every service registers its name + address here |
| `api-gateway` | 8080 | Single entry point; routes by path and finds services through Eureka |
| `user-service` | 8081 | Registration, login (issues JWTs), profile, addresses |
| `admin-service` | 8082 | Categories, products, stock (ADMIN) + public catalog |

Stack: Java 21, Spring Boot 4.1.2, Spring Cloud 2025.1.4 (Netflix Eureka, Gateway Server WebMVC).

## Gateway routes

| Path | Routed to |
|---|---|
| `/api/v1/auth/**`, `/api/v1/users/**` | `lb://user-service` |
| `/api/v1/admin/**`, `/api/v1/catalog/**` | `lb://admin-service` |
| anything else | 404 |

`lb://name` means "ask Eureka for a running instance of *name*", so no ports are hard-coded and you can
start a second instance of a service on another port and the gateway will load-balance between them.

Security is unchanged: the gateway forwards the `Authorization` header and each service validates the JWT itself.

## Start order

1. **discovery-server** (wait until http://localhost:8761 opens)
2. **user-service** and **admin-service** (set `DB_PASSWORD` for both)
3. **api-gateway**

Each one: open the folder in IntelliJ and run its `*Application` class, or in PowerShell:

```powershell
cd discovery-server;  .\mvnw spring-boot:run          # terminal 1
cd user-service;      $env:DB_PASSWORD="..."; .\mvnw spring-boot:run   # terminal 2
cd admin-service;     $env:DB_PASSWORD="..."; .\mvnw spring-boot:run   # terminal 3
cd api-gateway;       .\mvnw spring-boot:run          # terminal 4
```

Services appear on the Eureka dashboard within ~10-30 seconds. Until a service has registered,
the gateway answers **503** for its routes. Just wait a moment and retry.

Then use **http://localhost:8080/api/v1/...** for everything (both Postman collections already point there).
The services' own ports (8081/8082) still work for debugging.

## Environment variables

| Variable | Used by | Default |
|---|---|---|
| `EUREKA_URL` | gateway, user-service, admin-service | `http://localhost:8761/eureka` |
| `DB_PASSWORD` / `DB_USERNAME` | user-service, admin-service | - / `root` |
| `JWT_SECRET` | user-service, admin-service (**must be identical**) | dev key |

## Tests

Each project is independent: `.\mvnw test` inside its folder. Service tests use H2 and disable Eureka,
so nothing else needs to be running.
