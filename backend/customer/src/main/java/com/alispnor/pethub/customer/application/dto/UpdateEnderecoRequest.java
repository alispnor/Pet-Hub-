package com.alispnor.pethub.customer.application.dto;

import com.alispnor.pethub.customer.domain.entity.TipoEndereco;
import com.alispnor.pethub.customer.domain.entity.UnidadeFederativa;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateEnderecoRequest(
        @Size(max = 50) String apelido,
        @Pattern(regexp = "\\d{8}", message = "CEP deve ter 8 dígitos numéricos (sem máscara)") String cep,
        @Size(max = 200) String logradouro,
        @Size(max = 20) String numero,
        @Size(max = 100) String complemento,
        @Size(max = 100) String bairro,
        @Size(max = 100) String cidade,
        UnidadeFederativa uf,
        TipoEndereco tipo,
        Boolean ativo
) {
}
