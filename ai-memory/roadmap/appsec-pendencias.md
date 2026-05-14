# AppSec — Pendências de código já mergeado

> **Status:** [draft] · 2026-05-14
> **Owner:** Ali (AppSec Lead)
> **Escopo:** Achados de revisão de segurança em código já presente em `origin/main` (Fases 0–3) que **não bloqueiam** desenvolvimento atual mas precisam ser endereçados quando o módulo correspondente for tocado novamente, ou no marco indicado.

Cada item tem: **ID · Severidade · Categoria OWASP · Local · Estado atual · Fix-alvo · Quando endereçar**.

A regra geral é: **não reabrimos código mergeado só por estes achados**, mas qualquer PR futuro que tocar o módulo afetado tem como pré-requisito fechar o achado correspondente. Os marcos abaixo indicam o **prazo máximo** (fase em que o fix deixa de ser opcional).

---

## JWT-1 · Tokens não validam `iss` e `aud`

- **Severidade:** Médio
- **OWASP:** API2:2023 Broken Authentication
- **Local:** `backend/identity/src/main/java/com/alispnor/pethub/identity/infrastructure/security/JwtAuthenticationFilter.java` + `JwtService` (verifica `exp` e assinatura HS256 mas não `iss`/`aud`).
- **Estado atual:** Token assinado com `JWT_SECRET` é aceito independente do issuer/audience. Em um cenário com múltiplos serviços compartilhando a mesma chave (futuro Pet Diary, futuro back-office), um token de um serviço passaria no outro.
- **Fix-alvo:**
  1. Adicionar `iss=pethub-api` e `aud=pethub-storefront` (e `pethub-admin` em paralelo) ao gerar tokens.
  2. Validar `iss` e `aud` no filter; rejeitar com 401 se mismatch.
  3. Documentar em `application.yml` (`app.jwt.issuer`, `app.jwt.audiences`).
  4. Fixar `alg` esperado em verificação (HS256) — não confiar no header do token.
- **Quando endereçar:** Antes do storefront ir para staging (Fase 5) **ou** antes de qualquer integração inter-serviço (Pet Diary, microserviço de NF-e na Fase 7).

---

## RL-1 · Rate limit em memória não sobrevive a múltiplas réplicas

- **Severidade:** Médio (alto no momento que houver >1 réplica)
- **OWASP:** API4:2023 Unrestricted Resource Consumption
- **Local:** `backend/identity/src/main/java/com/alispnor/pethub/identity/infrastructure/security/LoginRateLimitFilter.java` — usa `ConcurrentHashMap` por IP.
- **Estado atual:** Funciona em 1 instância. Em N instâncias atrás de LB, cada uma tem o próprio contador → atacante pode fazer N×5 tentativas.
- **Fix-alvo:** Migrar para Bucket4j com backend Redis (`bucket4j-redis`), chave `ratelimit:login:{ip}` ou `ratelimit:login:{ip}:{email}`.
- **Quando endereçar:** Fase 11 (cloud-native, múltiplas réplicas) **ou** assim que entrar 2ª instância em qualquer ambiente. Antes disso é puramente dívida de escalabilidade.

---

## CORS-1 · Falta validação fail-fast de CORS em profile `prod`

- **Severidade:** Médio
- **OWASP:** A05:2021 Security Misconfiguration
- **Local:** `backend/identity/src/main/java/com/alispnor/pethub/identity/infrastructure/security/SecurityConfig.java` — `corsConfigurationSource()` usa `corsProperties.allowedOrigins()` sem validação.
- **Estado atual:** Se `APP_CORS_ALLOWED_ORIGINS=*` for setado em prod por engano, o backend aceita silenciosamente. Combinado com `Allow-Credentials: true`, é uma porta aberta.
- **Fix-alvo:**
  1. `@PostConstruct` em `SecurityConfig` (ou `ApplicationRunner`) que, no profile `prod`, verifica `allowedOrigins` e dispara `IllegalStateException` se contém `*` ou wildcards.
  2. Em `dev`/`test`, permitir `*` mas logar WARN.
- **Quando endereçar:** Antes do primeiro deploy em ambiente compartilhado (staging/prod). Se Ali não tem staging hoje, vai junto com a Fase 8 (Docker) ou Fase 11 (Cloud).

