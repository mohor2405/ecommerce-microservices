package com.ecommerce.adminservice;

import com.jayway.jsonpath.JsonPath;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Date;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end test of the whole Admin Service (security + controllers + JPA) on H2.
 * Tokens are created here the same way the User Service creates them, signed with the shared secret.
 */
@SpringBootTest
@ActiveProfiles("test")
class AdminServiceIntegrationTest {

    @Autowired private WebApplicationContext context;
    @Value("${app.jwt.secret}") private String secret;

    private MockMvc mockMvc;
    private String adminToken;
    private String customerToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        adminToken = token("ADMIN", 1L);
        customerToken = token("CUSTOMER", 2L);
    }

    private String token(String role, long userId) {
        return Jwts.builder().subject(role.toLowerCase() + "@example.com")
                .claim("userId", userId).claim("role", role).issuer("user-service")
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret))).compact();
    }

    private static String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private ResultActions send(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
                               String token, String json) throws Exception {
        if (token != null) request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        if (json != null) request.contentType(MediaType.APPLICATION_JSON).content(json);
        return mockMvc.perform(request);
    }

    private long createCategory(String name, boolean active) throws Exception {
        String body = send(post("/api/v1/admin/categories"), adminToken,
                "{\"name\":\"%s\",\"description\":\"test\",\"active\":%s}".formatted(name, active))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private long createProduct(String sku, String name, String price, int stock, long categoryId) throws Exception {
        String body = send(post("/api/v1/admin/products"), adminToken, """
                {"sku":"%s","name":"%s","price":%s,"stockQuantity":%d,"categoryId":%d}
                """.formatted(sku, name, price, stock, categoryId))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    @Test
    @DisplayName("security: no token -> 401, customer -> 403, catalog is public")
    void securityRules() throws Exception {
        send(get("/api/v1/admin/categories"), null, null)
                .andExpect(status().isUnauthorized());
        send(get("/api/v1/admin/categories"), customerToken, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Admin access required"));
        send(get("/api/v1/catalog/products"), null, null)
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("categories: create, duplicate (case-insensitive), validate, update, delete")
    void categoryCrud() throws Exception {
        String name = unique("Electronics");
        String body = send(post("/api/v1/admin/categories"), adminToken, "{\"name\":\"%s\"}".formatted(name))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(body, "$.id")).longValue();

        send(get("/api/v1/admin/categories"), adminToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem(name)));

        send(post("/api/v1/admin/categories"), adminToken, "{\"name\":\"%s\"}".formatted(name.toUpperCase()))
                .andExpect(status().isConflict());
        send(post("/api/v1/admin/categories"), adminToken, "{\"name\":\"\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.name").exists());

        String renamed = unique("Gadgets");
        send(put("/api/v1/admin/categories/" + id), adminToken, "{\"name\":\"%s\"}".formatted(renamed))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(renamed));

        send(delete("/api/v1/admin/categories/" + id), adminToken, null).andExpect(status().isNoContent());
        send(get("/api/v1/admin/categories/" + id), adminToken, null).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("products: create, duplicate SKU, validation, stock, soft delete, category protection")
    void productLifecycle() throws Exception {
        long categoryId = createCategory(unique("Phones"), true);
        String sku = unique("ph").toLowerCase();

        String body = send(post("/api/v1/admin/products"), adminToken, """
                {"sku":"%s","name":"Smartphone X","price":24999.00,"stockQuantity":5,"categoryId":%d,
                 "imageUrl":"https://example.com/x.jpg"}
                """.formatted(sku, categoryId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value(sku.toUpperCase()))
                .andExpect(jsonPath("$.createdBy").value(1))
                .andExpect(jsonPath("$.category.id").value(categoryId))
                .andReturn().getResponse().getContentAsString();
        long productId = ((Number) JsonPath.read(body, "$.id")).longValue();

        // duplicate SKU (different case), unknown category, invalid fields
        send(post("/api/v1/admin/products"), adminToken, """
                {"sku":"%s","name":"Copy","price":1,"stockQuantity":1,"categoryId":%d}
                """.formatted(sku.toUpperCase(), categoryId)).andExpect(status().isConflict());
        send(post("/api/v1/admin/products"), adminToken, """
                {"sku":"%s","name":"X","price":1,"stockQuantity":1,"categoryId":999999}
                """.formatted(unique("SKU"))).andExpect(status().isNotFound());
        send(post("/api/v1/admin/products"), adminToken, """
                {"sku":"a","name":"","price":0,"stockQuantity":-1,"categoryId":%d,"imageUrl":"ftp://x"}
                """.formatted(categoryId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.sku").exists())
                .andExpect(jsonPath("$.validationErrors.price").exists())
                .andExpect(jsonPath("$.validationErrors.stockQuantity").exists())
                .andExpect(jsonPath("$.validationErrors.imageUrl").exists());

        // stock
        send(patch("/api/v1/admin/products/%d/stock".formatted(productId)), adminToken, "{\"quantityChange\":-3}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.stockQuantity").value(2));
        send(patch("/api/v1/admin/products/%d/stock".formatted(productId)), adminToken, "{\"quantityChange\":-10}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Insufficient stock: available 2, requested change -10"));
        send(get("/api/v1/admin/products/low-stock?threshold=2"), adminToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem((int) productId)));

        // a category with products cannot be deleted
        send(delete("/api/v1/admin/categories/" + categoryId), adminToken, null)
                .andExpect(status().isConflict());

        // public view, then soft delete hides it from the catalog but admins still see it
        send(get("/api/v1/catalog/products/" + productId), null, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inStock").value(true))
                .andExpect(jsonPath("$.createdBy").doesNotExist())
                .andExpect(jsonPath("$.stockQuantity").doesNotExist());
        send(delete("/api/v1/admin/products/" + productId), adminToken, null).andExpect(status().isNoContent());
        send(get("/api/v1/admin/products/" + productId), adminToken, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
        send(get("/api/v1/catalog/products/" + productId), null, null).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("listing: filters, sorting, pagination, max page size, bad input")
    void productListing() throws Exception {
        long categoryId = createCategory(unique("Laptops"), true);
        String prefix = unique("Laptop");
        createProduct(unique("LP"), prefix + " Basic", "30000", 10, categoryId);
        createProduct(unique("LP"), prefix + " Pro", "90000", 10, categoryId);
        createProduct(unique("LP"), prefix + " Max", "150000", 10, categoryId);

        send(get("/api/v1/admin/products?name=%s&sort=price,desc&size=2".formatted(prefix)), adminToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].name").value(prefix + " Max"))
                .andExpect(jsonPath("$.content[1].name").value(prefix + " Pro"));

        send(get("/api/v1/catalog/products?categoryId=%d&minPrice=50000&maxPrice=100000".formatted(categoryId)), null, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value(prefix + " Pro"));

        send(get("/api/v1/admin/products?size=1000"), adminToken, null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.size").value(50));
        send(get("/api/v1/admin/products?minPrice=500&maxPrice=100"), adminToken, null)
                .andExpect(status().isBadRequest());
        send(get("/api/v1/admin/products?sort=doesNotExist,asc"), adminToken, null)
                .andExpect(status().isBadRequest());
        send(get("/api/v1/admin/products?minPrice=abc"), adminToken, null)
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("catalog: products in an inactive category are hidden")
    void catalogHidesInactiveCategory() throws Exception {
        String hiddenName = unique("Hidden");
        long hiddenCategory = createCategory(hiddenName, false);
        long productId = createProduct(unique("HD"), "Hidden product", "100", 1, hiddenCategory);

        send(get("/api/v1/catalog/categories"), null, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", not(hasItem(hiddenName))));
        send(get("/api/v1/catalog/products?categoryId=" + hiddenCategory), null, null)
                .andExpect(jsonPath("$.totalElements").value(0));
        send(get("/api/v1/catalog/products/" + productId), null, null)
                .andExpect(status().isNotFound());
    }
}
