package com.epcore.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

@Service
public class JwtService {

    /** Claves que estuvieron publicadas como valor por defecto y no deben usarse nunca. */
    private static final java.util.Set<String> CLAVES_PUBLICAS = java.util.Set.of(
            "change-this-development-secret-key-at-least-32-chars",
            "CAMBIAR_POR_UN_SECRETO_LARGO_Y_ALEATORIO_DE_AL_MENOS_32_CARACTERES");

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms:28800000}") long expirationMs) {
        if (secret == null || secret.isBlank()) {
            // Sin JWT_SECRET se usa una clave aleatoria que nadie conoce. Es segura,
            // pero los tokens dejan de servir al reiniciar: en producción defina JWT_SECRET.
            byte[] aleatoria = new byte[48];
            new java.security.SecureRandom().nextBytes(aleatoria);
            org.slf4j.LoggerFactory.getLogger(JwtService.class).warn(
                    "JWT_SECRET no está definido: se generó una clave aleatoria temporal.");
            this.signingKey = Keys.hmacShaKeyFor(aleatoria);
        } else {
            if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
                throw new IllegalArgumentException("JWT_SECRET debe tener al menos 32 bytes");
            }
            if (CLAVES_PUBLICAS.contains(secret)) {
                throw new IllegalArgumentException(
                        "JWT_SECRET usa una clave de ejemplo publicada en el repositorio; defina una propia.");
            }
            this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        }
        this.expirationMs = expirationMs;
    }

    public String generateToken(UserDetails userDetails) {
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(signingKey)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return resolver.apply(claims);
    }
}
