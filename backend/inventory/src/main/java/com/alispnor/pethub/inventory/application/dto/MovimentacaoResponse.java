package com.alispnor.pethub.inventory.application.dto;

import com.alispnor.pethub.inventory.domain.entity.TipoMovimentacao;

import java.time.LocalDateTime;

public record MovimentacaoResponse(
        Long id,
        String sku,
        TipoMovimentacao tipo,
        int quantidade,
        String motivo,
        Long pedidoId,
        String referenciaPedido,
        Long criadoPorId,
        LocalDateTime criadoEm
) {
}
