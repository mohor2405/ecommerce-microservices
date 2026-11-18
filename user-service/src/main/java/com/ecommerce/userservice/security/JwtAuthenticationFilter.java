package com.ecommerce.userservice.security;

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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Runs once per HTTP request, before Spring Security decides whether the request is allowed.
 *
 * <ol>
 *   <li>Reads the {@code Authorization: Bearer <token>} header.</li>
 *   <li>Validates the token and loads the user from the database.</li>
 *   <li>Stores the authenticated user in the {@link SecurityContextHolder}.</li>
 * </ol>
 * If the token is missing or invalid we simply continue unauthenticated; Spring Security then
 * returns 401 for protected endpoints (see RestSecurityErrorHandler).
 *
 * <p>It is created in SecurityConfig rather than annotated with {@code @Component}, otherwise
 * Spring Boot would also register it as a global servlet filter.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** Request attribute used to tell the entry point why authentication failed. */
    public static final String AUTH_ERROR_ATTRIBUTE = "auth_error";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();
        try {
            String email = jwtService.extractUsername(token);
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Loading from the DB on each request means a deleted user's token stops working immediately.
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);
            }
        } catch (ExpiredJwtException ex) {
            request.setAttribute(AUTH_ERROR_ATTRIBUTE, "Token has expired");
        } catch (JwtException | IllegalArgumentException ex) {
            request.setAttribute(AUTH_ERROR_ATTRIBUTE, "Invalid token");
        } catch (UsernameNotFoundException ex) {
            request.setAttribute(AUTH_ERROR_ATTRIBUTE, "User for this token no longer exists");
        }

        filterChain.doFilter(request, response);
    }
}
