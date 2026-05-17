package com.alispnor.pethub.catalog.application.dto;

import com.alispnor.pethub.catalog.domain.entity.Origem;
import com.alispnor.pethub.common.validation.ValidNcm;
import com.alispnor.pethub.common.validation.ValidVideoEmbedUrl;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Map;

public record ProdutoRequest(
        @NotBlank @Size(max = 50) String sku,
        @NotBlank @Size(max = 200) String nome,
        @Size(max = 500) String descricaoCurta,
        String descricaoCompleta,
        @Size(max = 100) String marca,
        @NotNull Long categoriaId,
        @NotNull @DecimalMin(value = "0.001", message = "Peso deve ser maior que zero") BigDecimal pesoKg,
        @DecimalMin(value = "0.0") BigDecimal alturaCm,
        @DecimalMin(value = "0.0") BigDecimal larguraCm,
        @DecimalMin(value = "0.0") BigDecimal profundidadeCm,
        @NotBlank @ValidNcm String ncm,
        @NotNull Origem origem,
        Map<String, Object> specs,
        boolean destacado,
        @NotNull @DecimalMin(value = "0.01", message = "Preço inicial deve ser maior que zero") BigDecimal precoInicial,
        @ValidVideoEmbedUrl @Size(max = 500) String videoUrl
) {
}
