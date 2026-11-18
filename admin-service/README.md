# Admin Service

Second microservice of the e-commerce platform. Administrators use it to manage the **product
catalog** (categories, products, stock), and it exposes a **public read-only catalog** for customers.

| | |
|---|---|
| Java / Spring Boot | 21 / 4.1.2 |
| Database | MySQL 8, own database `admin_service_db` |
| Port | **8082** (user-service = 8081, future gateway = 8080) |
| Base URL | `http://localhost:8082/api/v1` |

---

## 1. Running it

Same prerequisites as the User Service (JDK 21, MySQL 8).

| Env variable | Default | Purpose |
|---|---|---|
| `DB_PASSWORD` | *(empty)* | **Your MySQL password (required)** |
| `DB_USERNAME` | `root` | MySQL user |
| `JWT_SECRET` | same dev key as user-service | **Must be identical in both services** |

IntelliJ: open the `admin-service` folder → Run configuration → environment variable `DB_PASSWORD=...` → run `AdminServiceApplication`.

```powershell
$env:DB_PASSWORD = "your_password"
./mvnw spring-boot:run      # tests: ./mvnw test   (28 tests, H2 - no MySQL needed)
```

### Getting an admin token
This service has **no login**. Tokens come from the User Service:

1. Register a user at `POST http://localhost:8081/api/v1/auth/register`.
2. Make them an admin: `UPDATE user_service_db.users SET role = 'ADMIN' WHERE email = '...';`
3. Log in at `POST http://localhost:8081/api/v1/auth/login` and use the `accessToken` here.

The Postman collection in [`postman/`](postman/admin-service.postman_collection.json) does step 3 for you.

---

## 2. Architecture

The code is organised **by feature** (category, product, catalog), with shared infrastructure in
`config`, `security`, `exception` and `dto`:

```
com.ecommerce.adminservice
├── AdminServiceApplication      (excludes Boot's default in-memory user: no login here)
├── config/          SecurityConfig, JwtProperties
├── security/        JwtService (verify only), JwtAuthenticationFilter (no DB lookup),
│                    AuthenticatedUser (principal from token claims), RestSecurityErrorHandler (401/403 JSON)
├── exception/       ResourceNotFound(404), DuplicateResource(409), Business(409), BadRequest(400),
│                    GlobalExceptionHandler
├── dto/response/    ErrorResponse, PageResponse<T>
├── category/        controller, dto, entity, mapper, repository, service
├── product/         controller, dto, entity, mapper, repository, service,
│                    specification/ProductSpecifications (dynamic filters)
└── catalog/         controller, service (public read-only view, reuses product/category repositories)
```

### How authentication works across services

```
Admin ──login──► User Service (8081) ──► JWT { sub, userId, role, iss="user-service" }
  │
  └──Bearer JWT──► Admin Service (8082)
                     JwtAuthenticationFilter: verify signature (shared JWT_SECRET), expiry, issuer
                     → AuthenticatedUser(userId, email, role)  (no database call)
                     → /api/v1/admin/** requires ROLE_ADMIN
```

Trade-off: a demoted admin's existing token keeps working here until it expires (1 hour).

### Concepts introduced in this service
| Concept | Where |
|---|---|
| Pagination & sorting (`Pageable`, `@PageableDefault`, max page size 50) | `AdminProductController`, `application.yml` |
| Dynamic filtering (Specification pattern) | `ProductSpecifications` |
| Avoiding N+1 queries (`@EntityGraph`) | `ProductRepository` |
| Soft delete | `ProductServiceImpl.deactivateProduct` |
| Optimistic locking (`@Version`) | `Product.version` → 409 on concurrent edits |
| Money as `BigDecimal` / `DECIMAL(10,2)` | `Product.price` |
| Separate public vs admin DTOs | `CatalogProductResponse` vs `ProductResponse` |
| Defence in depth (`@PreAuthorize` + URL rules) | admin controllers |

---

## 3. Database design

```
categories                              products
────────────────────────────            ──────────────────────────────────
id           BIGINT PK AI     1 ─── N   id              BIGINT PK AI
name         VARCHAR(100) UQ            category_id     BIGINT FK → categories.id
description  VARCHAR(500)               sku             VARCHAR(50) UQ  (uppercase)
active       BIT                        name            VARCHAR(150)    (indexed)
created_at   TIMESTAMP                  description     TEXT
updated_at   TIMESTAMP                  price           DECIMAL(10,2)
                                        stock_quantity  INT             (never < 0)
                                        image_url       VARCHAR(500)
                                        active          BIT             (soft delete)
                                        created_by      BIGINT          (admin userId, NO FK)
                                        updated_by      BIGINT
                                        version         BIGINT          (optimistic lock)
                                        created_at, updated_at
```

- `Product.category` is `@ManyToOne(fetch = LAZY)`; **no cascade**, so deleting a category never deletes products.
  A category that still has products (even deactivated ones) cannot be deleted → 409.
