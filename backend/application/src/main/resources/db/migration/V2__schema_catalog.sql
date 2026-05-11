-- ============================================================
-- V2 — Schema do módulo catalog
-- ============================================================

CREATE TABLE categorias (
    id                  BIGSERIAL PRIMARY KEY,
    nome                VARCHAR(100) NOT NULL,
    slug                VARCHAR(120) NOT NULL UNIQUE,
    descricao           VARCHAR(500),
    categoria_pai_id    BIGINT REFERENCES categorias(id),
    ativo               BOOLEAN      NOT NULL DEFAULT TRUE,
    ordem               INT          NOT NULL DEFAULT 0,
    criado_em           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_categorias_slug ON categorias(slug);
CREATE INDEX idx_categorias_pai  ON categorias(categoria_pai_id);

CREATE TABLE produtos (
    id                  BIGSERIAL PRIMARY KEY,
    sku                 VARCHAR(50)  NOT NULL UNIQUE,
    nome                VARCHAR(200) NOT NULL,
    descricao_curta     VARCHAR(500),
    descricao_completa  TEXT,
    marca               VARCHAR(100),
    categoria_id        BIGINT       NOT NULL REFERENCES categorias(id),
    peso_kg             NUMERIC(8,3) NOT NULL,
    altura_cm           NUMERIC(8,2),
    largura_cm          NUMERIC(8,2),
    profundidade_cm     NUMERIC(8,2),
    ncm                 VARCHAR(8)   NOT NULL,
    origem              VARCHAR(30)  NOT NULL,
    specs               JSONB        NOT NULL DEFAULT '{}'::jsonb,
    ativo               BOOLEAN      NOT NULL DEFAULT TRUE,
    destacado           BOOLEAN      NOT NULL DEFAULT FALSE,
    criado_em           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_produtos_origem
        CHECK (origem IN ('NACIONAL','IMPORTADO_DIRETO','IMPORTADO_INDIRETO')),
    CONSTRAINT chk_produtos_ncm CHECK (ncm ~ '^[0-9]{8}$')
);

CREATE INDEX idx_produtos_categoria  ON produtos(categoria_id);
CREATE INDEX idx_produtos_ativo      ON produtos(ativo);
CREATE INDEX idx_produtos_destacado  ON produtos(destacado);
CREATE INDEX idx_produtos_nome_trgm  ON produtos USING gin (LOWER(nome) gin_trgm_ops);

CREATE TABLE produto_imagens (
    id          BIGSERIAL PRIMARY KEY,
    produto_id  BIGINT       NOT NULL REFERENCES produtos(id) ON DELETE CASCADE,
    url         VARCHAR(500) NOT NULL,
    ordem       INT          NOT NULL DEFAULT 0,
    principal   BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_produto_imagens_produto ON produto_imagens(produto_id);

CREATE TABLE precos_vigentes (
    id              BIGSERIAL PRIMARY KEY,
    produto_id      BIGINT        NOT NULL REFERENCES produtos(id),
    valor_base      NUMERIC(10,2) NOT NULL,
    data_inicio     TIMESTAMP     NOT NULL,
    data_fim        TIMESTAMP,
    criado_por_id   BIGINT        REFERENCES usuarios(id),
    criado_em       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_precos_positivo CHECK (valor_base > 0)
);

CREATE INDEX idx_precos_vigentes_produto         ON precos_vigentes(produto_id);
CREATE INDEX idx_precos_vigentes_produto_vigente ON precos_vigentes(produto_id) WHERE data_fim IS NULL;
