package com.ecommerce.adminservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

/**
 * Entry point of the Admin Service.
 *
 * <p>UserDetailsServiceAutoConfiguration is excluded because this service has no users table and
 * never checks passwords. Users log in at the User Service; here we only verify their JWT.
 * Without the exclusion Spring Boot would create a default in-memory user with a random password.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
public class AdminServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminServiceApplication.class, args);
    }
}
