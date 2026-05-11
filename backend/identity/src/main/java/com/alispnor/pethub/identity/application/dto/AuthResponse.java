package com.alispnor.pethub.identity.application.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        int expiresIn,
        UsuarioResponse usuario
) {
}
