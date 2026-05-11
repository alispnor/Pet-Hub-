package com.alispnor.pethub.identity.infrastructure.security;

import com.alispnor.pethub.identity.domain.entity.Role;
import com.alispnor.pethub.identity.domain.entity.TipoUsuario;

import java.util.Set;

/**
 * Principal injetado no SecurityContext após validação do JWT.
 * Dados vêm das claims do token; sem hit no banco em requests autenticadas.
 */
public record AuthenticatedUser(
        long id,
        String email,
        TipoUsuario tipo,
        Set<Role> roles
) {

    public boolean hasRole(Role role) {
        return roles.contains(role);
    }
}
