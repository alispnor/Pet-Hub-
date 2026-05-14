-- ============================================================
-- V15 — Schema order (Fase 4)
-- ============================================================
-- Pedido completo com itens (snapshot), eventos auditáveis e sequência
-- de numeração por ano (formato PH-{YYYY}-{NNNNNN}).
--
-- Endereços são snapshots em JSON (não FK) — se o cliente deletar um endereço
-- o pedido continua íntegro.

-- Sequência humana por ano: lock pessimista no incremento
CREATE TABLE pedido_sequence (
    ano             INTEGER     PRIMARY KEY,
    ultimo_numero   INTEGER     NOT NULL DEFAULT 0,
    atualizado_em   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE pedidos (
    id                          BIGSERIAL    PRIMARY KEY,
    numero_pedido               VARCHAR(20)  NOT NULL UNIQUE,
    cliente_id                  BIGINT       NOT NULL REFERENCES usuarios(id),
    status                      VARCHAR(30)  NOT NULL,
    endereco_entrega_snapshot   JSONB        NOT NULL,
    endereco_cobranca_snapshot  JSONB        NOT NULL,
    opcao_frete_snapshot        JSONB        NOT NULL,
    cupom_codigo                VARCHAR(50),
    valor_subtotal              NUMERIC(10,2) NOT NULL,
    valor_descontos             NUMERIC(10,2) NOT NULL DEFAULT 0,
    valor_impostos              NUMERIC(10,2) NOT NULL DEFAULT 0,
    valor_frete                 NUMERIC(10,2) NOT NULL,
    valor_total                 NUMERIC(10,2) NOT NULL,
    forma_pagamento_tipo        VARCHAR(20)  NOT NULL,
    forma_pagamento_ultimos4    VARCHAR(4),
    forma_pagamento_bandeira    VARCHAR(20),
    tentativa_pagamento_id      BIGINT       REFERENCES tentativas_pagamento(id),
    observacoes                 VARCHAR(500),
    criado_em                   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em               TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_pedidos_status CHECK (status IN (
        'PENDENTE_PAGAMENTO',
        'PAGAMENTO_APROVADO',
        'PAGAMENTO_REJEITADO',
        'SEPARACAO',
        'EM_TRANSPORTE',
        'ENTREGUE',
        'CANCELADO',
        'DEVOLVIDO'
    )),
    CONSTRAINT chk_pedidos_forma_pagamento_tipo
        CHECK (forma_pagamento_tipo IN ('CARTAO_CREDITO','CARTAO_DEBITO','PIX','BOLETO'))
);
CREATE INDEX idx_pedidos_cliente_criado ON pedidos(cliente_id, criado_em DESC);
CREATE INDEX idx_pedidos_status         ON pedidos(status);
CREATE INDEX idx_pedidos_numero         ON pedidos(numero_pedido);

CREATE TABLE pedido_itens (
    id                 BIGSERIAL     PRIMARY KEY,
    pedido_id          BIGINT        NOT NULL REFERENCES pedidos(id) ON DELETE CASCADE,
    produto_id         BIGINT        NOT NULL,
    sku                VARCHAR(50)   NOT NULL,
    nome_produto       VARCHAR(200)  NOT NULL,
    imagem_url         VARCHAR(500),
    qty                INTEGER       NOT NULL,
    preco_unitario     NUMERIC(10,2) NOT NULL,
    desconto_unitario  NUMERIC(10,2) NOT NULL DEFAULT 0,
    preco_final        NUMERIC(10,2) NOT NULL,
    ncm                VARCHAR(10),
    impostos           JSONB,
    criado_em          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_pedido_itens_qty_positiva CHECK (qty > 0)
);
CREATE INDEX idx_pedido_itens_pedido ON pedido_itens(pedido_id);

CREATE TABLE pedido_eventos (
    id            BIGSERIAL    PRIMARY KEY,
    pedido_id     BIGINT       NOT NULL REFERENCES pedidos(id) ON DELETE CASCADE,
    tipo          VARCHAR(40)  NOT NULL,
    descricao     VARCHAR(500),
    ocorrido_em   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ator_tipo     VARCHAR(20)  NOT NULL,
    ator_id       BIGINT,
    payload       JSONB,
    CONSTRAINT chk_eventos_ator_tipo CHECK (ator_tipo IN ('SISTEMA','CLIENTE','ADMIN','GATEWAY'))
);
CREATE INDEX idx_pedido_eventos_pedido_ocorrido ON pedido_eventos(pedido_id, ocorrido_em);