---

## SEC-1 · Headers de segurança incompletos no backend

- **Severidade:** Baixo-Médio
- **OWASP:** A05:2021 Security Misconfiguration
- **Local:** `SecurityConfig` — usa defaults do Spring Security mas não declara HSTS, CSP, Referrer-Policy, Permissions-Policy.
- **Estado atual:** Spring Security já manda `X-Content-Type-Options`, `X-Frame-Options: DENY`, `Cache-Control: no-cache`. Falta o resto.
- **Fix-alvo:** Adicionar via `http.headers(headers -> ...)`:
  - `strictTransportSecurity` com `maxAgeInSeconds(31536000).includeSubDomains(true).preload(true)` — só em `prod` (HSTS sem HTTPS quebra dev).
  - `referrerPolicy(STRICT_ORIGIN_WHEN_CROSS_ORIGIN)`.
  - `permissionsPolicy("geolocation=(), microphone=(), camera=()")`.
  - CSP: definir no nginx (Fase 8) quando o frontend for servido — backend só responde JSON.
- **Quando endereçar:** Junto com a Fase 8 (Docker + nginx). Backend isolado não se beneficia muito de CSP.

---

## JPA-1 · Convergir AES-GCM cipher para suportar rotação de chave

- **Severidade:** Baixo
- **OWASP:** A02:2021 Cryptographic Failures
- **Local:** `backend/common/src/main/java/com/alispnor/pethub/common/security/AesGcmCipher.java`
- **Estado atual:** Ciphertext armazenado como `base64(iv + ct + tag)`. Não há marcador de versão de chave embutido.
- **Fix-alvo:** Quando a primeira rotação de chave for necessária:
  1. Prefixar ciphertext com `v{N}:` (ex.: `v1:base64(...)`, `v2:base64(...)`).
  2. Manter mapa `Map<Integer, SecretKey>` em config; ler com `vN` correto, escrever sempre com `vCurrent`.
  3. Migration backfill opcional re-cifrando registros antigos.
- **Quando endereçar:** Na primeira rotação real (não há urgência hoje; chave dev é a única). Documentar como ADR antes de fazer.

---

## PAY-1 · `response_gateway` JSONB pode acumular dados sensíveis de gateways futuros

- **Severidade:** Médio (potencial alto com Mercado Pago/Stripe reais)
- **OWASP:** A02:2021 Cryptographic Failures + PCI-DSS 3.4
- **Local:** `backend/checkout/src/main/java/com/alispnor/pethub/checkout/domain/entity/TentativaPagamento.java` — coluna `response_gateway JSONB`.
- **Estado atual:** Mock devolve só `transactionId`, `status`, `rng`, `threshold`. Nenhum PII/PAN. Mas o campo é tipado como `Map<String, Object>` — qualquer gateway real pode tacar bastante coisa ali.
- **Fix-alvo:** Antes de plugar Mercado Pago/Stripe sandbox:
  1. Definir whitelist explícita de campos a persistir (`transactionId`, `status`, `acquirerCode`, `installments`, `paymentMethodId`, timestamps).
  2. Sanitizer no `paymentGateway.charge()` wrapper que drena tudo fora da whitelist antes de persistir.
  3. Garantir que `last4`, `brand`, nunca o PAN completo, fiquem em `forma_pagamento`, não em `tentativa_pagamento`.
  4. Audit log separado registra payload completo (em log estruturado com PII masking), não no DB principal.
- **Quando endereçar:** Antes do primeiro commit que troque `MockPaymentGateway` por implementação real (Fase 7 ou antes).

---

## FLY-1 · Flyway `clean` não bloqueado para `prod`

- **Severidade:** Alto (em prod) · Baixo (hoje, sem prod)
- **OWASP:** A04:2021 Insecure Design
- **Local:** `backend/application/src/main/resources/application.yml` (`spring.flyway.*`).
- **Estado atual:** Default do Flyway 10 já é `clean-disabled: true`, mas conviria explicitar para não regredir em refactor.
- **Fix-alvo:** Setar explicitamente `spring.flyway.clean-disabled=true` em `application.yml` (todas as profiles) e adicionar override `false` apenas em `application-test.yml` se Testcontainers precisar.
- **Quando endereçar:** Próximo PR que mexa em `application.yml` ou na entrada da Fase 8.

