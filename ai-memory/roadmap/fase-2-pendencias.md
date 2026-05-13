# Fase 2 — Pendências e checklist

> **Status:** Fase 2 ✅ entregue em 2026-05-13. 7 commits, todos validados E2E.
> Testes unitários/integração formais com JaCoCo permanecem como dívida técnica (mesmo bloqueio da Fase 1: Testcontainers em container).

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

## ✅ Commits entregues na continuação (2026-05-13)

6. `c4ccb05 feat(customer): add payment method CRUD and mock pci-safe tokenization`
   - V9 cria `formas_pagamento`; V10 alarga `validade_{mes,ano}` de SMALLINT para INTEGER (mesmo padrão de fix da V8)
   - `PaymentGateway` + `MockPaymentGateway` (Luhn, detecção de bandeira por prefixo, valida CVV e expiry)
   - `/payment-methods/tokenize` simula o que o frontend/gateway externo fariam
   - `gateway_token` persistido mas nunca aparece nas responses
   - Padrão único via partial index `uq_formas_pagamento_padrao`

7. `f9691df feat(customer): add admin customer listing and detail with masked pii`
   - `GET /admin/customers?q=&page=&size=` + `GET /admin/customers/{id}`
   - Email/telefone/CPF mascarados; cartões/tokens nunca expostos
   - `@PreAuthorize` ROLE_ADMIN_LOJA/ROLE_GERENTE — cliente comum → 403
   - `UsuarioRepository.searchByTipo` separado de `findByTipo` para evitar `function lower(bytea) does not exist` (Postgres inferia BYTEA no parâmetro NULL)

## ✅ Validações E2E acumuladas

- Criptografia AES-GCM: CPF persistido como ciphertext base64 (52 bytes); hash SHA-256 determinístico permite detectar duplicidade sem decrypt
- ViaCEP Redis cache: 1ª chamada 1711ms → 2ª 28ms (TTL 24h)
- Isolamento por owner: 12+ tentativas de cross-user em pet/endereço/forma de pagamento → 403 (`ForbiddenException`)
- Padrão único: 3 partial unique indexes (endereço entrega, endereço cobrança, forma pagamento padrão)
- PCI-safe: tokenize com Luhn passa → token retornado; cartão duplicado com dígito errado → 422; persistência só guarda token + brand + últimos 4
- Admin: cliente comum → 403; sem token → 403; admin filtra por nome/email funciona; admin tentando detalhar usuário ADMIN → 404

## ⚠️ Dívida técnica restante (não bloqueia próximas fases)

### Testes formais (acumulado Fase 1 + Fase 2)

Todos os cenários smoke acima precisam virar testes unitários (Mockito) e de integração (Testcontainers) cobrindo `AuthService`, `ProdutoService`, `PerfilService`, `PetService`, `EnderecoService`, `FormaPagamentoService`, `AdminCustomerService`. Spec pede cobertura > 85% no módulo customer e > 80% no identity/catalog.

**Bloqueio:** Testcontainers em container Maven falha (rede entre sibling containers). Solução: `sudo apt install openjdk-21-jdk maven` no host e rodar Maven nativamente. Sem isso, manter como dívida.

### JaCoCo

Plugin no parent POM com `merge` execution agregando relatórios. Sem ele, "cobertura > 85%" não é medível.

### Decisões a confirmar

- `Usuario.cpf` ainda existe em texto plano (legado da V1). Decisão pragmática: deixar como está, mover lookup de CPF pra `perfil_cliente.cpf_hash`. Eventualmente DROP em uma migration de Fase 3.
- Endpoints `/api/v1/admin/customers` exigem `ADMIN_LOJA` ou `GERENTE`; `OPERADOR` recebe 403 mesmo o `SecurityConfig` global permitindo `/admin/**`. Alinhar com a matriz de roles formal antes da Fase 6 (admin SPA).

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
