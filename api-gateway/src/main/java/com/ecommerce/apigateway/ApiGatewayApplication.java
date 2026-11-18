package com.ecommerce.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway = the single front door of the system (http://localhost:8080).
 *
 * <p>Clients no longer need to know which service lives on which port. The routes in
 * application.yml match the URL path and forward the request to "lb://service-name";
 * the "lb" (load balancer) scheme asks Eureka for a running instance of that service.
 *
 * <p>Security stays inside each service: the gateway forwards the Authorization header
 * untouched, and user-service / admin-service validate the JWT themselves.
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
