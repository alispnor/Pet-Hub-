-- ============================================================
-- V12 — Schema do módulo shipping (Fase 3)
-- ============================================================

CREATE TABLE faixas_cep_regiao (
    id           BIGSERIAL    PRIMARY KEY,
    cep_inicio   VARCHAR(8)   NOT NULL,
    cep_fim      VARCHAR(8)   NOT NULL,
    regiao       VARCHAR(20)  NOT NULL,
    criado_em    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_faixas_cep_regiao
        CHECK (regiao IN ('NORTE','NORDESTE','CENTRO_OESTE','SUDESTE','SUL')),
    CONSTRAINT chk_faixas_cep_inicio CHECK (cep_inicio ~ '^\d{8}$'),
    CONSTRAINT chk_faixas_cep_fim CHECK (cep_fim ~ '^\d{8}$')
);

CREATE INDEX idx_faixas_cep_lookup ON faixas_cep_regiao(cep_inicio, cep_fim);

-- Seed de faixas aproximadas (5 regiões brasileiras pelo CEP).
INSERT INTO faixas_cep_regiao (cep_inicio, cep_fim, regiao) VALUES
    ('01000000','19999999','SUDESTE'),       -- SP
    ('20000000','28999999','SUDESTE'),       -- RJ
    ('29000000','29999999','SUDESTE'),       -- ES
    ('30000000','39999999','SUDESTE'),       -- MG
    ('40000000','48999999','NORDESTE'),      -- BA
    ('49000000','49999999','NORDESTE'),      -- SE
    ('50000000','56999999','NORDESTE'),      -- PE
    ('57000000','57999999','NORDESTE'),      -- AL
    ('58000000','58999999','NORDESTE'),      -- PB
    ('59000000','59999999','NORDESTE'),      -- RN
    ('60000000','63999999','NORDESTE'),      -- CE
    ('64000000','64999999','NORDESTE'),      -- PI
    ('65000000','65999999','NORDESTE'),      -- MA
    ('66000000','68899999','NORTE'),         -- PA
    ('68900000','68999999','NORTE'),         -- AP
    ('69000000','69299999','NORTE'),         -- AM
    ('69300000','69399999','NORTE'),         -- RR
    ('69400000','69899999','NORTE'),         -- AM
    ('69900000','69999999','NORTE'),         -- AC
    ('70000000','72799999','CENTRO_OESTE'),  -- DF/GO
    ('72800000','73699999','CENTRO_OESTE'),  -- GO
    ('73700000','76799999','CENTRO_OESTE'),  -- GO/TO
    ('76800000','76999999','NORTE'),         -- RO
    ('77000000','77999999','NORTE'),         -- TO
    ('78000000','78899999','CENTRO_OESTE'),  -- MT
    ('78900000','78999999','NORTE'),         -- RO
    ('79000000','79999999','CENTRO_OESTE'),  -- MS
    ('80000000','87999999','SUL'),           -- PR
    ('88000000','89999999','SUL'),           -- SC
    ('90000000','99999999','SUL');           -- RS
