package com.ecommerce.adminservice.config;

import com.ecommerce.adminservice.security.JwtAuthenticationFilter;
import com.ecommerce.adminservice.security.JwtService;
import com.ecommerce.adminservice.security.RestSecurityErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration for the Admin Service.
 *
 * <p>Differences from the User Service:
 * <ul>
 *   <li>No PasswordEncoder / AuthenticationManager / UserDetailsService: there is no login here.</li>
 *   <li>The JWT filter trusts the token's claims and does not query a database.</li>
 *   <li>URL rules: public read-only catalog, everything under /api/v1/admin requires ADMIN.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // allows @PreAuthorize on individual methods if ever needed
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtService jwtService,
                                                   RestSecurityErrorHandler securityErrorHandler) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)          // stateless Bearer tokens, no cookies
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Customers (or anyone) can browse the catalog without logging in.
                        .requestMatchers(HttpMethod.GET, "/api/v1/catalog/**").permitAll()
                        // Health/info for Eureka and monitoring (only these two are exposed, see application.yml)
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/error").permitAll()
                        // All management endpoints: ADMIN only. hasRole("ADMIN") checks for authority "ROLE_ADMIN".
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(securityErrorHandler)  // no/invalid token -> 401 JSON
                        .accessDeniedHandler(securityErrorHandler))      // valid token, not ADMIN -> 403 JSON
                .addFilterBefore(new JwtAuthenticationFilter(jwtService),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
