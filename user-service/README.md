# User Service

The first microservice of the e-commerce platform. It handles registration, login (JWT),
profile management and address management. It is a fully independent Spring Boot application
with its own MySQL database. There is no Eureka, Gateway, Kafka or Docker yet, but it is designed so
those can be added later without a rewrite.

| | |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.2 (Spring Framework 7, Spring Security 7, Hibernate 7) |
| Database | MySQL 8 (`user_service_db`) |
| Port | **8081** (8080 is reserved for the future API Gateway) |
| Base URL | `http://localhost:8081/api/v1` |

---

## 1. Running it

### Prerequisites
- JDK 21 (in IntelliJ: *File → Project Structure → SDK → Download JDK → 21, Eclipse Temurin*)
- MySQL 8 running on `localhost:3306`
- Maven 3.9+ (or use the included `mvnw` wrapper)

### Configuration (environment variables)

| Variable | Default | Purpose |
|---|---|---|
| `DB_PASSWORD` | *(empty)* | **Your MySQL password (required)** |
| `DB_USERNAME` | `root` | MySQL user |
| `DB_URL` | `jdbc:mysql://localhost:3306/user_service_db?createDatabaseIfNotExist=true` | JDBC URL |
| `JWT_SECRET` | dev-only key | Base64 key ≥ 256 bits. **Set your own outside local dev** (`openssl rand -base64 32`) |
| `JWT_EXPIRATION_MS` | `3600000` | Token lifetime (1 hour) |

The database `user_service_db` is created automatically on first start, and Hibernate creates the tables.

### From IntelliJ IDEA
1. *File → Open* → select the `user-service` folder (the one containing `pom.xml`).
2. Enable annotation processing when prompted (needed for Lombok).
3. *Run → Edit Configurations → UserServiceApplication → Environment variables*: `DB_PASSWORD=your_password`
4. Run `UserServiceApplication`. Look for `Tomcat started on port 8081`.

### From the command line (PowerShell)
```powershell
$env:DB_PASSWORD = "your_password"
./mvnw spring-boot:run
```

### Tests
```powershell
./mvnw test
```
35 tests: Mockito unit tests for every service + JWT, and an end-to-end integration test that
runs the whole app against an in-memory H2 database (no MySQL needed for tests).

---

## 2. Architecture

```
HTTP request (JSON)
   │
   ▼
JwtAuthenticationFilter ── reads "Authorization: Bearer <token>", validates it, loads the user
   │                        (invalid/missing token on a protected URL → RestSecurityErrorHandler → 401 JSON)
   ▼
Controller  ─── @Valid request DTO → calls service → returns response DTO
   │
   ▼
Service (interface + impl) ─── business rules, @Transactional; uses Mapper (Entity ⇄ DTO)
   │
   ▼
Repository (Spring Data JPA) ─── generated SQL
   │
   ▼
MySQL (user_service_db)

Any exception thrown in controller/service → GlobalExceptionHandler → consistent JSON error
```

### Package structure

```
com.ecommerce.userservice
├── UserServiceApplication          Entry point
├── config/
│   ├── SecurityConfig              Filter chain, stateless sessions, public vs protected URLs, BCrypt, AuthenticationManager
│   └── JwtProperties               Type-safe binding of app.jwt.* (fails fast if missing)
├── controller/
│   ├── AuthController              POST /auth/register, /auth/login
│   ├── UserController              /users/me (get, update, change password, delete), /users/{id} (ADMIN)
│   └── AddressController           /users/me/addresses CRUD
├── dto/request/                    RegisterRequest, LoginRequest, UpdateUserRequest, ChangePasswordRequest,
│                                   AddressRequest, ValidationPatterns  (records + Bean Validation)
├── dto/response/                   UserResponse, AuthResponse, AddressResponse, ErrorResponse
├── entity/                         User, Address, Role
├── exception/                      ResourceNotFound(404), DuplicateResource(409), InvalidPassword(400),
│                                   GlobalExceptionHandler
├── mapper/                         UserMapper, AddressMapper
├── repository/                     UserRepository, AddressRepository
├── security/
│   ├── JwtService                  Create/validate HS256 tokens
│   ├── JwtAuthenticationFilter     Authenticates each request from its Bearer token
│   ├── CustomUserDetailsService    Loads users from MySQL for Spring Security
│   ├── UserPrincipal               Adapter: User entity → Spring Security UserDetails
│   └── RestSecurityErrorHandler    JSON 401/403 for errors raised inside the security filters
└── service/                        AuthService, UserService, AddressService (+ impl/)
```

