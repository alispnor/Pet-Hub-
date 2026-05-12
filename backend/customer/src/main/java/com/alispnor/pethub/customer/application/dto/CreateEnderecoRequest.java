package com.alispnor.pethub.customer.application.dto;

import com.alispnor.pethub.customer.domain.entity.TipoEndereco;
import com.alispnor.pethub.customer.domain.entity.UnidadeFederativa;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateEnderecoRequest(
        @NotBlank @Size(max = 50) String apelido,
        @NotBlank @Pattern(regexp = "\\d{8}", message = "CEP deve ter 8 dígitos numéricos (sem máscara)") String cep,
        @NotBlank @Size(max = 200) String logradouro,
        @Size(max = 20) String numero,
        @Size(max = 100) String complemento,
        @NotBlank @Size(max = 100) String bairro,
        @NotBlank @Size(max = 100) String cidade,
        @NotNull UnidadeFederativa uf,
        @NotNull TipoEndereco tipo,
        Boolean padraoEntrega,
        Boolean padraoCobranca
) {
}
