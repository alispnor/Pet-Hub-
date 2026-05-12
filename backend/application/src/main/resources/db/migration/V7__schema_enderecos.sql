-- ============================================================
-- V7 — Tabela enderecos (Fase 2)
-- ============================================================

CREATE TABLE enderecos (
    id                BIGSERIAL    PRIMARY KEY,
    perfil_cliente_id BIGINT       NOT NULL REFERENCES perfil_cliente(id) ON DELETE CASCADE,
    apelido           VARCHAR(50)  NOT NULL,
    cep               VARCHAR(8)   NOT NULL,
    logradouro        VARCHAR(200) NOT NULL,
    numero            VARCHAR(20),
    complemento       VARCHAR(100),
    bairro            VARCHAR(100) NOT NULL,
    cidade            VARCHAR(100) NOT NULL,
    uf                CHAR(2)      NOT NULL,
    pais              VARCHAR(2)   NOT NULL DEFAULT 'BR',
    tipo              VARCHAR(20)  NOT NULL,
    padrao_entrega    BOOLEAN      NOT NULL DEFAULT FALSE,
    padrao_cobranca   BOOLEAN      NOT NULL DEFAULT FALSE,
    ativo             BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_enderecos_tipo CHECK (tipo IN ('RESIDENCIAL','COMERCIAL')),
    CONSTRAINT chk_enderecos_uf CHECK (uf IN (
        'AC','AL','AP','AM','BA','CE','DF','ES','GO','MA','MT','MS','MG',
        'PA','PB','PR','PE','PI','RJ','RN','RS','RO','RR','SC','SP','SE','TO'
    )),
    CONSTRAINT chk_enderecos_cep CHECK (cep ~ '^\d{8}$')
);

CREATE INDEX idx_enderecos_perfil ON enderecos(perfil_cliente_id);

-- Apenas 1 endereço pode ter padrao_entrega=true (e idem para cobrança) por perfil.
CREATE UNIQUE INDEX uq_enderecos_padrao_entrega
    ON enderecos(perfil_cliente_id)
    WHERE padrao_entrega = TRUE;

CREATE UNIQUE INDEX uq_enderecos_padrao_cobranca
    ON enderecos(perfil_cliente_id)
    WHERE padrao_cobranca = TRUE;
