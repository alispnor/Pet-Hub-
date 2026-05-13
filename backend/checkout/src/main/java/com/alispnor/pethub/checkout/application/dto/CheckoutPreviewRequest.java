package com.alispnor.pethub.checkout.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CheckoutPreviewRequest(
        @NotNull Long enderecoEntregaId,
        @NotBlank String opcaoFreteCodigo,
        String cupom
) {
}
