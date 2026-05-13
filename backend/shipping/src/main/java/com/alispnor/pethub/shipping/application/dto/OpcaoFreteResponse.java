package com.alispnor.pethub.shipping.application.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public record OpcaoFreteResponse(
        String codigo,
        String transportadora,
        String servico,
        BigDecimal valor,
        int prazoDias
) implements Serializable {
}
