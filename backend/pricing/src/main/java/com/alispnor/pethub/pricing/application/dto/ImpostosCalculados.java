package com.alispnor.pethub.pricing.application.dto;

import java.math.BigDecimal;

public record ImpostosCalculados(
        BigDecimal icms,
        BigDecimal ipi,
        BigDecimal pis,
        BigDecimal cofins,
        BigDecimal total
) {
    public static ImpostosCalculados zero() {
        return new ImpostosCalculados(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }
}
