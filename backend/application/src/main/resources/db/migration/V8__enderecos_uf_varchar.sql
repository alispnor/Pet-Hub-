-- ============================================================
-- V8 — enderecos.uf: CHAR(2) → VARCHAR(2)
-- ============================================================
-- Hibernate schema-validation espera VARCHAR para enums com EnumType.STRING.
-- Conteúdo armazenado segue sendo o name() do enum ("SP", "RJ", ...).

ALTER TABLE enderecos ALTER COLUMN uf TYPE VARCHAR(2);
