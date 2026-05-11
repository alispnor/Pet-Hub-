package com.alispnor.pethub.catalog.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdicionarImagemRequest(
        @NotBlank @Size(max = 500) String url,
        boolean principal
) {
}
