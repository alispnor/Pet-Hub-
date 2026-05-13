-- ============================================================
-- V10 — formas_pagamento.validade_{mes,ano}: SMALLINT → INTEGER
-- ============================================================
-- Hibernate schema-validation espera Types#INTEGER (java.lang.Integer)
-- e rejeita Types#SMALLINT. Mesma classe de problema da V8 (uf CHAR→VARCHAR).
--
-- V9 não pode ser editada (Flyway é append-only e o checksum bate em
-- ambientes que já aplicaram a versão anterior); por isso o fix entra
-- aqui em V10.

ALTER TABLE formas_pagamento ALTER COLUMN validade_mes TYPE INTEGER;
ALTER TABLE formas_pagamento ALTER COLUMN validade_ano TYPE INTEGER;
