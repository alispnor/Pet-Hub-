package com.alispnor.pethub.identity.application.dto;

import com.alispnor.pethub.identity.domain.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record RegisterAdminRequest(
        @NotBlank @Size(max = 200) String nome,
        @NotBlank @Email @Size(max = 200) String email,
        @NotBlank @Size(min = 8, max = 100) String senha,
        @NotEmpty(message = "Pelo menos uma role admin é obrigatória") Set<Role> roles
) {
}
