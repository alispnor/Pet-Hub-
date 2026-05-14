package com.alispnor.pethub.order.application.dto;

import com.alispnor.pethub.order.domain.entity.StatusPedido;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TransicaoRequest(
        @NotNull StatusPedido paraStatus,
        @Size(max = 500) String observacao
) {
}
