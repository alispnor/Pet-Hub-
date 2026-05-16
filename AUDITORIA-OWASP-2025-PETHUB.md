# Auditoria OWASP Top 10 2025 — Pet Hub

| Campo | Valor |
|---|---|
| **Documento base** | [`/home/ali/projects/guep-crm/AUDITORIA-OWASP-2025-PLANO.md`](file:///home/ali/projects/guep-crm/AUDITORIA-OWASP-2025-PLANO.md) — plano detalhado do CRM, usado como modelo |
| **Branch / commit** | `main` @ `d80e519` |
| **Data** | 2026-05-16 |
| **Stack escopo** | Backend Java 21/Spring Boot 3 + PostgreSQL + Redis · Frontend Angular 17 · Infra Docker (Fase 8) · CI/CD GitHub Actions (Fase 9) |
| **Método** | Mapping linha-a-linha dos 21 achados do CRM contra o estado atual do Pet Hub, complementado por inspeção de `appsec-guidelines.md`, `appsec-pendencias.md` e código fonte |
| **Status global** | 8 achados já catalogados, 3 já resolvidos pela escolha de stack, 5 GAPs novos adicionados em `appsec-pendencias.md`, 5 não aplicáveis ao stack Java/Spring |

---

## 0. Resumo Executivo

A auditoria do guep-crm identifica 21 achados (2 P0 + 5 P1 + 7 P2 + 7 P3). Mapeando contra o Pet Hub:

- **8 achados** já estão catalogados em [`ai-memory/roadmap/appsec-pendencias.md`](./ai-memory/roadmap/appsec-pendencias.md) com IDs próprios (JWT-1, RL-1, CORS-1, SEC-1, OBS-1, DEP-1, VAL-1, JPA-1, PAY-1, FLY-1, LOG-1). Os Fix-alvo e marcos já estão alinhados — nada a fazer aqui exceto continuar executando o que já foi planejado.
- **3 achados** já estão **resolvidos** pela escolha de stack ou configuração inicial:
  - **A04-1** (segredos no Git) → `.gitignore` cobre `.env*` desde a Fase 0. Validar histórico com `git ls-files | grep -E '^\.env'` deve retornar vazio.
  - **A02-6** (`trust proxy`) → `server.forward-headers-strategy: framework` em `application.yml` desde Fase 1.
  - **A08-2** (`sync({ alter: true })` em runtime) → Flyway versionado desde Fase 1; `application.yml` tem `spring.flyway.enabled: true` + migrations V0..V15.
- **5 GAPs novos** identificados e adicionados a `appsec-pendencias.md`:
  - **LOG-2** mapeamento PII em logs (a partir de A09-1)
  - **LOG-3** rotação de logs Logback (A09-3)
  - **DEP-2** auditoria de deps Maven não usadas (A03-2)
  - **EXC-1** uncaughtException handler para threads async (A10-1)
  - **PAY-3** Spring multipart/payload limits explícitos (A02-4)
- **5 achados** **não aplicáveis** ao stack do Pet Hub (são Node/Express/Sequelize/Caddy-específicos): A05-1 já está prevista com DOMPurify no slice 4; A07-1 (MFA Keycloak) idem fora do roadmap; A06-3 (S3 retention) Pet Hub não usa S3 hoje (uploads em volume local); A02-7 (`./public` estático) Pet Hub não serve estáticos; A03-3 (lock file) Maven não usa lockfile.

Não há item P0 aberto no Pet Hub. Os achados pendentes são todos P1-P3 com marcos amarrados ao roadmap (Fase 8 Docker, Fase 9 CI, Fase 11 cloud).

---

## 1. Mapping consolidado CRM → Pet Hub

| ID CRM | Achado | Sev. | Pet Hub: ID equivalente | Status no Pet Hub |
|---|---|---|---|---|
| A04-1 | Segredos versionados no Git | 🔴 P0 | — | ✅ Resolvido (`.gitignore` cobre `.env*`) |
| A02-1 | Containers root | 🔴 P0 | — | ⏳ Pendente (Fase 8 — Dockerfile do backend) |
| A01-1 | Fallback permissivo no `requirePermission` | 🟠 P1 | — | ⚪ N/A (Pet Hub usa `@PreAuthorize` Spring, sem fallback) |
| A02-2 | CSP desabilitado | 🟠 P1 | **VAL-1** | ⏳ Aguarda Fase 8 (nginx) |
| A02-3 | CORS default `*` | 🟠 P1 | **CORS-1** | ⏳ Pendente |
| A09-1 | LogsMiddleware sem redaction | 🟠 P1 | **LOG-2** | ⏳ Novo (mapeado nesta auditoria) |
| A07-2 | `iss`/`aud` não verificados em `jwt.verify` | 🟠 P1 | **JWT-1** | ⏳ Pendente |
| A02-4 | Payload 50 MB para JSON | 🟡 P2 | **PAY-3** | ⏳ Novo (mapeado nesta auditoria) |
| A02-5 | Sem rate limiting | 🟡 P2 | **RL-1** | ⏳ Pendente (Bucket4j+Redis Fase 11) |
| A03-1 | Sem SCA / Dependabot | 🟡 P2 | **DEP-1** | ⏳ Pendente |
| A03-2 | Dependências não usadas | 🟡 P2 | **DEP-2** | ⏳ Novo (mapeado nesta auditoria) |
| A06-2 | Sem audit log estruturado | 🟡 P2 | **OBS-1** | ⏳ Pendente (Fase 6 admin) |
| A08-1 | Sem CI/CD versionado | 🟡 P2 | — | ⏳ Esqueleto existe (`.github/workflows/ci.yml`), aguarda Fase 8/9 |
| A08-2 | `sync({ alter: true })` em runtime | 🟡 P2 | — | ✅ Resolvido (Flyway desde Fase 1) |
| A01-2 + A01-3 | MASTER bypass + middleware morto | 🔵 P3 | — | ⚪ N/A |
| A02-6 | `trust proxy` não configurado | 🔵 P3 | — | ✅ Resolvido (`forward-headers-strategy`) |
| A05-1 | Sanitização TipTap server-side | 🔵 P3 | — | ⏳ Slice 4 (DOMPurify pipe) + admin Fase 6 (server-side se entrar editor rico) |
| A06-3 | S3 retention fire-and-forget no boot | 🔵 P3 | — | ⚪ N/A (Pet Hub usa volume local para uploads hoje) |
| A09-2 | Sem forwarder de logs | 🔵 P3 | — | ⏳ Fase 11 (cloud com observability stack) |
| A09-3 | Winston sem rotação | 🔵 P3 | **LOG-3** | ⏳ Novo (mapeado nesta auditoria) |
| A10-1 | Sem `unhandledRejection` handler | 🔵 P3 | **EXC-1** | ⏳ Novo (mapeado nesta auditoria) |

Linhas adicionais do plano CRM (A04-2 HSTS, A05-2 node-geocoder, A07-1 MFA, A03-4 Bootstrap 4, A08-3 SRI) ou são já cobertas (A04-2 via Spring Security defaults), ou são N/A pelo stack (A05-2, A03-4, A08-3), ou estão fora do roadmap (A07-1 MFA).

## 2. Ondas de execução (ordem lógica)

A ordem abaixo reaproveita o esquema do CRM, adaptada ao roadmap do Pet Hub.

### Onda 1 — Bloqueantes pré-staging (deve fechar antes do storefront em ambiente compartilhado)

- **JWT-1** — Validar `iss`/`aud` em `JwtService` e `JwtAuthenticationFilter`. Esforço: baixo.
- **CORS-1** — `@PostConstruct` no `SecurityConfig` que falha o boot em `prod` se `allowedOrigins` contém `*`. Esforço: baixo.
- **VAL-1** — Headers `Content-Security-Policy`, `Strict-Transport-Security`, `Referrer-Policy`, `Permissions-Policy` no nginx (Fase 8) ou via Spring `http.headers(...)` para HSTS apenas em prod.

### Onda 2 — Higiene de logs (Fase 6 ou próximo PR em logback)

- **LOG-2** — `MaskingPatternLayout` em `logback-spring.xml` para PII.
- **LOG-1** — Handlers específicos para `DataIntegrityViolationException` e `HttpMessageNotReadableException` (já catalogado).
- **PAY-3** — `spring.servlet.multipart.max-file-size=2MB` + `max-request-size=10MB`.

### Onda 3 — Containers + headers (Fase 8)

- **A02-1 equivalente** — Dockerfile do backend com `USER appuser` non-root + `mkdir + chown` dos diretórios mutáveis (`uploads/`).
- **SEC-1** — HSTS + Referrer-Policy + Permissions-Policy via Spring Security em prod.
- **FLY-1** — `spring.flyway.clean-disabled=true` explícito em `application.yml`.
- **LOG-3** — RollingFileAppender + Docker `log-opts`.

### Onda 4 — CI/CD (Fase 9)

- **DEP-1** — OWASP Dependency-Check + Trivy + Dependabot.
- **DEP-2** — `mvn dependency:analyze` no pipeline, remover declared-not-used.
- **A08-1 equivalente** — Substituir o `ci.yml` placeholder por jobs reais (lint, test, build).

### Onda 5 — Audit + escalabilidade (Fase 6 admin + Fase 11 cloud)

- **OBS-1** — Tabela `eventos_seguranca` + emissão via `ApplicationEventPublisher`. Endpoint admin para consultar (Fase 6).
- **EXC-1** — Async + scheduled exception handlers (junto com OBS-1, para emitir eventos de catástrofe).
- **RL-1** — Migrar `LoginRateLimitFilter` para Bucket4j+Redis (Fase 11, quando entrar 2ª réplica).

### Onda 6 — Pagamento real (Fase 7)

- **PAY-1** — Whitelist de campos persistidos em `response_gateway` antes de plugar Mercado Pago/Stripe.

### Onda 7 — Operação

- **JPA-1** — `AesGcmCipher` versionado quando primeira rotação de chave for necessária.

---

## 3. Decisões pendentes para Ali

| Decisão | Opções | Recomendação | Impacto |
|---|---|---|---|
| Gestor de segredos (futuro) | AWS Secrets Manager / GCP Secret Manager / Vault / Oracle Vault | Aguardar Fase 11 (decisão de cloud); até lá, `.env.local` ignorado é suficiente | Médio |
| Backend de rate limit (RL-1) | Bucket4j+Redis / Spring Cloud Gateway / nginx limit_req | Bucket4j+Redis (já tem Redis na stack desde Fase 1) | Médio |
| Stack de observability (A09-2) | CloudWatch / Loki+Grafana / Datadog | Depende da cloud (Fase 11) | Médio |
| Cadência de DEP-1 scan | Pre-merge / nightly / weekly | Pre-merge bloqueante (threshold CVSS ≥ 7) | Alto |
| Audit log retention (OBS-1) | 1 ano / 2 anos / partição | 2 anos com partição trimestral por `data_evento` | Médio |

## 4. Próxima auditoria

Recomenda-se nova auditoria após a Onda 3 ser fechada (Fase 8 — Docker + headers de segurança no nginx). Pentest externo (DAST) após Onda 5.

---

> **Status deste documento**: sumário consolidado para alinhar com a auditoria do guep-crm. O plano detalhado por achado está distribuído em [`ai-memory/roadmap/appsec-pendencias.md`](./ai-memory/roadmap/appsec-pendencias.md) (Fix-alvo + marco). As guidelines invariantes que TODA geração de código deve seguir estão em [`ai-memory/architecture/appsec-guidelines.md`](./ai-memory/architecture/appsec-guidelines.md).
