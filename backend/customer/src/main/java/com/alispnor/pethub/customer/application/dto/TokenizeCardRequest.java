package com.alispnor.pethub.customer.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TokenizeCardRequest(
        @NotBlank @Pattern(regexp = "\\d{13,19}", message = "Número deve ter 13–19 dígitos numéricos") String numero,
        @NotBlank @Pattern(regexp = "\\d{3,4}", message = "CVV deve ter 3 ou 4 dígitos") String cvv,
        @NotBlank @Size(max = 100) String nomeImpresso,
        @Min(1) @Max(12) int validadeMes,
        @Min(2024) @Max(2100) int validadeAno
) {
}
