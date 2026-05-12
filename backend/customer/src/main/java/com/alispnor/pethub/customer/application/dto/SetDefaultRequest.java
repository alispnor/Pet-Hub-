package com.alispnor.pethub.customer.application.dto;

import jakarta.validation.constraints.AssertTrue;

public record SetDefaultRequest(
        Boolean padraoEntrega,
        Boolean padraoCobranca
) {
    @AssertTrue(message = "Informe ao menos um campo (padraoEntrega ou padraoCobranca) como true")
    public boolean isAoMenosUm() {
        return Boolean.TRUE.equals(padraoEntrega) || Boolean.TRUE.equals(padraoCobranca);
    }
}