### Design principles applied
- **Single Responsibility**: controllers only handle HTTP, services hold business rules, repositories handle data access, mappers handle conversion.
- **Dependency Inversion**: controllers depend on service *interfaces*, which makes them mockable in tests.
- **DTO pattern**: entities never leave the service layer, so `password` cannot leak (there is no such field in any response DTO).
- **Adapter pattern**: `UserPrincipal` adapts `User` to Spring Security without coupling the entity to Security.
- **Constructor injection** everywhere (`@RequiredArgsConstructor` + `final` fields).

### Security decisions
- Passwords are hashed with **BCrypt** (random salt and slow on purpose).
- **Stateless JWT**: no HTTP session; CSRF disabled because no cookies are used.
- Self-service URLs use **`/me`**, so the user id comes from the token and never from the URL. You cannot act on someone else's account by changing an id.
- Addresses are looked up by **id AND owner**. Another user's address returns 404, so its existence is not revealed.
- Login errors are generic ("Invalid email or password") so nobody can probe which emails are registered.
- Registration always assigns `CUSTOMER`; the client cannot choose its role.
- The user is reloaded from the DB on each request, so a **deleted account's token stops working immediately**.

---

## 3. Database design

```
┌──────────────────────────────┐          ┌──────────────────────────────┐
│ users                        │          │ addresses                    │
├──────────────────────────────┤          ├──────────────────────────────┤
│ id           BIGINT PK AI    │ 1      N │ id            BIGINT PK AI   │
│ first_name   VARCHAR(50) NN  │──────────│ user_id       BIGINT FK NN   │
│ last_name    VARCHAR(50) NN  │          │ address_line1 VARCHAR(255) NN│
│ email        VARCHAR(100) UQ │          │ address_line2 VARCHAR(255)   │
│ password     VARCHAR(100) NN │          │ city          VARCHAR(100) NN│
│ phone        VARCHAR(20)     │          │ state         VARCHAR(100) NN│
│ role         VARCHAR(20) NN  │          │ postal_code   VARCHAR(10) NN │
│ created_at   TIMESTAMP(6) NN │          │ country       VARCHAR(100) NN│
│ updated_at   TIMESTAMP(6) NN │          │ is_default    BIT NN         │
└──────────────────────────────┘          │ created_at    TIMESTAMP(6) NN│
                                          │ updated_at    TIMESTAMP(6) NN│
                                          └──────────────────────────────┘
```

Equivalent DDL (Hibernate generates the real schema from the entities; for `java.time.Instant` it may choose `DATETIME(6)` instead of `TIMESTAMP(6)` depending on version):

```sql
CREATE TABLE users (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  first_name  VARCHAR(50)  NOT NULL,
  last_name   VARCHAR(50)  NOT NULL,
  email       VARCHAR(100) NOT NULL,
  password    VARCHAR(100) NOT NULL,
  phone       VARCHAR(20),
  role        VARCHAR(20)  NOT NULL,
  created_at  TIMESTAMP(6) NOT NULL,
  updated_at  TIMESTAMP(6) NOT NULL,
  CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE addresses (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id        BIGINT       NOT NULL,
  address_line1  VARCHAR(255) NOT NULL,
  address_line2  VARCHAR(255),
  city           VARCHAR(100) NOT NULL,
  state          VARCHAR(100) NOT NULL,
  postal_code    VARCHAR(10)  NOT NULL,
  country        VARCHAR(100) NOT NULL,
  is_default     BIT          NOT NULL,
  created_at     TIMESTAMP(6) NOT NULL,
  updated_at     TIMESTAMP(6) NOT NULL,
  CONSTRAINT fk_addresses_user FOREIGN KEY (user_id) REFERENCES users (id),
  INDEX idx_addresses_user_id (user_id)
);
```

