package com.alispnor.pethub.customer.application.dto;

import com.alispnor.pethub.customer.domain.entity.Genero;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdatePerfilRequest(
        @Past LocalDate dataNascimento,
        Genero genero,
        @Pattern(regexp = "^\\d{10,11}$", message = "Telefone deve ter 10 ou 11 dígitos (apenas números)")
        @Size(max = 20)
        String telefoneAdicional,
        Boolean aceiteTermos,
        Boolean aceiteMarketing
) {
}
