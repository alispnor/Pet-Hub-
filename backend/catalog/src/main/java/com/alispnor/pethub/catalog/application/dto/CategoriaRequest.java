package com.alispnor.pethub.catalog.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CategoriaRequest(
        @NotBlank @Size(max = 100) String nome,
        @NotBlank @Size(max = 120)
        @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Slug deve ser kebab-case (a-z, 0-9, hifens)")
        String slug,
        @Size(max = 500) String descricao,
        Long categoriaPaiId,
        @PositiveOrZero int ordem
) {
}
