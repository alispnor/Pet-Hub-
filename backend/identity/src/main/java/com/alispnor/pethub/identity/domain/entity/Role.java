package com.alispnor.pethub.identity.domain.entity;

/**
 * Authorities aplicadas via @PreAuthorize. Os nomes usam o prefixo "ROLE_"
 * exigido pelo Spring Security para integração com hasRole().
 */
public enum Role {
    ROLE_CLIENTE,
    ROLE_ADMIN_LOJA,
    ROLE_GERENTE,
    ROLE_OPERADOR
}
