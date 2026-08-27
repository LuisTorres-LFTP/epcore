package com.epcore.auth;

import java.util.List;

public record LoginResponse(
        String token,
        String tokenType,
        long expiresIn,
        String correo,
        List<String> roles
) {
}