### Relationship: one user → many addresses
- `Address.user` is `@ManyToOne(fetch = LAZY)`. It is the **owning side** and holds the `user_id` foreign key.
- `User.addresses` is `@OneToMany(mappedBy = "user", cascade = ALL, orphanRemoval = true)`, the inverse side.
  Deleting a user deletes their addresses.
- `email` has a **unique constraint**, and the service also checks it first to return a friendly 409.
- `role` is stored as text (`EnumType.STRING`), so reordering the enum can't corrupt data.
- Business rule in code: **exactly one default address per user.** The first address becomes the default,
  marking another as default un-defaults the old one, and deleting the default promotes the oldest remaining address.

### Microservice note
This database belongs **only** to the User Service. A future Order Service will store just the
`userId` (taken from the JWT) and never join or hold a foreign key to these tables.

---

## 4. API reference

All protected endpoints need the header `Authorization: Bearer <accessToken>`.
A ready-made Postman collection is in [`postman/user-service.postman_collection.json`](postman/user-service.postman_collection.json)
(*Postman → Import*). Register/Login automatically store the token for all other requests.

| Method | Endpoint | Auth | Success |
|---|---|---|---|
| POST | `/api/v1/auth/register` | public | 201 |
| POST | `/api/v1/auth/login` | public | 200 |
| GET | `/api/v1/users/me` | user | 200 |
| PUT | `/api/v1/users/me` | user | 200 |
| PUT | `/api/v1/users/me/password` | user | 204 |
| DELETE | `/api/v1/users/me` | user | 204 |
| GET | `/api/v1/users/{id}` | **ADMIN** | 200 |
| POST | `/api/v1/users/me/addresses` | user | 201 + `Location` header |
| GET | `/api/v1/users/me/addresses` | user | 200 |
| GET | `/api/v1/users/me/addresses/{addressId}` | user | 200 |
| PUT | `/api/v1/users/me/addresses/{addressId}` | user | 200 |
| DELETE | `/api/v1/users/me/addresses/{addressId}` | user | 204 |

### Register: `POST /api/v1/auth/register`
Request:
```json
{
  "firstName": "Soupik",
  "lastName": "Chowdhury",
  "email": "soupik@example.com",
  "password": "Secret123",
  "phone": "9876543210"
}
```
Rules: names ≤ 50 chars; valid email; password 8–64 chars with an uppercase letter, a lowercase letter and a digit;
phone optional, 10–15 digits, may start with `+`.

