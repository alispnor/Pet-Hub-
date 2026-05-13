-- ============================================================
-- V11 — Schema do módulo pricing (Fase 3)
-- ============================================================

-- Cupons (códigos de desconto promocionais)
CREATE TABLE cupons (
    id                         BIGSERIAL    PRIMARY KEY,
    codigo                     VARCHAR(50)  NOT NULL UNIQUE,
    tipo                       VARCHAR(20)  NOT NULL,
    valor                      NUMERIC(10,2) NOT NULL,
    valor_minimo_compra        NUMERIC(10,2),
    data_inicio                TIMESTAMP    NOT NULL,
    data_fim                   TIMESTAMP,
    uso_maximo_total           INTEGER,
    uso_maximo_por_cliente     INTEGER,
    ativo                      BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_por                 BIGINT       REFERENCES usuarios(id),
    criado_em                  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_cupons_tipo CHECK (tipo IN ('PERCENTUAL','VALOR_FIXO')),
    CONSTRAINT chk_cupons_valor_positivo CHECK (valor > 0),
    CONSTRAINT chk_cupons_percentual_max CHECK (tipo <> 'PERCENTUAL' OR valor <= 100)
);

CREATE INDEX idx_cupons_codigo ON cupons(codigo);
CREATE INDEX idx_cupons_ativo ON cupons(ativo);

-- Histórico de uso de cupons (1 cupom pode ser usado por vários, com limites)
CREATE TABLE cupons_uso (
    id              BIGSERIAL  PRIMARY KEY,
    cupom_id        BIGINT     NOT NULL REFERENCES cupons(id) ON DELETE CASCADE,
    usuario_id      BIGINT     NOT NULL REFERENCES usuarios(id),
    pedido_id       BIGINT,
    valor_desconto  NUMERIC(10,2) NOT NULL,
    usado_em        TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_cupons_uso_cupom ON cupons_uso(cupom_id);
CREATE INDEX idx_cupons_uso_usuario ON cupons_uso(usuario_id);
CREATE INDEX idx_cupons_uso_pedido ON cupons_uso(pedido_id);

-- Promoções (descontos automáticos por categoria/produto/marca)
CREATE TABLE promocoes (
    id                    BIGSERIAL    PRIMARY KEY,
    nome                  VARCHAR(200) NOT NULL,
    tipo                  VARCHAR(20)  NOT NULL,
    desconto_percentual   NUMERIC(5,2) NOT NULL,
    data_inicio           TIMESTAMP    NOT NULL,
    data_fim              TIMESTAMP,
    ativo                 BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_promocoes_tipo CHECK (tipo IN ('CATEGORIA','PRODUTO','MARCA','FLASH_SALE')),
    CONSTRAINT chk_promocoes_desconto CHECK (desconto_percentual > 0 AND desconto_percentual <= 100)
);

CREATE INDEX idx_promocoes_tipo ON promocoes(tipo);
CREATE INDEX idx_promocoes_ativo ON promocoes(ativo);

-- Alvos das promoções
CREATE TABLE promocoes_itens (
    id                 BIGSERIAL    PRIMARY KEY,
    promocao_id        BIGINT       NOT NULL REFERENCES promocoes(id) ON DELETE CASCADE,
    referencia_id      BIGINT,
    referencia_valor   VARCHAR(100),
    tipo_referencia    VARCHAR(20)  NOT NULL,
    CONSTRAINT chk_promocoes_itens_tipo CHECK (tipo_referencia IN ('CATEGORIA','PRODUTO','MARCA'))
);

CREATE INDEX idx_promocoes_itens_promo ON promocoes_itens(promocao_id);

-- Regras de imposto por NCM + UF de origem/destino
CREATE TABLE regras_imposto (
    id                 BIGSERIAL    PRIMARY KEY,
    ncm                VARCHAR(8)   NOT NULL,
    uf_origem          VARCHAR(2)   NOT NULL,
    uf_destino         VARCHAR(2)   NOT NULL,
    icms_aliquota      NUMERIC(5,2) NOT NULL DEFAULT 0,
    ipi_aliquota       NUMERIC(5,2) NOT NULL DEFAULT 0,
    pis_aliquota       NUMERIC(5,2) NOT NULL DEFAULT 0,
    cofins_aliquota    NUMERIC(5,2) NOT NULL DEFAULT 0,
    vigencia_inicio    DATE         NOT NULL,
    vigencia_fim       DATE,
    criado_em          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_regras_imposto_lookup ON regras_imposto(ncm, uf_origem, uf_destino, vigencia_inicio);
