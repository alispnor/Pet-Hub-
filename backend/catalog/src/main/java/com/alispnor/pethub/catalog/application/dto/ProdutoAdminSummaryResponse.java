package com.alispnor.pethub.catalog.application.dto;

import java.math.BigDecimal;

public record ProdutoAdminSummaryResponse(
        Long id,
        String sku,
        String nome,
        String marca,
        BigDecimal preco,
        Long categoriaId,
        String categoriaNome,
        String categoriaSlug,
        String imagemPrincipal,
        int totalImagens,
        boolean destacado,
        boolean ativo,
        boolean temVideo
) {
}
