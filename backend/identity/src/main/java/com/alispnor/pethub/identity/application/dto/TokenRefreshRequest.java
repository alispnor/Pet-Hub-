package com.alispnor.pethub.identity.application.dto;

/**
 * Refresh / logout aceitam o token vindo do cookie HttpOnly {@code pethub_refresh}
 * (browser anexa automaticamente) OU como fallback do body — útil para clientes
 * server-to-server (Postman, integrações) que não suportam cookies.
 *
 * <p>Quando o cookie está presente, ele tem prioridade. Se nenhum dos dois for
 * informado, o controller responde 400.
 */
public record TokenRefreshRequest(
        String refreshToken
) {
}
