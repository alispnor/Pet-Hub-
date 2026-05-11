package com.alispnor.pethub.catalog.application.dto;

import com.alispnor.pethub.catalog.domain.entity.Origem;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ProdutoDetailResponse(
        Long id,
        String sku,
        String nome,
        String descricaoCurta,
        String descricaoCompleta,
        String marca,
        String ncm,
        Origem origem,
        BigDecimal preco,
        BigDecimal pesoKg,
        BigDecimal alturaCm,
        BigDecimal larguraCm,
        BigDecimal profundidadeCm,
        Map<String, Object> specs,
        List<ProdutoImagemResponse> imagens,
        CategoriaResponse categoria,
        boolean ativo,
        boolean destacado
) {
}
