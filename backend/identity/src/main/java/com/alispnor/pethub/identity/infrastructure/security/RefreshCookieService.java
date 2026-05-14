package com.alispnor.pethub.identity.infrastructure.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Set / clear / extract do cookie de refresh token. Mantém o token fora do
 * alcance do JavaScript (HttpOnly) e com escopo de path estreito para mitigar
 * vazamento por XSS ou rota equivocada.
 *
 * <p>Headers SameSite/Secure são montados manualmente porque a API
 * {@link Cookie} antiga só ganhou suporte a SameSite no Servlet 6 — mais
 * portável escrever o {@code Set-Cookie} direto.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(RefreshCookieProperties.class)
public class RefreshCookieService {

    private final RefreshCookieProperties properties;

    public void setCookie(HttpServletResponse response, String value, Duration maxAge) {
        log.debug("RefreshCookieService.setCookie — name={}, path={}, maxAge={}, secure={}, sameSite={}",
                properties.name(), properties.path(), maxAge, properties.secure(), properties.sameSite());
        var sb = new StringBuilder()
                .append(properties.name()).append('=').append(value).append("; ")
                .append("Path=").append(properties.path()).append("; ")
                .append("Max-Age=").append(maxAge.toSeconds()).append("; ")
                .append("HttpOnly; ")
                .append("SameSite=").append(properties.sameSite());
        if (properties.secure()) {
            sb.append("; Secure");
        }
        if (properties.domain() != null && !properties.domain().isBlank()) {
            sb.append("; Domain=").append(properties.domain());
        }
        response.addHeader("Set-Cookie", sb.toString());
    }

    public void clearCookie(HttpServletResponse response) {
        log.debug("RefreshCookieService.clearCookie — name={}, path={}", properties.name(), properties.path());
        var sb = new StringBuilder()
                .append(properties.name()).append("=; ")
                .append("Path=").append(properties.path()).append("; ")
                .append("Max-Age=0; ")
                .append("HttpOnly; ")
                .append("SameSite=").append(properties.sameSite());
        if (properties.secure()) {
            sb.append("; Secure");
        }
        if (properties.domain() != null && !properties.domain().isBlank()) {
            sb.append("; Domain=").append(properties.domain());
        }
        response.addHeader("Set-Cookie", sb.toString());
    }

    public Optional<String> extract(HttpServletRequest request) {
        if (request == null || request.getCookies() == null) {
            return Optional.empty();
        }
        for (var c : request.getCookies()) {
            if (properties.name().equals(c.getName())) {
                var v = c.getValue();
                if (v != null && !v.isBlank()) {
                    return Optional.of(v);
                }
            }
        }
        return Optional.empty();
    }

    public String cookieName() {
        return properties.name();
    }
}
