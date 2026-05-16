package com.alispnor.pethub.identity.application.dto;

/**
 * Resultado interno da emissão de tokens (login + refresh).
 *
 * <p>Existe para que o controller saiba qual {@code Max-Age} aplicar no
 * cookie HttpOnly sem precisar fazer uma query extra para inferir do
 * {@code expira_em}: o service emite, sabe a flag e propaga aqui.
 *
 * <p>Este record NÃO é serializado no body de resposta — o controller
 * usa o campo {@link #response()} no body e mantém {@link #manterConectado()}
 * apenas em memória para a decisão do cookie.
 */
public record AuthEmissionResult(
        AuthResponse response,
        boolean manterConectado
) {
}
