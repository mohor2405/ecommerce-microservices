package com.ecommerce.userservice.security;

import com.ecommerce.userservice.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Writes JSON errors for failures that happen inside the security filter chain,
 * i.e. before any controller (and therefore before GlobalExceptionHandler) is reached.
 * <ul>
 *   <li>{@link AuthenticationEntryPoint}: no/invalid token on a protected endpoint -> 401</li>
 *   <li>{@link AccessDeniedHandler}: authenticated but missing the required role -> 403</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class RestSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        Object reason = request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_ATTRIBUTE);
        String message = reason != null ? reason.toString() : "Authentication is required to access this resource";
        write(response, HttpStatus.UNAUTHORIZED, message, request);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        write(response, HttpStatus.FORBIDDEN, "You do not have permission to access this resource", request);
    }

    private void write(HttpServletResponse response, HttpStatus status, String message,
                       HttpServletRequest request) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(),
                ErrorResponse.of(status, message, request.getRequestURI()));
    }
}
