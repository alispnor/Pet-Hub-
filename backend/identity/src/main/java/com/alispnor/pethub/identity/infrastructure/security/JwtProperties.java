package com.alispnor.pethub.identity.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.security.jwt")
public record JwtProperties(
        String secret,
        int accessTokenTtlMinutes,
        int refreshTokenTtlDays,
        int refreshTokenTtlExtendedDays
) {
    public JwtProperties {
        if (refreshTokenTtlExtendedDays <= 0) {
            refreshTokenTtlExtendedDays = 30;
        }
        if (refreshTokenTtlExtendedDays < refreshTokenTtlDays) {
            throw new IllegalStateException(
                    "refreshTokenTtlExtendedDays (" + refreshTokenTtlExtendedDays
                    + ") deve ser maior ou igual a refreshTokenTtlDays (" + refreshTokenTtlDays + ")");
        }
    }
}
