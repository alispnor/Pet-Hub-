package com.alispnor.pethub.catalog.application.dto;

public record ProdutoImagemResponse(
        Long id,
        String url,
        int ordem,
        boolean principal
) {
}
