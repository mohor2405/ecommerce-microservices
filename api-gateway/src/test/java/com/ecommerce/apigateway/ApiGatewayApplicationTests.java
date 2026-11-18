package com.ecommerce.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/** Verifies the gateway starts and its routes configuration is valid (Eureka disabled for the test). */
@SpringBootTest(properties = "eureka.client.enabled=false")
class ApiGatewayApplicationTests {

    @Test
    void contextLoads() {
    }
}
