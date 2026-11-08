package com.epcore.auth;

import com.epcore.security.LoginAttemptService;
import org.springframework.security.authentication.BadCredentialsException;

import com.epcore.config.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttemptService;

    public AuthService(AuthenticationManager authenticationManager, JwtService jwtService,
                       LoginAttemptService loginAttemptService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.loginAttemptService = loginAttemptService;
    }

    public LoginResponse login(LoginRequest request) {
        if (request == null || request.correo() == null || request.correo().isBlank()
                || request.password() == null || request.password().isBlank()) {
            throw new IllegalArgumentException("Correo y contraseña son obligatorios");
        }

        String correo = request.correo().trim();
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(correo, request.password())
            );
        } catch (BadCredentialsException ex) {
            loginAttemptService.registrarFallo(correo);
            throw ex;
        }
        loginAttemptService.registrarExito(correo);

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtService.generateToken(userDetails);
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return new LoginResponse(
                token,
                "Bearer",
                jwtService.getExpirationMs(),
                userDetails.getUsername(),
                roles
        );
    }
}
