package com.alispnor.pethub.order.application.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record CriarPedidoRequest(
        Long clienteId,
        List<PedidoItemSnapshot> itens,
        Map<String, Object> enderecoEntregaSnapshot,
        Map<String, Object> enderecoCobrancaSnapshot,
        Map<String, Object> opcaoFreteSnapshot,
        String cupomCodigo,
        BigDecimal valorSubtotal,
        BigDecimal valorDescontos,
        BigDecimal valorImpostos,
        BigDecimal valorFrete,
        BigDecimal valorTotal,
        String formaPagamentoTipo,
        String formaPagamentoUltimos4,
        String formaPagamentoBandeira
) {
}
