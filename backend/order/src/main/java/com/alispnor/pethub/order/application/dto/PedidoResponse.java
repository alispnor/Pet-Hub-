package com.alispnor.pethub.order.application.dto;

import com.alispnor.pethub.order.domain.entity.StatusPedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record PedidoResponse(
        Long id,
        String numeroPedido,
        Long clienteId,
        StatusPedido status,
        List<PedidoItemResponse> itens,
        Map<String, Object> enderecoEntrega,
        Map<String, Object> enderecoCobranca,
        Map<String, Object> opcaoFrete,
        String cupomCodigo,
        BigDecimal valorSubtotal,
        BigDecimal valorDescontos,
        BigDecimal valorImpostos,
        BigDecimal valorFrete,
        BigDecimal valorTotal,
        String formaPagamentoTipo,
        String formaPagamentoUltimos4,
        String formaPagamentoBandeira,
        Long tentativaPagamentoId,
        String observacoes,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm
) {
}
