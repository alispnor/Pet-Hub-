-- ============================================================
-- V1 — Schema do módulo identity
-- ============================================================

CREATE TABLE usuarios (
    id                 BIGSERIAL PRIMARY KEY,
    nome               VARCHAR(200) NOT NULL,
    email              VARCHAR(200) NOT NULL UNIQUE,
    senha_hash         VARCHAR(100) NOT NULL,
    cpf                VARCHAR(11)  UNIQUE,
    telefone           VARCHAR(20),
    tipo_usuario       VARCHAR(20)  NOT NULL,
    ativo              BOOLEAN      NOT NULL DEFAULT TRUE,
    tentativas_falhas  INT          NOT NULL DEFAULT 0,
    bloqueado_ate      TIMESTAMP,
    data_cadastro      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ultimo_login       TIMESTAMP,
    CONSTRAINT chk_usuarios_tipo CHECK (tipo_usuario IN ('CLIENTE', 'ADMIN'))
);

CREATE INDEX idx_usuarios_email ON usuarios(email);
CREATE INDEX idx_usuarios_tipo  ON usuarios(tipo_usuario);

CREATE TABLE usuario_roles (
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    role       VARCHAR(30) NOT NULL,
    PRIMARY KEY (usuario_id, role),
    CONSTRAINT chk_usuario_roles_value
        CHECK (role IN ('ROLE_CLIENTE','ROLE_ADMIN_LOJA','ROLE_GERENTE','ROLE_OPERADOR'))
);

CREATE TABLE refresh_tokens (
    id          BIGSERIAL PRIMARY KEY,
    usuario_id  BIGINT       NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    token_hash  VARCHAR(64)  NOT NULL UNIQUE,
    expira_em   TIMESTAMP    NOT NULL,
    revogado    BOOLEAN      NOT NULL DEFAULT FALSE,
    criado_em   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens(token_hash);
CREATE INDEX idx_refresh_tokens_usuario    ON refresh_tokens(usuario_id);
CREATE INDEX idx_refresh_tokens_expira     ON refresh_tokens(expira_em);
