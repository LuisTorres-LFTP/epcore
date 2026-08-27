package com.epcore.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(
            "this-is-a-development-secret-key-with-more-than-32-chars",
            3_600_000
    );

    @Test
    void shouldGenerateAndValidateToken() {
        UserDetails user = User.withUsername("admin@epcore.com")
                .password("ignored")
                .roles("ADMIN")
                .build();

        String token = jwtService.generateToken(user);

        assertNotNull(token);
        assertEquals("admin@epcore.com", jwtService.extractUsername(token));
        assertTrue(jwtService.isTokenValid(token, user));
    }

    @Test
    void shouldRejectTokenForAnotherUser() {
        UserDetails owner = User.withUsername("admin@epcore.com")
                .password("ignored")
                .roles("ADMIN")
                .build();
        UserDetails otherUser = User.withUsername("other@epcore.com")
                .password("ignored")
                .roles("ADMIN")
                .build();

        String token = jwtService.generateToken(owner);

        assertFalse(jwtService.isTokenValid(token, otherUser));
    }
}
