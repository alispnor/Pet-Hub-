-- ============================================================
-- V9 — Tabela formas_pagamento (Fase 2)
-- ============================================================
-- PCI-DSS: NUNCA persistir PAN (número completo) nem CVV. Apenas o
-- token retornado pelo gateway de pagamento + bandeira + 4 últimos
-- dígitos + nome impresso + validade.

CREATE TABLE formas_pagamento (
    id                     BIGSERIAL    PRIMARY KEY,
    perfil_cliente_id      BIGINT       NOT NULL REFERENCES perfil_cliente(id) ON DELETE CASCADE,
    tipo                   VARCHAR(20)  NOT NULL,
    apelido                VARCHAR(50),
    gateway_token          VARCHAR(100),
    bandeira               VARCHAR(20),
    ultimos_quatro_digitos VARCHAR(4),
    nome_impresso          VARCHAR(100),
    validade_mes           SMALLINT,
    validade_ano           SMALLINT,
    padrao                 BOOLEAN      NOT NULL DEFAULT FALSE,
    ativo                  BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_formas_pagamento_tipo
        CHECK (tipo IN ('CARTAO_CREDITO','CARTAO_DEBITO','PIX','BOLETO')),
    CONSTRAINT chk_formas_pagamento_bandeira
        CHECK (bandeira IS NULL OR bandeira IN ('VISA','MASTER','AMEX','ELO','HIPERCARD','OUTRO')),
    CONSTRAINT chk_formas_pagamento_ultimos
        CHECK (ultimos_quatro_digitos IS NULL OR ultimos_quatro_digitos ~ '^\d{4}$'),
    CONSTRAINT chk_formas_pagamento_validade_mes
        CHECK (validade_mes IS NULL OR (validade_mes BETWEEN 1 AND 12)),
    CONSTRAINT chk_formas_pagamento_cartao_completo
        CHECK (
            tipo NOT IN ('CARTAO_CREDITO','CARTAO_DEBITO')
            OR (gateway_token IS NOT NULL
                AND bandeira IS NOT NULL
                AND ultimos_quatro_digitos IS NOT NULL
                AND validade_mes IS NOT NULL
                AND validade_ano IS NOT NULL)
        )
);

CREATE INDEX idx_formas_pagamento_perfil ON formas_pagamento(perfil_cliente_id);

-- Apenas 1 forma de pagamento pode ser padrão por perfil.
CREATE UNIQUE INDEX uq_formas_pagamento_padrao
    ON formas_pagamento(perfil_cliente_id)
    WHERE padrao = TRUE;
