-- V16: Usuários de teste adicionais para smoke E2E manual
--
-- Por que existe: o seed da V3 só cria 1 admin com role ADMIN_LOJA e 3
-- clientes. Para validar matriz de permissões (RBAC) e cenários de admin
-- em dev/staging, precisamos cobrir as roles GERENTE e OPERADOR também,
-- além de um cliente "padrão" facilmente identificável.
--
-- IDEMPOTENTE: usa ON CONFLICT para suportar:
--   - ambiente novo (nada existe → INSERT puro)
--   - ambiente onde algum usuário foi criado runtime durante validação
--     manual (caso da sessão de 2026-05-16 — não estoura unique constraint)
--   - re-aplicação por mistake (NOT NULL hash mantém quem está lá)
--
-- Senhas (bcrypt cost=12, mesmo do AuthService):
--   gerente@pethub.com    -> Gerente@123
--   operador@pethub.com   -> Operador@123
--   joao.teste@pethub.com -> Teste@123
--
-- IMPORTANTE: estes hashes são válidos apenas para o conjunto fixo acima.
-- NÃO copiar para outros ambientes sem trocar a senha em seguida.

INSERT INTO usuarios (nome, email, senha_hash, tipo_usuario, ativo, data_cadastro)
VALUES
    ('Gerente Teste',  'gerente@pethub.com',
        '$2b$12$BX7eSojIjZptZ9NnMgLJXOW1LTFlDN8n7gxn6f5fjggOqUtoBf8.6',
        'ADMIN',   TRUE, NOW()),
    ('Operador Teste', 'operador@pethub.com',
        '$2b$12$FP2EG8UiM5HoVzubCcGeNuR3uP02cZRe/XNP7o.iVWsFJ4Dtwhj.2',
        'ADMIN',   TRUE, NOW()),
    ('João Teste',     'joao.teste@pethub.com',
        '$2b$12$CBRKT5sjtDVxjLTQY/uhP.ZGyzKntT7KaDY9wVscI04ah5IJuk68.',
        'CLIENTE', TRUE, NOW())
ON CONFLICT (email) DO NOTHING;

-- Atribui roles. Subquery garante que pega o id correto mesmo se ON CONFLICT
-- não criou (linha pré-existente) — o usuario_id existente é reutilizado.
INSERT INTO usuario_roles (usuario_id, role)
SELECT id, 'ROLE_GERENTE'
  FROM usuarios WHERE email = 'gerente@pethub.com'
ON CONFLICT (usuario_id, role) DO NOTHING;

INSERT INTO usuario_roles (usuario_id, role)
SELECT id, 'ROLE_OPERADOR'
  FROM usuarios WHERE email = 'operador@pethub.com'
ON CONFLICT (usuario_id, role) DO NOTHING;

INSERT INTO usuario_roles (usuario_id, role)
SELECT id, 'ROLE_CLIENTE'
  FROM usuarios WHERE email = 'joao.teste@pethub.com'
ON CONFLICT (usuario_id, role) DO NOTHING;
