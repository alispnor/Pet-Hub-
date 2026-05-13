package com.alispnor.pethub.customer.application.dto;

import com.alispnor.pethub.customer.domain.entity.Bandeira;
import com.alispnor.pethub.customer.domain.entity.TipoPagamento;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateFormaPagamentoRequest(
        @NotNull TipoPagamento tipo,
        @Size(max = 50) String apelido,
        @Size(max = 100) String gatewayToken,
        Bandeira bandeira,
        @Pattern(regexp = "\\d{4}", message = "Últimos 4 dígitos devem ser numéricos") String ultimosQuatroDigitos,
        @Size(max = 100) String nomeImpresso,
        @Min(1) @Max(12) Integer validadeMes,
        @Min(2024) @Max(2100) Integer validadeAno,
        Boolean padrao
) {
}
