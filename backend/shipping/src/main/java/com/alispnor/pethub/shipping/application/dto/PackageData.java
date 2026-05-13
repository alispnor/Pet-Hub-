package com.alispnor.pethub.shipping.application.dto;

import java.math.BigDecimal;

/**
 * Dados agregados do pacote a partir dos itens (peso, dimensões totais).
 */
public record PackageData(
        BigDecimal pesoKgTotal,
        BigDecimal pesoCubicoKg,
        BigDecimal pesoFaturavelKg,
        int quantidadeItens
) {
}
