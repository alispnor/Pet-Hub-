package com.alispnor.pethub.order.application.dto;

import com.alispnor.pethub.order.domain.entity.StatusPedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Versão enxuta usada em listagens. Sem itens, sem snapshots — só o resumo.
 */
public record PedidoResumoResponse(
        Long id,
        String numeroPedido,
        StatusPedido status,
        BigDecimal valorTotal,
        String formaPagamentoTipo,
        int totalItens,
        LocalDateTime criadoEm
) {
}
