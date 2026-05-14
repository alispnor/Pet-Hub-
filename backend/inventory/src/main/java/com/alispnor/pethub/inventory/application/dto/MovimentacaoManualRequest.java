package com.alispnor.pethub.inventory.application.dto;

import com.alispnor.pethub.inventory.domain.entity.TipoMovimentacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record MovimentacaoManualRequest(
        @NotBlank @Size(max = 50) String sku,
        @NotNull TipoMovimentacao tipo,
        @Positive int quantidade,
        @Size(max = 200) String motivo
) {
}
