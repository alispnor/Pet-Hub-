package com.alispnor.pethub.order.domain.entity;

public enum StatusPedido {
    PENDENTE_PAGAMENTO,
    PAGAMENTO_APROVADO,
    PAGAMENTO_REJEITADO,
    SEPARACAO,
    EM_TRANSPORTE,
    ENTREGUE,
    CANCELADO,
    DEVOLVIDO
}
