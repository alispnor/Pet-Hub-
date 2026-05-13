package com.alispnor.pethub.customer.domain.entity;

public enum TipoPagamento {
    CARTAO_CREDITO,
    CARTAO_DEBITO,
    PIX,
    BOLETO;

    public boolean exigeCartao() {
        return this == CARTAO_CREDITO || this == CARTAO_DEBITO;
    }
}
