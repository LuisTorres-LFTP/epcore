package com.epcore.auth;

import com.epcore.config.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    @Test
    void shouldAuthenticateAndReturnJwt() {
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        JwtService jwtService = new JwtService(
                "this-is-a-development-secret-key-with-more-than-32-chars",
                3_600_000
        );

        UserDetails user = User.withUsername("admin@epcore.com")
                .password("ignored")
                .roles("ADMIN")
                .build();
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user,
                null,
                user.getAuthorities()
        );

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        AuthService service = new AuthService(authenticationManager, jwtService);
        LoginResponse response = service.login(new LoginRequest("admin@epcore.com", "secret"));

        assertNotNull(response.token());
        assertEquals("Bearer", response.tokenType());
        assertEquals("admin@epcore.com", response.correo());
        assertEquals(List.of("ROLE_ADMIN"), response.roles());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }
}
