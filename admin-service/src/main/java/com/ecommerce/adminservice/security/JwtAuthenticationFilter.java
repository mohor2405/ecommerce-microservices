package com.ecommerce.adminservice.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Authenticates each request from its "Authorization: Bearer &lt;token&gt;" header.
 *
 * <p>Key difference from the User Service filter: NO database lookup. The token's signature
 * proves the User Service issued it, so we trust its userId/role claims. This is what lets
 * each microservice authenticate requests on its own.
 *
 * <p>Trade-off: if an admin is demoted or deleted in the User Service, their existing token keeps
 * working here until it expires (1 hour). Short token lifetimes keep that window small.
 *
 * <p>Not a {@code @Component}: it is created in SecurityConfig so Spring Boot does not also
 * register it as a global servlet filter.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** Request attribute read by RestSecurityErrorHandler to explain a 401. */
    public static final String AUTH_ERROR_ATTRIBUTE = "auth_error";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);   // anonymous; public URLs still work
            return;
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();
        try {
            AuthenticatedUser user = jwtService.parseToken(token);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(user, null, user.authorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
        } catch (ExpiredJwtException ex) {
            request.setAttribute(AUTH_ERROR_ATTRIBUTE, "Token has expired");
        } catch (JwtException | IllegalArgumentException ex) {
            request.setAttribute(AUTH_ERROR_ATTRIBUTE, "Invalid token");
        }

        filterChain.doFilter(request, response);
    }
}
