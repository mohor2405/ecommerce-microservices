package com.ecommerce.userservice.security;

import com.ecommerce.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Tells Spring Security how to load a user by "username" (our email) from MySQL.
 * Used by (1) the AuthenticationManager during login and (2) the JWT filter on every request.
 * Because a bean of this type exists, Spring Boot no longer creates its default in-memory user.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .map(UserPrincipal::from)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }
}
