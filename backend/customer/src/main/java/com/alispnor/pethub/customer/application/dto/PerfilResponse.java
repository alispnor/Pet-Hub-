package com.alispnor.pethub.customer.application.dto;

import com.alispnor.pethub.customer.domain.entity.Genero;

import java.time.LocalDate;

public record PerfilResponse(
        Long id,
        String nome,
        String email,
        String cpfMascarado,
        LocalDate dataNascimento,
        Genero genero,
        String telefoneAdicional,
        boolean aceiteTermos,
        boolean aceiteMarketing
) {
}
