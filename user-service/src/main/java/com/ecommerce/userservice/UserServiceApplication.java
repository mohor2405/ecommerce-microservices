package com.ecommerce.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point of the User Service.
 *
 * <p>{@code @SpringBootApplication} = {@code @Configuration} + {@code @EnableAutoConfiguration}
 * + {@code @ComponentScan}: it scans this package and all sub-packages for Spring beans.
 * <p>{@code @ConfigurationPropertiesScan} registers {@code @ConfigurationProperties} records
 * such as {@link com.ecommerce.userservice.config.JwtProperties}.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
