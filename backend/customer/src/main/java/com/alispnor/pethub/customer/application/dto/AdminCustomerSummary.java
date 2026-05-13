package com.alispnor.pethub.customer.application.dto;

import java.time.LocalDateTime;

public record AdminCustomerSummary(
        Long id,
        String nome,
        String emailMascarado,
        boolean ativo,
        LocalDateTime dataCadastro,
        long totalPets,
        long totalEnderecos
) {
}
