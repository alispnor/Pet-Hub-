package com.alispnor.pethub.identity.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Payload de login. {@code manterConectado} é opcional (default {@code false})
 * e, quando {@code true}, faz o refresh token receber TTL estendido
 * (default 30 dias em vez dos 7 padrão) — comportamento clássico de
 * "Manter conectado".
 *
 * <p>O access token NUNCA é afetado pela flag: continua com TTL curto
 * (15min). A flag muda apenas a janela em que o refresh transparente
 * fica valendo sem novo login.
 */
public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String senha,
        Boolean manterConectado
) {
    /** {@code manterConectado} é {@link Boolean}, então default explícito quando nulo. */
    public boolean manterConectadoOrDefault() {
        return Boolean.TRUE.equals(manterConectado);
    }
}
