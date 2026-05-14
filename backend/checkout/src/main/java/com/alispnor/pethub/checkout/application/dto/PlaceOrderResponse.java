package com.alispnor.pethub.checkout.application.dto;

import com.alispnor.pethub.payment.domain.PaymentGateway;

import java.math.BigDecimal;

public record PlaceOrderResponse(
        Long tentativaPagamentoId,
        String referenciaPedido,
        PaymentGateway.Method metodo,
        PaymentGateway.Status status,
        BigDecimal valorTotal,
        String gatewayTransactionId,
        String qrCode,
        String boletoUrl
) {
}
