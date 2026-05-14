package com.alispnor.pethub.order.application.dto;

import java.math.BigDecimal;

public record PedidoItemResponse(
        Long id,
        Long produtoId,
        String sku,
        String nomeProduto,
        String imagemUrl,
        int qty,
        BigDecimal precoUnitario,
        BigDecimal descontoUnitario,
        BigDecimal precoFinal,
        String ncm
) {
}
