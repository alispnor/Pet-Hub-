package com.alispnor.pethub.common.storage;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.uploads")
public record PhotoStorageProperties(
        @NotBlank String dir,
        @NotBlank String publicBaseUrl,
        long maxBytes
) {
    public PhotoStorageProperties {
        if (maxBytes <= 0) {
            maxBytes = 5L * 1024 * 1024;
        }
    }
}
