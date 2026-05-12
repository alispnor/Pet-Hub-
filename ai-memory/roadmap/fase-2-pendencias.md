# Fase 2 — Pendências e checklist

> **Status:** Fase 2 em andamento (4/6 commits entregues e validados em 2026-05-12).
> **Próxima sessão:** continuar pelas tasks #10 e #11.

## ✅ Commits entregues nesta sessão (4)

1. `86cfd4c chore(customer): scaffold module with AES-GCM crypto and V4 migration`
   - Módulo `pet-hub-customer` adicionado ao reactor
   - `AesGcmCipher` + `EncryptedStringConverter` em `common/security/`
   - V4 cria tabela `perfil_cliente`
   - `app.encryption.key` em `application.yml` (default dev gerada com `openssl rand -base64 32`)

2. `2785857 feat(backend): enable swagger ui with bearer auth and parameterize dev compose`
   - `OpenApiConfig` com schema `bearerAuth` (botão Authorize no /swagger-ui.html cola token uma vez)
   - `springdoc-openapi-starter-webmvc-ui` adicionada também em `application/` e `catalog/`
   - `docker-compose.dev.yml` lê de env vars com defaults → workflow `docker compose --env-file .env.local ...`
   - `.env.local.example` documenta todas as variáveis

3. `1d6a7ec feat(customer): add PerfilCliente entity with encrypted CPF and profile endpoints`
   - V5: `ALTER TABLE perfil_cliente ADD COLUMN cpf_hash VARCHAR(64) UNIQUE`
   - CPF AES-GCM no DB; hash SHA-256 determinístico para detectar duplicidade
   - Endpoints `GET/PUT /me/profile` e `POST /me/profile/cpf` (idempotente; 409 se CPF de outro user)
   - Response sempre com CPF mascarado `***.XXX.XXX-**`

4. `0a9f1cb feat(customer): add Pet entity with CRUD and isolated multipart photo upload`
   - V6 cria tabela `pets` com check de Especie/Porte
   - `ForbiddenException` (HTTP 403) adicionada em `common/exception/`
   - CRUD `/me/pets` + upload multipart `/me/pets/{id}/photo`
   - `PhotoStorage` interface + `LocalPhotoStorage` (gravando em `${app.uploads.dir}`)
   - WebMvcConfig expõe `/files/**` read-only para servir as fotos
   - Isolamento testado: user B → 3× 403

5. `f638522 feat(customer): add address CRUD with default unicity and viacep redis cache`
   - V7 cria `enderecos`; V8 alarga `uf` de CHAR(2) para VARCHAR(2) (Hibernate schema-validation exigia Types#VARCHAR)
   - Dois unique partial indexes (`WHERE padrao_entrega = TRUE` e `WHERE padrao_cobranca = TRUE`) garantem unicidade por perfil
   - ViaCEP via `RestClient` cacheado em Redis (`@Cacheable("cep")`, TTL 24h)
   - `RedisCacheManager` configurado manualmente com `GenericJackson2JsonRedisSerializer` default
   - E2E: 1ª chamada 1711ms → 2ª 28ms (61× mais rápido)

## ⚠️ O que falta (próxima sessão)

### Task #10 — FormaPagamento + MockPaymentGateway

Spec original em `plano-completo.md` linhas ~414:

- Migration V9: tabela `formas_pagamento` com colunas conforme LGPD/PCI:
  - `id`, `perfil_cliente_id`, `tipo` (CARTAO_CREDITO/CARTAO_DEBITO/PIX/BOLETO), `apelido`, `gateway_token`, `bandeira` (VISA/MASTER/AMEX/ELO/HIPERCARD/OUTRO), `ultimos_quatro_digitos`, `nome_impresso`, `validade_mes`, `validade_ano`, `padrao`, `ativo`
- Entity `FormaPagamento` + enums `TipoPagamento` e `Bandeira`
- Interface `PaymentGateway` com `tokenizeCard(cardData) → token` (retorna `tok_mock_xxxx`)
- Impl `MockPaymentGateway` no módulo customer (substituído por MP/Stripe sandbox na Fase 3)
- Endpoints:
  - `GET    /api/v1/customers/me/payment-methods`
  - `POST   /api/v1/customers/me/payment-methods` (recebe token do gateway, **NÃO** o cartão)
  - `DELETE /api/v1/customers/me/payment-methods/{id}`
  - `POST   /api/v1/customers/me/payment-methods/{id}/default`
- Para PIX e BOLETO, apenas o `tipo` é armazenado (sem token nem cartão)
- Mesma lógica de "padrão único" usando partial unique index

### Task #11 — Admin endpoints + testes de integração

- `GET /api/v1/admin/customers?q=&page=&size=` lista paginada com email mascarado, **sem** CPF/cartões
- `GET /api/v1/admin/customers/{id}` detalhes (sem CPF completo, sem cartões)
- Autorização: role `ADMIN_LOJA`/`GERENTE` (matriz da Fase 1)
- Testes de integração (precisam JDK 21 + Maven nativos OU resolver Testcontainers-em-container):
  - Cobertura > 85% do módulo customer (sem JaCoCo configurado ainda)
  - Tentar criar 2 endereços padrão entrega → último prevalece (✅ já comprovado em smoke; falta como teste automatizado)
  - Criptografia: salvar CPF, ler raw via JdbcTemplate, verificar que está cifrado (✅ já comprovado em smoke)
  - Isolation: cliente A não vê dados de B (✅ já comprovado em smoke)

### Decisões a confirmar

- `Usuario.cpf` ainda existe em texto plano (legado da V1). Decisão pragmática: deixar como está, mover lookup de CPF pra `perfil_cliente.cpf_hash`. Eventualmente DROP em uma migration de Fase 2/3.
- `OPERADOR` ainda não tem acesso aos endpoints admin de customer — alinhar com matriz de roles.

### Dívida técnica herdada da Fase 1 (ainda aberta)

- Testes Testcontainers falham em container (rede docker entre sibling containers). Solução: instalar JDK 21 + Maven no host, OU rodar Maven na mesma bridge dos containers Testcontainers. Adiar.
- Testes unitários Mockito para `AuthService`, `ProdutoService`, e agora `PerfilService`/`PetService`/`EnderecoService`.
- JaCoCo plugin no parent POM.

## 🚀 Comando rápido para retomar

```bash
# Verificar estado
cat ai-memory/roadmap/fase-2-pendencias.md
git log --oneline -10

# Subir infra + app via container
docker compose --env-file .env.local -f infrastructure/docker/docker-compose.dev.yml up -d
cd backend && docker run --rm -d --name pethub-app-dev \
  --env-file /home/ali/projects/pet-hub/.env.local \
  -v "$PWD":/workspace -v /home/ali/.m2:/root/.m2 \
  --network host -w /workspace \
  maven:3.9-eclipse-temurin-21 \
  mvn -B -ntp -pl application spring-boot:run -DskipTests
```

Swagger: http://localhost:8080/swagger-ui.html
Login dev: `admin@pethub.com` / `Admin@123` (admin) ou `maria.fase2@pethub.com` / `Senha@123` (cliente)
