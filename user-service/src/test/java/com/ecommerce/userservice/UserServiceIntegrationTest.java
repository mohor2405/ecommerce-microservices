package com.ecommerce.userservice;

import com.ecommerce.userservice.entity.Role;
import com.ecommerce.userservice.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end test of the real application (controllers + security + JWT + JPA) against an
 * in-memory H2 database. Proves that all the layers work together over HTTP.
 */
@SpringBootTest
@ActiveProfiles("test")
class UserServiceIntegrationTest {

    private static final String PASSWORD = "Secret123";

    @Autowired private WebApplicationContext context;
    @Autowired private UserRepository userRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    private static String registerJson(String email) {
        return """
                {"firstName":"John","lastName":"Doe","email":"%s","password":"%s","phone":"9876543210"}
                """.formatted(email, PASSWORD);
    }

    private static String addressJson(String line1, Boolean isDefault) {
        return """
                {"addressLine1":"%s","city":"Kolkata","state":"West Bengal","postalCode":"700001",
                 "country":"India","defaultAddress":%s}
                """.formatted(line1, isDefault);
    }

    private String registerAndGetToken(String email) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(registerJson(email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    @DisplayName("register returns 201 with token and never exposes the password")
    void register() throws Exception {
        String email = uniqueEmail();
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(registerJson(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.user.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.user.password").doesNotExist());
    }

    @Test
    @DisplayName("duplicate email returns 409")
    void registerDuplicate() throws Exception {
        String email = uniqueEmail();
        registerAndGetToken(email);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(registerJson(email.toUpperCase())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("invalid registration returns 400 with per-field validation errors")
    void registerValidation() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"","lastName":"Doe","email":"not-an-email","password":"weak"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.firstName").exists())
                .andExpect(jsonPath("$.validationErrors.email").exists())
                .andExpect(jsonPath("$.validationErrors.password").exists());
    }

    @Test
    @DisplayName("login works with correct password and returns 401 with wrong password")
    void login() throws Exception {
        String email = uniqueEmail();
        registerAndGetToken(email);

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"Wrong1234\"}".formatted(email)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("protected endpoints return 401 JSON without a valid token")
    void unauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer garbage"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid token"));
    }

    @Test
    @DisplayName("profile: get, update, change password")
    void profileFlow() throws Exception {
        String email = uniqueEmail();
        String token = registerAndGetToken(email);

        mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));

        mockMvc.perform(put("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Johnny\",\"lastName\":\"Smith\",\"phone\":\"+919999999999\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Johnny"))
                .andExpect(jsonPath("$.phone").value("+919999999999"));

        mockMvc.perform(put("/api/v1/users/me/password").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"%s\",\"newPassword\":\"NewSecret456\"}".formatted(PASSWORD)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"NewSecret456\"}".formatted(email)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("addresses: add, default switching, list, update, delete, ownership")
    void addressFlow() throws Exception {
        String token = registerAndGetToken(uniqueEmail());

        String first = mockMvc.perform(post("/api/v1/users/me/addresses")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(addressJson("Home street 1", null)))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.defaultAddress").value(true))
                .andReturn().getResponse().getContentAsString();
        Integer firstId = JsonPath.read(first, "$.id");

        mockMvc.perform(post("/api/v1/users/me/addresses")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(addressJson("Office road 2", true)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.defaultAddress").value(true));

        mockMvc.perform(get("/api/v1/users/me/addresses").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].defaultAddress").value(false))
                .andExpect(jsonPath("$[1].defaultAddress").value(true));

        mockMvc.perform(put("/api/v1/users/me/addresses/" + firstId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(addressJson("Home street 99", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addressLine1").value("Home street 99"));

        // Another user must not see this user's address.
        String otherToken = registerAndGetToken(uniqueEmail());
        mockMvc.perform(get("/api/v1/users/me/addresses/" + firstId).header(HttpHeaders.AUTHORIZATION, bearer(otherToken)))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/v1/users/me/addresses/" + firstId).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/users/me/addresses").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("address validation errors return 400")
    void addressValidation() throws Exception {
        String token = registerAndGetToken(uniqueEmail());
        mockMvc.perform(post("/api/v1/users/me/addresses").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"addressLine1\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.city").exists());
    }

    @Test
    @DisplayName("authorization: GET /users/{id} is ADMIN only")
    void adminOnlyEndpoint() throws Exception {
        String customerToken = registerAndGetToken(uniqueEmail());
        mockMvc.perform(get("/api/v1/users/1").header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
                .andExpect(status().isForbidden());

        String adminEmail = uniqueEmail();
        registerAndGetToken(adminEmail);
        var admin = userRepository.findByEmail(adminEmail).orElseThrow();
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
        // Role is read from the DB on each request, but log in again to get a token whose claim says ADMIN.
        String body = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(adminEmail, PASSWORD)))
                .andReturn().getResponse().getContentAsString();
        String adminToken = JsonPath.read(body, "$.accessToken");

        mockMvc.perform(get("/api/v1/users/" + admin.getId()).header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @DisplayName("delete account: returns 204 and the old token stops working")
    void deleteAccount() throws Exception {
        String token = registerAndGetToken(uniqueEmail());
        mockMvc.perform(post("/api/v1/users/me/addresses").header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(addressJson("Somewhere 1", null)));

        mockMvc.perform(delete("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnauthorized());
    }
}
