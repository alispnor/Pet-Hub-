package com.alispnor.pethub.catalog.application.dto;

import java.math.BigDecimal;

public record ProdutoSummaryResponse(
        Long id,
        String sku,
        String nome,
        String descricaoCurta,
        BigDecimal preco,
        String imagemPrincipal,
        String categoriaSlug,
        boolean destacado
) {
}
