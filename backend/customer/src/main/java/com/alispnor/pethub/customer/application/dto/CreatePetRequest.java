package com.alispnor.pethub.customer.application.dto;

import com.alispnor.pethub.customer.domain.entity.Especie;
import com.alispnor.pethub.customer.domain.entity.Porte;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreatePetRequest(
        @NotBlank @Size(max = 100) String nome,
        @NotNull Especie especie,
        @Size(max = 100) String raca,
        @PastOrPresent LocalDate dataNascimento,
        @DecimalMin("0.01") @DecimalMax("999.99") BigDecimal pesoKg,
        Porte porte,
        @Size(max = 2000) String observacoes
) {
}
