package com.alispnor.pethub.identity.application.dto;

import com.alispnor.pethub.identity.domain.entity.Role;
import com.alispnor.pethub.identity.domain.entity.TipoUsuario;

import java.util.Set;

public record UsuarioResponse(
        Long id,
        String nome,
        String email,
        TipoUsuario tipo,
        Set<Role> roles,
        boolean ativo
) {
}
