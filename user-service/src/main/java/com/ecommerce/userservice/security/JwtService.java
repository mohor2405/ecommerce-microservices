package com.ecommerce.userservice.security;

import com.ecommerce.userservice.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

/**
 * Creates and validates JSON Web Tokens (HS256).
 *
 * <p>Token payload: {@code sub} = email, plus {@code userId} and {@code role} claims.
 * Including userId/role means future services (Order, Payment...) or an API Gateway can
 * identify the caller just by validating the token with the shared secret, with no call back
 * to this service.
 */
@Service
public class JwtService {

    static final String CLAIM_USER_ID = "userId";
    static final String CLAIM_ROLE = "role";
    private static final String ISSUER = "user-service";

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(JwtProperties properties) {
        // hmacShaKeyFor throws WeakKeyException if the key is shorter than 256 bits -> fail fast at startup.
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secret()));
        this.expirationMs = properties.expirationMs();
    }

    public String generateToken(UserPrincipal principal) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(principal.getUsername())
                .claim(CLAIM_USER_ID, principal.getId())
                .claim(CLAIM_ROLE, principal.getRole().name())
                .issuer(ISSUER)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Verifies the signature and expiry, then returns the subject (email).
     *
     * @throws JwtException if the token is malformed, tampered with or expired
     */
    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public long getExpirationSeconds() {
        return expirationMs / 1000;
    }

    Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(ISSUER)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
