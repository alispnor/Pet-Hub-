package com.alispnor.pethub.identity.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configura o cookie HttpOnly que carrega o refresh token. O cliente nunca
 * lê o cookie diretamente (HttpOnly + path estreito): o browser anexa
 * automaticamente em chamadas para {@code /api/v1/auth/refresh} e
 * {@code /api/v1/auth/logout}.
 *
 * @param name     nome do cookie (default {@code pethub_refresh})
 * @param path     escopo do cookie (default {@code /api/v1/auth} — apenas auth)
 * @param sameSite {@code Strict|Lax|None} (default {@code Lax} — permite redirects de mesma origem)
 * @param secure   se {@code true} adiciona flag Secure (obrigatório em prod com HTTPS)
 * @param domain   opcional; quando ausente o cookie fica restrito ao host emissor
 */
@ConfigurationProperties("app.auth.refresh-cookie")
public record RefreshCookieProperties(
        String name,
        String path,
        String sameSite,
        boolean secure,
        String domain
) {
    public RefreshCookieProperties {
        if (name == null || name.isBlank()) {
            name = "pethub_refresh";
        }
        if (path == null || path.isBlank()) {
            path = "/api/v1/auth";
        }
        if (sameSite == null || sameSite.isBlank()) {
            sameSite = "Lax";
        }
    }
}
