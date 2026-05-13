package com.alispnor.pethub.customer.application.dto;

import com.alispnor.pethub.customer.domain.entity.Genero;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdminCustomerDetail(
        Long id,
        String nome,
        String emailMascarado,
        String telefoneMascarado,
        boolean ativo,
        LocalDateTime dataCadastro,
        LocalDateTime ultimoLogin,
        LocalDate dataNascimento,
        Genero genero,
        String cpfMascarado,
        boolean aceiteTermos,
        boolean aceiteMarketing,
        long totalPets,
        long totalEnderecos,
        long totalFormasPagamento
) {
}
