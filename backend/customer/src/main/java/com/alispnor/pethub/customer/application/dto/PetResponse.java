package com.alispnor.pethub.customer.application.dto;

import com.alispnor.pethub.customer.domain.entity.Especie;
import com.alispnor.pethub.customer.domain.entity.Porte;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PetResponse(
        Long id,
        String nome,
        Especie especie,
        String raca,
        LocalDate dataNascimento,
        BigDecimal pesoKg,
        Porte porte,
        String observacoes,
        String fotoUrl
) {
}
