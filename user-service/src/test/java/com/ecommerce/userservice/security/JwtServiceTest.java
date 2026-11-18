package com.ecommerce.userservice.security;

import com.ecommerce.userservice.config.JwtProperties;
import com.ecommerce.userservice.entity.Role;
import com.ecommerce.userservice.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    // 32+ random bytes, Base64-encoded (test-only key)
    private static final String SECRET = "dGVzdC1zZWNyZXQta2V5LWZvci11bml0LXRlc3RzLW9ubHktMzJieXRlcyE=";
    private static final String OTHER_SECRET = "YW5vdGhlci1zZWNyZXQta2V5LWZvci11bml0LXRlc3RzLTMyYnl0ZXMhIQ==";

    private final UserPrincipal principal = UserPrincipal.from(
            User.builder().id(7L).email("john@example.com").password("x").role(Role.ADMIN).build());

    @Test
    void generatedToken_containsSubjectUserIdAndRole() {
        JwtService jwtService = new JwtService(new JwtProperties(SECRET, 60_000));

        String token = jwtService.generateToken(principal);
        Claims claims = jwtService.parseClaims(token);

        assertThat(jwtService.extractUsername(token)).isEqualTo("john@example.com");
        assertThat(claims.get(JwtService.CLAIM_USER_ID, Long.class)).isEqualTo(7L);
        assertThat(claims.get(JwtService.CLAIM_ROLE, String.class)).isEqualTo("ADMIN");
        assertThat(jwtService.getExpirationSeconds()).isEqualTo(60);
    }

    @Test
    void expiredToken_isRejected() {
        JwtService jwtService = new JwtService(new JwtProperties(SECRET, -1_000)); // already expired

        String token = jwtService.generateToken(principal);

        assertThatThrownBy(() -> jwtService.extractUsername(token)).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void tokenSignedWithDifferentKey_isRejected() {
        String foreignToken = new JwtService(new JwtProperties(OTHER_SECRET, 60_000)).generateToken(principal);
        JwtService jwtService = new JwtService(new JwtProperties(SECRET, 60_000));

        assertThatThrownBy(() -> jwtService.extractUsername(foreignToken)).isInstanceOf(JwtException.class);
    }

    @Test
    void malformedToken_isRejected() {
        JwtService jwtService = new JwtService(new JwtProperties(SECRET, 60_000));

        assertThatThrownBy(() -> jwtService.extractUsername("not.a.jwt")).isInstanceOf(JwtException.class);
    }
}
