package com.alispnor.pethub.order.application.dto;

import java.math.BigDecimal;

/**
 * Dados que o checkout passa para o order-service para materializar cada
 * PedidoItem. Tudo é snapshot — desacopla o pedido de mudanças futuras no
 * produto.
 */
public record PedidoItemSnapshot(
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
