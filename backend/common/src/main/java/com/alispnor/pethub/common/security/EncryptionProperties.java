package com.alispnor.pethub.common.security;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.encryption")
public record EncryptionProperties(
        @NotBlank String key
) {
}