Response `201 Created`:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJzb3VwaWtAZXhhbXBsZS5jb20iLCJ1c2VySWQiOjEsInJvbGUiOiJDVVNUT01FUiIs...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "id": 1,
    "firstName": "Soupik",
    "lastName": "Chowdhury",
    "email": "soupik@example.com",
    "phone": "9876543210",
    "role": "CUSTOMER",
    "createdAt": "2026-11-10T14:30:00.123456Z",
    "updatedAt": "2026-11-10T14:30:00.123456Z"
  }
}
```

### Login: `POST /api/v1/auth/login`
```json
{ "email": "soupik@example.com", "password": "Secret123" }
```
Response `200 OK` has the same shape as register. Wrong credentials → `401`.

### Get profile: `GET /api/v1/users/me`
Response `200 OK`:
```json
{
  "id": 1,
  "firstName": "Soupik",
  "lastName": "Chowdhury",
  "email": "soupik@example.com",
  "phone": "9876543210",
  "role": "CUSTOMER",
  "createdAt": "2026-11-10T14:30:00.123456Z",
  "updatedAt": "2026-11-10T14:30:00.123456Z"
}
```

### Update profile: `PUT /api/v1/users/me`
```json
{ "firstName": "Soupik", "lastName": "Chowdhury", "phone": "+919876543210" }
```
Response `200 OK`: the updated profile (as above). Email and role cannot be changed here.

### Change password: `PUT /api/v1/users/me/password`
```json
{ "currentPassword": "Secret123", "newPassword": "NewSecret456" }
```
Response `204 No Content`. Wrong current password, or a new password equal to the old one → `400`.

### Delete account: `DELETE /api/v1/users/me`
Response `204 No Content`. The user's addresses are deleted too, and the token stops working.

### Get user by id (ADMIN): `GET /api/v1/users/{id}`
`200` with a user profile for admins; `403` for customers. There is no admin sign-up (by design).
To make an admin while developing:
```sql
UPDATE user_service_db.users SET role = 'ADMIN' WHERE email = 'soupik@example.com';
```
Then log in again.

### Add address: `POST /api/v1/users/me/addresses`
```json
{
  "addressLine1": "12 Park Street",
  "addressLine2": "Flat 4B",
  "city": "Kolkata",
  "state": "West Bengal",
  "postalCode": "700016",
  "country": "India",
  "defaultAddress": true
}
```
Response `201 Created` (header `Location: http://localhost:8081/api/v1/users/me/addresses/1`):
```json
{
  "id": 1,
  "addressLine1": "12 Park Street",
  "addressLine2": "Flat 4B",
  "city": "Kolkata",
  "state": "West Bengal",
  "postalCode": "700016",
  "country": "India",
  "defaultAddress": true,
  "createdAt": "2026-11-10T14:35:00.000000Z",
  "updatedAt": "2026-11-10T14:35:00.000000Z"
}
```

### List addresses: `GET /api/v1/users/me/addresses`
`200 OK` → JSON array of address objects (oldest first).

### Get / Update / Delete one address: `/api/v1/users/me/addresses/{addressId}`
- `GET` → `200` address object
- `PUT` (same body as add) → `200` updated address. `"defaultAddress": true` makes it the default.
  To move the default, set `true` on another address (`false` is ignored, so you always keep one default).
- `DELETE` → `204`. If it was the default, your oldest remaining address becomes the default.
- Someone else's address id → `404`.

---

## 5. Error format

Every error, whether from validation, business rules or security, has the same shape:

```json
{
  "timestamp": "2026-11-10T14:40:00.123Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/auth/register",
  "validationErrors": {
    "firstName": "First name is required",
    "email": "Email must be a valid email address",
    "password": "Password must be between 8 and 64 characters"
  }
}
```
(`validationErrors` appears only for validation failures.)

| Status | When |
|---|---|
| 400 | Validation failed, malformed JSON, wrong current password, bad path variable type |
| 401 | Missing/invalid/expired token (`"Token has expired"`, `"Invalid token"`), wrong login credentials |
| 403 | Logged in but lacking the required role |
| 404 | User/address not found, unknown endpoint |
| 405 / 415 | Wrong HTTP method / Content-Type is not JSON |
| 409 | Email already registered |
| 500 | Unexpected error (details are logged and never sent to the client) |

---

## 6. Ready for future microservice integration

Already in place:
- **Own database** (database-per-service).
- **`spring.application.name: user-service`**: becomes the Eureka service ID.
- **JWT carries `sub` (email), `userId`, `role`**: a Gateway or other services can authorise requests by validating
  the token with the shared `JWT_SECRET`, without calling this service.
- **All config via environment variables**: maps directly to Config Server / Docker env later.
- **Versioned API** (`/api/v1`) and port 8081 (Gateway gets 8080).

Known limitations to address later:
- `ddl-auto: update` is for development only. Use Flyway migrations before production.
- After a password change, previously issued tokens stay valid until they expire (1h). Possible fixes: short-lived
  tokens + refresh tokens, or a token version stored on the user.
- No rate limiting on login (best done at the API Gateway).
