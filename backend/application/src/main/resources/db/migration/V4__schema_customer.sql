-- ============================================================
-- V4 — Schema do módulo customer (Fase 2)
-- ============================================================
-- Tabela inicial perfil_cliente. As demais (pets, enderecos,
-- formas_pagamento) entram em migrations subsequentes conforme
-- cada entidade é implementada.

CREATE TABLE perfil_cliente (
    id                   BIGSERIAL    PRIMARY KEY,
    usuario_id           BIGINT       NOT NULL UNIQUE REFERENCES usuarios(id) ON DELETE CASCADE,
    cpf_criptografado    VARCHAR(255),
    data_nascimento      DATE,
    genero               VARCHAR(20),
    telefone_adicional   VARCHAR(20),
    aceite_termos        BOOLEAN      NOT NULL DEFAULT FALSE,
    aceite_termos_em     TIMESTAMP,
    aceite_marketing     BOOLEAN      NOT NULL DEFAULT FALSE,
    criado_em            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_perfil_cliente_genero
        CHECK (genero IS NULL OR genero IN ('MASCULINO','FEMININO','NAO_INFORMADO','OUTRO'))
);

CREATE INDEX idx_perfil_cliente_usuario ON perfil_cliente(usuario_id);
