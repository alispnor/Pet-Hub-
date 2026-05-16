-- V17: Suporte a "Manter conectado" no login.
--
-- O storefront agora oferece checkbox "Manter conectado" no /login. Quando
-- marcado, o backend emite o refresh token com TTL estendido (default 30 dias
-- em vez dos 7 padrão). Como o refresh é opaco (não-JWT), a flag tem que
-- viver na própria linha de refresh_tokens — assim a rotação a cada uso
-- preserva o comportamento de TTL longo até o usuário fazer logout.
--
-- A flag NÃO afeta o access token (continua 15min).

ALTER TABLE refresh_tokens
    ADD COLUMN manter_conectado BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN refresh_tokens.manter_conectado IS
    'TRUE quando o usuário marcou "Manter conectado" no login; preserva TTL estendido nas rotações.';
