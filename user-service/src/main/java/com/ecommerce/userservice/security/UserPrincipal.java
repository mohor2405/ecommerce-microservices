package com.ecommerce.userservice.security;

import com.ecommerce.userservice.entity.Role;
import com.ecommerce.userservice.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Adapter between our {@link User} entity and Spring Security's {@link UserDetails}
 * (Adapter design pattern). Keeping it separate means the JPA entity does not depend on
 * Spring Security classes.
 *
 * <p>It is what controllers receive through {@code @AuthenticationPrincipal}, which gives
 * them the logged-in user's id without another DB lookup.
 */
@Getter
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final Role role;

    private UserPrincipal(Long id, String email, String password, Role role) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    public static UserPrincipal from(User user) {
        return new UserPrincipal(user.getId(), user.getEmail(), user.getPassword(), user.getRole());
    }

    /** Spring Security convention: roles are authorities prefixed with "ROLE_". */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    /** Our "username" is the email address. */
    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }
}
