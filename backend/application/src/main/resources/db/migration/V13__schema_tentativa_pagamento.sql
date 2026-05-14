-- ============================================================
-- V13 — Tabela tentativas_pagamento (Fase 3)
-- ============================================================
-- Persistência das tentativas de cobrança feitas pelo /checkout/place-order.
-- A entity Pedido completa entra na Fase 4 com máquina de estados; aqui só
-- guardamos o que foi cobrado (valor, método, gateway, status).

CREATE TABLE tentativas_pagamento (
    id                       BIGSERIAL    PRIMARY KEY,
    referencia_pedido        VARCHAR(50)  NOT NULL,
    usuario_id               BIGINT       NOT NULL REFERENCES usuarios(id),
    forma_pagamento_id       BIGINT       REFERENCES formas_pagamento(id),
    metodo                   VARCHAR(20)  NOT NULL,
    valor                    NUMERIC(10,2) NOT NULL,
    gateway_transaction_id   VARCHAR(100),
    status                   VARCHAR(20)  NOT NULL,
    qr_code                  TEXT,
    boleto_url               VARCHAR(500),
    response_gateway         JSONB,
    idempotency_key          VARCHAR(100) NOT NULL UNIQUE,
    criado_em                TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_tentativas_metodo
        CHECK (metodo IN ('CARTAO_CREDITO','CARTAO_DEBITO','PIX','BOLETO')),
    CONSTRAINT chk_tentativas_status
        CHECK (status IN ('PROCESSING','APPROVED','REJECTED','REFUNDED'))
);

CREATE INDEX idx_tentativas_pagamento_usuario ON tentativas_pagamento(usuario_id);
CREATE INDEX idx_tentativas_pagamento_referencia ON tentativas_pagamento(referencia_pedido);
CREATE INDEX idx_tentativas_pagamento_status ON tentativas_pagamento(status);
