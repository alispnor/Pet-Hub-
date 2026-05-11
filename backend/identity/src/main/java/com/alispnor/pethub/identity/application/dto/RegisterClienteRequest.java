package com.alispnor.pethub.identity.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterClienteRequest(
        @NotBlank @Size(max = 200) String nome,
        @NotBlank @Email @Size(max = 200) String email,
        @NotBlank @Size(min = 8, max = 100, message = "Senha deve ter pelo menos 8 caracteres") String senha,
        @Pattern(regexp = "^$|^\\+?[0-9]{10,15}$", message = "Telefone deve ter 10–15 dígitos numéricos") String telefone
) {
}
