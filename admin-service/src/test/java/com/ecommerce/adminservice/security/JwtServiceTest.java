package com.ecommerce.adminservice.security;

import com.ecommerce.adminservice.config.JwtProperties;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * This service only VERIFIES tokens, so the test creates tokens itself, exactly like the
 * User Service does, and checks what JwtService accepts and rejects.
 */
class JwtServiceTest {

    private static final String SECRET = "dGVzdC1zZWNyZXQta2V5LWZvci11bml0LXRlc3RzLW9ubHktMzJieXRlcyE=";
    private static final String OTHER_SECRET = "YW5vdGhlci1zZWNyZXQta2V5LWZvci11bml0LXRlc3RzLTMyYnl0ZXMhIQ==";

    private final JwtService jwtService = new JwtService(new JwtProperties(SECRET, "user-service"));

    private static String token(String secret, String issuer, String role, Long userId, long ttlMs) {
        var builder = Jwts.builder()
                .subject("admin@example.com")
                .issuer(issuer)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ttlMs));
        if (role != null) builder.claim("role", role);
        if (userId != null) builder.claim("userId", userId);
        return builder.signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret))).compact();
    }

    @Test
    void validToken_returnsUserFromClaims() {
        AuthenticatedUser user = jwtService.parseToken(token(SECRET, "user-service", "ADMIN", 3L, 60_000));

        assertThat(user.userId()).isEqualTo(3L);
        assertThat(user.email()).isEqualTo("admin@example.com");
        assertThat(user.role()).isEqualTo("ADMIN");
        assertThat(user.authorities()).extracting(Object::toString).containsExactly("ROLE_ADMIN");
    }

    @Test
    void expiredToken_isRejected() {
        String token = token(SECRET, "user-service", "ADMIN", 3L, -5_000);
        assertThatThrownBy(() -> jwtService.parseToken(token)).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void tokenSignedWithAnotherKey_isRejected() {
        String token = token(OTHER_SECRET, "user-service", "ADMIN", 3L, 60_000);
        assertThatThrownBy(() -> jwtService.parseToken(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void tokenFromAnotherIssuer_isRejected() {
        String token = token(SECRET, "some-other-service", "ADMIN", 3L, 60_000);
        assertThatThrownBy(() -> jwtService.parseToken(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void tokenWithoutRoleOrUserId_isRejected() {
        String noRole = token(SECRET, "user-service", null, 3L, 60_000);
        String noUserId = token(SECRET, "user-service", "ADMIN", null, 60_000);

        assertThatThrownBy(() -> jwtService.parseToken(noRole)).isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> jwtService.parseToken(noUserId)).isInstanceOf(JwtException.class);
    }
}
