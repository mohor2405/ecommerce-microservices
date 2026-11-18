package com.ecommerce.discoveryserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Eureka Server = the "phone book" of the microservices.
 *
 * <p>Each service registers its name (spring.application.name) and address on startup and
 * sends a heartbeat every 30 s. Other services (like the API Gateway) ask Eureka
 * "where is user-service?" instead of hard-coding localhost:8081.
 *
 * <p>Dashboard: http://localhost:8761
 */
@SpringBootApplication
@EnableEurekaServer
public class DiscoveryServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServerApplication.class, args);
    }
}
