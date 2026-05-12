package com.alispnor.pethub.customer.application.dto;

import com.alispnor.pethub.customer.domain.entity.TipoEndereco;
import com.alispnor.pethub.customer.domain.entity.UnidadeFederativa;

public record EnderecoResponse(
        Long id,
        String apelido,
        String cep,
        String logradouro,
        String numero,
        String complemento,
        String bairro,
        String cidade,
        UnidadeFederativa uf,
        String pais,
        TipoEndereco tipo,
        boolean padraoEntrega,
        boolean padraoCobranca,
        boolean ativo
) {
}