- `created_by`/`updated_by` hold ids from **another service's database**, so there is no foreign key.
- Category names are unique ignoring case (checked in the service; DB unique key as a safety net).

---

## 4. API

### Admin endpoints (`Authorization: Bearer <ADMIN token>`)

| Method | Endpoint | Success | Errors |
|---|---|---|---|
| POST | `/admin/categories` | 201 + Location | 400, 409 duplicate name |
| GET | `/admin/categories` | 200 (sorted by name) | |
| GET | `/admin/categories/{id}` | 200 | 404 |
| PUT | `/admin/categories/{id}` | 200 | 400, 404, 409 |
| DELETE | `/admin/categories/{id}` | 204 | 404, 409 has products |
| POST | `/admin/products` | 201 + Location | 400, 404 category, 409 SKU |
| GET | `/admin/products?name=&categoryId=&minPrice=&maxPrice=&active=&page=&size=&sort=` | 200 paged | 400 bad range/sort |
| GET | `/admin/products/{id}` | 200 (incl. inactive) | 404 |
| PUT | `/admin/products/{id}` | 200 | 400, 404, 409 |
| DELETE | `/admin/products/{id}` | 204 (soft delete) | 404 |
| PATCH | `/admin/products/{id}/stock` | 200 | 400 insufficient stock / zero |
| GET | `/admin/products/low-stock?threshold=5` | 200 (lowest stock first) | 400 negative |

No token → **401**, non-admin token → **403**.

### Public catalog (no token)

| Method | Endpoint | Notes |
|---|---|---|
| GET | `/catalog/categories` | active categories only |
| GET | `/catalog/products?name=&categoryId=&minPrice=&maxPrice=&page=&size=&sort=` | active products in active categories |
| GET | `/catalog/products/{id}` | 404 if inactive (indistinguishable from missing) |

### Samples

**Create category**: `POST /api/v1/admin/categories`
```json
{ "name": "Electronics", "description": "Phones, laptops and gadgets" }
```
`201 Created`
```json
{ "id": 1, "name": "Electronics", "description": "Phones, laptops and gadgets", "active": true,
  "createdAt": "2026-11-11T10:00:00Z", "updatedAt": "2026-11-11T10:00:00Z" }
```

**Create product**: `POST /api/v1/admin/products`
```json
{
  "sku": "ph-001",
  "name": "Smartphone X",
  "description": "6.5 inch display, 128 GB",
  "price": 24999.00,
  "stockQuantity": 50,
  "categoryId": 1,
  "imageUrl": "https://example.com/images/phone-x.jpg"
}
```
`201 Created`
```json
{
  "id": 1, "sku": "PH-001", "name": "Smartphone X", "description": "6.5 inch display, 128 GB",
  "price": 24999.00, "stockQuantity": 50, "imageUrl": "https://example.com/images/phone-x.jpg",
  "active": true, "category": { "id": 1, "name": "Electronics" },
  "createdBy": 3, "updatedBy": 3,
  "createdAt": "2026-11-11T10:05:00Z", "updatedAt": "2026-11-11T10:05:00Z"
}
```

**Update product**: `PUT /api/v1/admin/products/1` has the same fields as create, **without `stockQuantity`**
(stock only changes via PATCH), plus optional `"active": true` to re-activate a soft-deleted product.

**Adjust stock**: `PATCH /api/v1/admin/products/1/stock`
```json
{ "quantityChange": -5 }
```
→ `200` with the updated product. If stock would go negative:
```json
{ "timestamp": "...", "status": 400, "error": "Bad Request",
  "message": "Insufficient stock: available 2, requested change -10",
  "path": "/api/v1/admin/products/1/stock" }
```

**Paged listing**: `GET /api/v1/admin/products?name=phone&sort=price,desc&page=0&size=10`
```json
{
  "content": [ { "id": 1, "sku": "PH-001", "name": "Smartphone X", "...": "..." } ],
  "page": 0, "size": 10, "totalElements": 1, "totalPages": 1, "first": true, "last": true
}
```

**Public product**: `GET /api/v1/catalog/products/1` (no admin fields, no exact stock)
```json
{ "id": 1, "sku": "PH-001", "name": "Smartphone X", "description": "6.5 inch display, 128 GB",
  "price": 24999.00, "inStock": true, "imageUrl": "https://example.com/images/phone-x.jpg",
  "category": { "id": 1, "name": "Electronics" } }
```

Errors use the same `ErrorResponse` format as the User Service (with `validationErrors` for 400 validation failures).

---

## 5. Not in scope yet
- User management (list/block users): data belongs to the User Service, so this needs OpenFeign later.
- Orders/payments: those depend on the future Order and Payment services.
- Image upload (URLs only), Flyway migrations (currently `ddl-auto: update`), Eureka/Gateway/Kafka/Docker.