---

## LOG-1 · Possível leakage em endpoints de erro genéricos

- **Severidade:** Baixo
- **OWASP:** A09:2021 Security Logging Failures
- **Local:** `backend/common/src/main/java/com/alispnor/pethub/common/exception/GlobalExceptionHandler.java`.
- **Estado atual:** Já trata `BusinessException` e `MethodArgumentNotValidException` com mensagens internas. Bom. Falta uma revisão de:
  1. `DataIntegrityViolationException` — pode vazar nome de constraint do Postgres (`uq_formas_pagamento_padrao`) — útil para atacante mapear schema.
  2. `HttpMessageNotReadableException` (JSON malformado) — pode incluir trecho do payload.
- **Fix-alvo:** Handlers específicos para esses dois, devolvendo mensagem genérica + logando o detalhe internamente.
- **Quando endereçar:** Próximo PR que tocar `GlobalExceptionHandler` ou quando começar a aparecer em logs de prod.

---

## OBS-1 · Falta tabela de auditoria de eventos de segurança

- **Severidade:** Baixo hoje · Médio com admin (Fase 6)
- **OWASP:** A09:2021
- **Local:** Inexistente — sem tabela `audit_log` ou equivalente.
- **Estado atual:** Eventos de login/logout/refresh ficam só no log do app (volátil em container).
- **Fix-alvo:** Tabela `eventos_seguranca` (timestamp, user_id, ip, user_agent, evento, sucesso, detalhes_json). Endpoints emitem evento via `ApplicationEventPublisher`. Admin (Fase 6) lê e expõe filtro.
- **Quando endereçar:** Fase 6 (back-office admin) — sem admin não vale a pena.

---

## DEP-1 · Falta scan de dependências no CI

- **Severidade:** Médio (passa a alto após primeiro release)
- **OWASP:** A06:2021 Vulnerable and Outdated Components
- **Local:** `.github/workflows/ci.yml` (placeholder hoje).
- **Estado atual:** Sem `dependency-check`, sem `dependabot.yml`, sem `mvn dependency:tree -Dincludes` em CI.
- **Fix-alvo:** Adicionar:
  1. OWASP Dependency-Check Maven plugin no parent POM, threshold CVSS ≥ 7 falha o build.
  2. `.github/dependabot.yml` para Maven e npm (quando frontend entrar).
  3. Trivy scan da imagem Docker (Fase 8).
- **Quando endereçar:** Fase 8 (Docker) ou Fase 9 (CI/CD), o que vier primeiro.

---

## VAL-1 · CSP/HSTS dependem do nginx que ainda não existe

- **Severidade:** Médio (ao deploy de frontend)
- **OWASP:** A05:2021
- **Local:** Não existe nginx config ainda — entra na Fase 8.
- **Fix-alvo:** Quando o `frontend/storefront/Dockerfile` for criado (Fase 5+) ou o `infrastructure/docker/nginx.conf` (Fase 8), incluir as diretivas da seção 2.3 do `appsec-guidelines.md`.
- **Quando endereçar:** Fase 5 (storefront) ou Fase 8 (Docker), o que vier primeiro.

---

## Resumo por marco

| Marco | Pendências que precisam fechar |
|---|---|
| Antes do storefront em staging | JWT-1 (iss/aud), CORS-1 (validador prod), VAL-1 (CSP nginx) |
| Antes de gateway real | PAY-1 (whitelist response_gateway) |
| Fase 6 (admin) | OBS-1 (audit log) |
| Fase 8 (Docker) | SEC-1 (headers HSTS), VAL-1 (CSP), FLY-1 (clean-disabled) |
| Fase 8/9 (CI/CD) | DEP-1 (scan de dependências) |
| Fase 11 (cloud N réplicas) | RL-1 (Bucket4j+Redis) |
| Quando houver primeira rotação de chave | JPA-1 (cipher versionado) |
| Próximo PR no GlobalExceptionHandler | LOG-1 |

Itens fechados deste documento devem ser removidos (ou movidos para `notes/appsec-historico.md` se quisermos preservar a justificativa).
