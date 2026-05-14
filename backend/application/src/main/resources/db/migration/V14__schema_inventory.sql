-- ============================================================
-- V14 — Schema inventory (Fase 4)
-- ============================================================
-- Estoque por SKU, movimentações (audit trail) e reservas com TTL.
-- Não usa FK direta para `produtos.id` em `reservas_estoque.produto_id` para
-- permitir que a fase trabalhe por SKU (consistente com o `CartItem.sku`).
-- O `Estoque.produto_id` aponta para `produtos.id` (canônico).

CREATE TABLE estoques (
    id                      BIGSERIAL    PRIMARY KEY,
    produto_id              BIGINT       NOT NULL UNIQUE REFERENCES produtos(id),
    quantidade              INTEGER      NOT NULL DEFAULT 0,
    quantidade_reservada    INTEGER      NOT NULL DEFAULT 0,
    quantidade_minima       INTEGER      NOT NULL DEFAULT 0,
    localizacao             VARCHAR(50)  NOT NULL DEFAULT 'DEPOSITO_PRINCIPAL',
    criado_em               TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_estoques_qty_positiva CHECK (quantidade >= 0),
    CONSTRAINT chk_estoques_reservada_positiva CHECK (quantidade_reservada >= 0),
    CONSTRAINT chk_estoques_reservada_le_qty CHECK (quantidade_reservada <= quantidade)
);
CREATE INDEX idx_estoques_low_stock
    ON estoques(produto_id) WHERE quantidade - quantidade_reservada <= quantidade_minima;

CREATE TABLE movimentacoes_estoque (
    id                BIGSERIAL    PRIMARY KEY,
    produto_id        BIGINT       NOT NULL REFERENCES produtos(id),
    tipo              VARCHAR(30)  NOT NULL,
    quantidade        INTEGER      NOT NULL,
    motivo            VARCHAR(200),
    pedido_id         BIGINT,
    referencia_pedido VARCHAR(50),
    criado_por_id     BIGINT       REFERENCES usuarios(id),
    criado_em         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_movimentacoes_tipo
        CHECK (tipo IN ('ENTRADA','SAIDA','AJUSTE','RESERVA','LIBERACAO_RESERVA','BAIXA_VENDA'))
);
CREATE INDEX idx_movimentacoes_produto ON movimentacoes_estoque(produto_id, criado_em DESC);
CREATE INDEX idx_movimentacoes_pedido  ON movimentacoes_estoque(pedido_id) WHERE pedido_id IS NOT NULL;

CREATE TABLE reservas_estoque (
    id                BIGSERIAL    PRIMARY KEY,
    produto_id        BIGINT       NOT NULL REFERENCES produtos(id),
    pedido_id         BIGINT,
    referencia_pedido VARCHAR(50)  NOT NULL,
    quantidade        INTEGER      NOT NULL,
    status            VARCHAR(20)  NOT NULL,
    expira_em         TIMESTAMP    NOT NULL,
    criado_em         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_reservas_status
        CHECK (status IN ('ATIVA','CONFIRMADA','EXPIRADA','CANCELADA')),
    CONSTRAINT chk_reservas_qty_positiva CHECK (quantidade > 0)
);
CREATE INDEX idx_reservas_status_expira
    ON reservas_estoque(status, expira_em) WHERE status = 'ATIVA';
CREATE INDEX idx_reservas_referencia ON reservas_estoque(referencia_pedido);

-- Seed: cria linha de estoque com 50 unidades para cada produto ativo.
-- Minimo = 5 (limiar de alerta).
INSERT INTO estoques (produto_id, quantidade, quantidade_reservada, quantidade_minima)
SELECT id, 50, 0, 5 FROM produtos WHERE ativo = TRUE;
