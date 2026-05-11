package com.alispnor.pethub.catalog.application.dto;

public record CategoriaResponse(
        Long id,
        String nome,
        String slug,
        String descricao,
        Long categoriaPaiId,
        boolean ativo,
        int ordem
) {
}
