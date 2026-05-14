package com.alispnor.pethub.checkout.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PlaceOrderRequest(
        @NotNull Long enderecoEntregaId,
        @NotNull Long enderecoCobrancaId,
        @NotBlank String opcaoFreteCodigo,
        @NotNull Long formaPagamentoId,
        String cupom,
        @Min(1) @Max(12) Integer parcelas,
        @Size(max = 100) String idempotencyKey
) {
}
