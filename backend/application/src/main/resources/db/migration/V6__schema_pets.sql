-- ============================================================
-- V6 — Tabela pets (Fase 2)
-- ============================================================

CREATE TABLE pets (
    id                 BIGSERIAL    PRIMARY KEY,
    perfil_cliente_id  BIGINT       NOT NULL REFERENCES perfil_cliente(id) ON DELETE CASCADE,
    nome               VARCHAR(100) NOT NULL,
    especie            VARCHAR(20)  NOT NULL,
    raca               VARCHAR(100),
    data_nascimento    DATE,
    peso_kg            NUMERIC(5,2),
    porte              VARCHAR(20),
    observacoes        TEXT,
    foto_url           VARCHAR(500),
    criado_em          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_pets_especie
        CHECK (especie IN ('CACHORRO','GATO','AVE','PEIXE','REPTIL','OUTROS')),
    CONSTRAINT chk_pets_porte
        CHECK (porte IS NULL OR porte IN ('PEQUENO','MEDIO','GRANDE','GIGANTE'))
);

CREATE INDEX idx_pets_perfil ON pets(perfil_cliente_id);
