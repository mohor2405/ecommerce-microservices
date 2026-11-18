package com.ecommerce.adminservice.security;

import com.ecommerce.adminservice.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;

/**
 * Verifies JWTs issued by the User Service. This service never creates tokens.
 *
 * <p>Verification checks the signature (shared secret), the expiry date and the issuer, then
 * requires the claims the User Service always puts in: sub (email), userId and role.
 */
@Service
public class JwtService {

    static final String CLAIM_USER_ID = "userId";
    static final String CLAIM_ROLE = "role";

    private final SecretKey signingKey;
    private final String issuer;

    public JwtService(JwtProperties properties) {
        // Throws WeakKeyException at startup if the key is shorter than 256 bits.
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secret()));
        this.issuer = properties.issuer();
    }

    /**
     * @return the caller's identity taken from the token
     * @throws JwtException if the token is malformed, tampered with, expired, from another
     *                      issuer, or missing required claims
     */
    public AuthenticatedUser parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String email = claims.getSubject();
        Long userId = claims.get(CLAIM_USER_ID, Long.class);
        String role = claims.get(CLAIM_ROLE, String.class);

        if (email == null || userId == null || role == null) {
            throw new MalformedJwtException("Token is missing required claims");
        }
        return new AuthenticatedUser(userId, email, role);
    }
}
