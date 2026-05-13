package com.alispnor.pethub.customer.application.dto;

import com.alispnor.pethub.customer.domain.entity.Bandeira;
import com.alispnor.pethub.customer.domain.entity.TipoPagamento;

public record FormaPagamentoResponse(
        Long id,
        TipoPagamento tipo,
        String apelido,
        Bandeira bandeira,
        String ultimosQuatroDigitos,
        String nomeImpresso,
        Integer validadeMes,
        Integer validadeAno,
        boolean padrao,
        boolean ativo
) {
}
