-- ============================================================
-- V5 — Adiciona cpf_hash (SHA-256) ao perfil_cliente
-- ============================================================
-- AES-GCM gera ciphertext diferente a cada chamada (IV randômico),
-- então o ciphertext não serve para detectar duplicidade. Mantemos
-- um hash determinístico ao lado, com índice UNIQUE.

ALTER TABLE perfil_cliente
    ADD COLUMN cpf_hash VARCHAR(64) UNIQUE;

CREATE INDEX idx_perfil_cliente_cpf_hash ON perfil_cliente(cpf_hash);
