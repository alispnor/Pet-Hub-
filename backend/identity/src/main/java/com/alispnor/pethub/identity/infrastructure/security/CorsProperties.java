package com.alispnor.pethub.identity.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties("app.security.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
