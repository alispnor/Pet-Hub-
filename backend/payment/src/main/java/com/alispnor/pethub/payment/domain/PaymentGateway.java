package com.alispnor.pethub.payment.domain;

import java.math.BigDecimal;

/**
 * Abstração de gateway de pagamento. Cobre o ciclo completo: tokenização do
 * cartão, charge síncrono, refund e consulta de status. Implementações concretas
 * (Mock, Mercado Pago, Stripe, etc.) ficam em `infrastructure`.
 */
public interface PaymentGateway {

    // -- Tokenização (já usada pelo módulo customer no cadastro de cartão) --

    record CardData(String numero, String cvv, String nomeImpresso, int validadeMes, int validadeAno) {
    }

    record TokenizationResult(String token, Brand brand, String ultimosQuatroDigitos) {
    }

    enum Brand { VISA, MASTER, AMEX, ELO, HIPERCARD, OUTRO }

    TokenizationResult tokenizeCard(CardData card);

    // -- Charge / Refund / Status (Fase 3 — checkout) --

    enum Method { CARTAO_CREDITO, CARTAO_DEBITO, PIX, BOLETO }

    enum Status { PROCESSING, APPROVED, REJECTED, REFUNDED }

    /**
     * @param gatewayToken obrigatório para CARTAO_*; null para PIX/BOLETO.
     * @param parcelas    1..N (cartão de crédito); ignorado nos demais.
     * @param idempotencyKey identificador estável da intenção; mesma chave → mesma resposta.
     */
    record ChargeRequest(
            String referenciaPedido,
            BigDecimal valor,
            Method method,
            String gatewayToken,
            Integer parcelas,
            String idempotencyKey
    ) {
    }

    /**
     * @param qrCode      payload PIX (somente quando method=PIX).
     * @param boletoUrl   url do PDF do boleto (somente quando method=BOLETO).
     * @param rawResponse JSON cru retornado pelo gateway; persistido para auditoria.
     */
    record PaymentResult(
            String transactionId,
            Status status,
            Method method,
            BigDecimal valor,
            String qrCode,
            String boletoUrl,
            String rawResponse
    ) {
    }

    record RefundResult(String refundId, Status status, BigDecimal valorEstornado) {
    }

    PaymentResult charge(ChargeRequest request);

    RefundResult refund(String transactionId, BigDecimal valor);

    Status getStatus(String transactionId);
}
