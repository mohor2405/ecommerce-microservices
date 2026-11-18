package com.ecommerce.adminservice.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.security.Principal;
import java.util.List;

/**
 * The caller's identity, built purely from the JWT claims (no database lookup).
 *
 * <p>Controllers can receive it with {@code @AuthenticationPrincipal AuthenticatedUser user}
 * and use {@code user.userId()} for fields like products.created_by.
 *
 * @param userId id of the user in the User Service (claim "userId")
 * @param email  email of the user (claim "sub")
 * @param role   CUSTOMER or ADMIN (claim "role")
 */
public record AuthenticatedUser(Long userId, String email, String role) implements Principal {

    /** Spring Security convention: roles are authorities prefixed with "ROLE_". */
    public List<GrantedAuthority> authorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    /** Lets Spring's Authentication.getName() return the email. */
    @Override
    public String getName() {
        return email;
    }
}
