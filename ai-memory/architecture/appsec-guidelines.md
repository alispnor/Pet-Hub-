# Pet Hub — AppSec Guidelines (Security by Design)

> **Status:** [approved] · v1.0 · 2026-05-14
> **Owner:** Ali (Tech Lead + AppSec Lead)
> **Aplicabilidade:** Todo código gerado, refatorado ou revisado neste repositório.

---

## 0. Regra de ouro

Segurança é **integrada nativamente na arquitetura**, nunca aplicada como remendo posterior. Nenhuma PR é aceita sem cumprir as regras desta página. Em caso de conflito com prazos ou conveniência, **a regra de segurança vence**.

Toda resposta de IA que gerar código novo neste projeto deve terminar com uma seção **"🛡️ OWASP & Security Checkpoint"** explicitando:
1. Quais vulnerabilidades OWASP foram mitigadas naquele código.
2. Quais premissas de infra / variáveis de ambiente o código assume como verdadeiras para que a segurança descrita se sustente.

---

## 1. Backend & APIs (OWASP Web Top 10 + API Security Top 10)

### 1.1 Validação estrita de input (API3:2023 Broken Object Property Level Authorization, A03:2021 Injection)

- Todo endpoint **REST público ou autenticado** valida no ponto de entrada: tipo, formato, tamanho, faixa numérica, charset/whitelist quando aplicável.
- Stack Pet Hub: `@Valid` + Bean Validation (JSR-380) em DTOs (`@NotBlank`, `@Size`, `@Pattern`, `@Min/@Max`, `@Email`, validadores customizados como `@Cpf`, `@Cep`).
- Path/Query params anotados com `@Validated` na classe e `@Pattern`/`@Min` direto no parâmetro.
- `GlobalExceptionHandler` traduz `MethodArgumentNotValidException` em **RFC 7807** sem expor internals (sem nome de classe, sem stack trace, sem caminho de filesystem).
- **Proibido:** receber `Map<String, Object>` ou `JsonNode` como request body em endpoint público — sempre DTO tipado.

### 1.2 Controle de acesso explícito por recurso (API1:2023 BOLA + API3:2023 BOPLA)

- Toda rota privada que opera sobre um recurso pertencente a um usuário **valida explicitamente** que `recurso.ownerUserId == auth.userId` antes de devolver/alterar.
- Padrão atual do Pet Hub: helper `loadOwnedXxx(usuarioId, id)` no service, que carrega o recurso e dispara `ForbiddenException` (HTTP 403) se o dono não bate. **Replicar esse padrão em qualquer novo recurso `/me/*`.**
- BOPLA: DTO de saída ≠ entidade. Nunca serializar entidade JPA direto — sempre passar por mapper (MapStruct) que escolhe os campos seguros. Campos sensíveis (`gateway_token`, `cpf` em texto, `senha_hash`, `refresh_token`) **nunca** podem aparecer em response.
- Roles validadas via `@PreAuthorize` no service ou nos endpoints admin (`@PreAuthorize("hasAnyRole('ADMIN_LOJA','GERENTE')")`). Não confiar apenas em `SecurityFilterChain` por path matching para autorização de role — pode haver mismatch de prefixo.

### 1.3 Autenticação, JWT e segredos (API2:2023 Broken Authentication, A02:2021 Cryptographic Failures, A07:2021 Identification and Authentication Failures)

- JWT valida **assinatura, `exp`, `iss`, `aud`, `nbf`** (quando presente). Aceitar token sem `iss`/`aud` correto é falha aberta para tokens de outro ambiente/serviço.
- Algoritmo fixo na verificação (HS256 ou RS256) — **nunca** confiar no header `alg` do token (CVE clássica `alg=none`).
- Access token TTL ≤ 15 min; refresh TTL ≤ 7 dias, com rotação a cada uso. Refresh revogado em logout, troca de senha e reset.
- Senhas: bcrypt cost ≥ 12 (atual). Migração futura para `argon2id` quando o time autorizar — biblioteca: `org.bouncycastle` ou `de.mkammerer:argon2-jvm`.
- Segredos (`JWT_SECRET`, `APP_ENCRYPTION_KEY`, `DATABASE_PASSWORD`, `MERCADOPAGO_TOKEN`, etc.) vêm exclusivamente de variáveis de ambiente. **Proibido** hardcode em `application.yml` em qualquer profile que não seja `dev` local. Em `dev`, default só para placeholders óbvios (`changeme`, `dev-secret`) que falham loud-and-clear se vazarem para prod.
- Reset de senha por email tem link com token de uso único, TTL ≤ 30 min, e invalida sessões ativas ao concluir.

### 1.4 Proteção de borda: rate limit, headers, CORS (API4:2023 Unrestricted Resource Consumption, A05:2021 Security Misconfiguration)

- Rate limit por IP + rota em endpoints sensíveis (`/auth/login`, `/auth/register`, `/auth/refresh`, `/checkout/place-order`, qualquer endpoint que dispare email/SMS).
  - Atual: `LoginRateLimitFilter` in-memory por IP (1 instância). **Pendência:** migrar para Bucket4j+Redis quando houver >1 réplica — ver `ai-memory/roadmap/appsec-pendencias.md` item RL-1.
- Headers via Spring Security default (`X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Cache-Control: no-store` em endpoints auth). Quando houver frontend servido por nginx, adicionar:
  - `Strict-Transport-Security: max-age=31536000; includeSubDomains; preload`
  - `Content-Security-Policy` estrita (ver seção 2.3)
  - `Referrer-Policy: strict-origin-when-cross-origin`
  - `Permissions-Policy: geolocation=(), microphone=(), camera=()` (ajustar para necessidades reais)
- CORS: lista **explícita** de origins em `app.cors.allowed-origins` por profile. **Nunca** `*` em prod. Em startup, profile `prod` deve falhar-rápido se a lista contiver `*` ou wildcards (`*.exemplo.com`). Ver pendência CORS-1.
- `Access-Control-Allow-Credentials: true` só com origins explícitas, nunca com `*`.

### 1.5 Prevenção de injeção e SSRF (A03:2021 Injection, A10:2021 SSRF)

- **SQL/JPA:** somente JPA, Spring Data, Query Methods ou `@Query` com parâmetros nomeados/posicionais. **Proibido** concatenar string em `EntityManager.createNativeQuery` ou `JdbcTemplate.queryForObject`. Se for inevitável, validar/sanitizar o parâmetro contra whitelist antes.
- **Comandos OS / Reflection:** proibido `Runtime.exec` ou `ProcessBuilder` com input do usuário. Se necessário (export, conversão), usar lista de argumentos fixa e validar input contra whitelist.
- **Path traversal:** uploads (fotos de pet, anexos) gravados em diretório fixo (`${app.uploads.dir}`), nome de arquivo gerado server-side (`UUID`), nunca o nome enviado pelo cliente. Extensão validada contra whitelist (`.jpg`, `.png`, `.webp`). Magic bytes verificados quando possível.
- **SSRF:** chamadas a URLs fornecidas por usuário (futuro: webhook de notificação, callback de gateway) devem passar por validador que:
  1. Resolve DNS server-side.
  2. Rejeita `127.0.0.0/8`, `10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`, `169.254.0.0/16`, `::1`, `fc00::/7`, `fe80::/10`.
  3. Aceita apenas `https://` (sem `http://`, `file://`, `gopher://`).
  4. Whitelist de hosts em integrações conhecidas (ViaCEP, Correios, Mercado Pago).
- Integrações HTTP (`RestClient`, `WebClient`) configuradas com timeout (`connectTimeout`/`readTimeout`) e número máximo de redirects (≤ 3, ou redirect manual).

### 1.6 Criptografia em repouso e em trânsito (A02:2021 Cryptographic Failures)

- PII sensível (CPF, dados clínicos do Pet Diary, futuros documentos) cifrada com **AES-256-GCM** via `AesGcmCipher` + `EncryptedStringConverter` (padrão já em uso no `customer`).
- Para lookup determinístico (ex.: encontrar CPF duplicado), usar coluna paralela com **SHA-256(cpf + pepper)** — nunca SHA-256 puro. Pepper em variável de ambiente.
- Chaves de criptografia (`app.encryption.key`) rotacionáveis com versionamento embutido no ciphertext (ex.: `v1:base64(iv+ct+tag)`). Hoje é v1 implícito — quando rotacionar, manter compatibilidade lendo v1 e gravando v2.
- Cartões: **nunca** persistir PAN ou CVV. Persistir apenas `gateway_token`, `bandeira`, `ultimos_4`, `validade_mes`, `validade_ano`. Tokenização cliente-side (Mercado Pago Elements / Stripe Elements) é mandatória em produção.
- TLS: produção exige TLS 1.3 (com fallback TLS 1.2). Nunca SSLv3/TLS 1.0/1.1. Terminação no nginx/load balancer; backend pode rodar HTTP plain atrás do LB se ambiente for confiável (VPC privada), nunca exposto direto.

### 1.7 Logging e observabilidade sem leakage (A09:2021 Security Logging and Monitoring Failures)

- Logs estruturados (JSON, Logback). Cada log de request inclui `traceId`/`userId` (quando autenticado), nunca `password`, `cpf` em claro, `token`, `cvv`, `pan`.
- Masking obrigatório em DTOs com `@ToString.Exclude` (Lombok) em campos sensíveis. Padrão já em uso na entidade `TentativaPagamento`.
- Stack traces **nunca** vão para o response do cliente — só para o log server-side. Cliente recebe RFC 7807 com `detail` genérico em 5xx.
- Eventos de segurança auditados em tabela própria (a entrar na Fase 6 com admin): login sucesso, login falha, reset de senha, criação de role, criação/cancelamento de pedido, mudança de chave de criptografia.

### 1.8 Idempotência e race conditions (API6:2023 Server Side Request Forgery / business logic, A04:2021 Insecure Design)

- Operações que cobram dinheiro, enviam email/SMS, ou alteram estado externo aceitam `Idempotency-Key` (header) ou campo equivalente. Padrão atual em `/checkout/place-order` com `UNIQUE` em `idempotency_key` no banco — replicar.
- Reservas de estoque com lock pessimista (`@Lock(LockModeType.PESSIMISTIC_WRITE)`) ou versão otimista (`@Version`). Ver Fase 4.
- Endpoints de "criar único por usuário" (ex.: marcar endereço/forma de pagamento como padrão) usam partial unique index no Postgres, não regra checada no service — DB é a fonte da verdade.

---

## 2. Frontend Web (OWASP Web Top 10 + Proactive Controls)

### 2.1 Mitigação absoluta de XSS (A03:2021 Injection - XSS)

- Angular sanitiza por default; **nunca** usar `bypassSecurityTrustHtml` etc. sem revisão e DOMPurify.
- Para HTML vindo de fonte externa (descrição de produto rica, mensagens de chat do bot AI): server-side sanitization no backend (`OWASP Java HTML Sanitizer` com policy whitelist) + DOMPurify no client.
- `[innerHTML]` proibido sem pipe `safeHtml` que usa DOMPurify.
- Atributos dinâmicos perigosos (`[href]`, `[src]`, `[style]`) validados — proibir `javascript:`, `data:` (exceto data URIs de imagem para preview de upload).

### 2.2 Gerenciamento seguro de estado e sessão (A07:2021)

- **Access token** vive em memória do `AuthService` (signal/state), **não** em `localStorage`/`sessionStorage`. Perda no F5 é aceitável — recarrega via refresh cookie.
- **Refresh token** em cookie `HttpOnly + Secure + SameSite=Strict` (ou `Lax` se houver fluxo de redirect OAuth). Domínio explícito, `Path=/api/v1/auth/refresh` (escopo mínimo).
- CSRF: como access token é Bearer header (origem cross-site não pode setar headers customizados em request com credentials sem CORS preflight), CSRF clássico é mitigado. Para o refresh em cookie, exigir `SameSite=Strict` + double-submit pattern se houver formulários cross-origin.
- Logout faz: 1) `POST /auth/logout` → backend revoga refresh + zera cookie; 2) limpa state local.

### 2.3 Content Security Policy (A05:2021)

CSP servida pelo nginx (Fase 8). Base recomendada para storefront/admin Angular:

```
Content-Security-Policy:
  default-src 'self';
  script-src 'self' 'nonce-{REQUEST_NONCE}';
  style-src 'self' 'nonce-{REQUEST_NONCE}';
  img-src 'self' data: https://*.pethub.com https://images.unsplash.com;
  connect-src 'self' https://api.pethub.com https://api.mercadopago.com;
  frame-ancestors 'none';
  base-uri 'self';
  form-action 'self';
  object-src 'none';
  upgrade-insecure-requests;
```

- **Sem** `unsafe-inline` ou `unsafe-eval`. Angular suporta CSP estrita com `extractCss: true` e nonce por request.
- `frame-ancestors 'none'` substitui `X-Frame-Options: DENY` (mais estrito).
- Reportar violações: `report-uri /csp-report` (endpoint backend que apenas loga, nunca confia em payload).

### 2.4 Integridade e supply chain (A06:2021 Vulnerable and Outdated Components, A08:2021 Software and Data Integrity Failures)

- `npm ci` (lockfile-based) em CI; `npm audit --audit-level=high` falha o build.
- Dependências críticas (Angular, axios/fetch wrappers, libs de PDF, libs de crypto) revisadas trimestralmente.
- Scripts externos (analytics, captcha): preferir self-host. Se externo, usar SRI (`integrity="sha384-..."`).
- Backend: `mvn dependency:tree`, OWASP Dependency-Check plugin no CI (Fase 9), atualização de versões major com PR isolado.

---

## 3. Mobile — iOS / Android (OWASP Mobile Top 10)

> Pet Hub não tem app mobile no roadmap atual (apenas web responsivo). Estas regras ficam prontas para quando um app for iniciado.

### 3.1 M1 Improper Credential Usage / M9 Insecure Data Storage

- iOS: tokens, refresh, PII em **Keychain** com `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly` (ou `WhenPasscodeSet` para itens mais sensíveis).
- Android: **EncryptedSharedPreferences** (`androidx.security:security-crypto`) ou Keystore direto para chaves. Nunca `SharedPreferences` simples para tokens.
- Nada de PII em `UserDefaults` (iOS) ou `SharedPreferences` (Android) sem criptografia.

### 3.2 M5 Insecure Communication

- HTTPS TLS 1.3 obrigatório. iOS: ATS (App Transport Security) sem exceções. Android: `network_security_config.xml` com `cleartextTrafficPermitted="false"` e `pin-set` para domínios críticos.
- SSL Pinning avaliado para flows críticos (login, checkout). Backup pin obrigatório.
- Certificados auto-assinados proibidos, inclusive em build de dev distribuído por TestFlight/internal.

### 3.3 M2 Inadequate Supply Chain Security / M7 Insufficient Binary Protection

- Release: ProGuard/R8 com obfuscation + shrinker (Android). Bitcode + symbol stripping (iOS).
- Debug logs removidos em release (`isDebuggable=false`, `BuildConfig.DEBUG` check, ou Timber com `Timber.plant` só em debug).
- Stack traces, requisições, tokens, PII **nunca** em log de produção.
- Root/jailbreak detection se app manipula dinheiro ou PII médica (warn, não block — UX).

### 3.4 M4 Insufficient Input Validation / M10 Insufficient Cryptography

- Validação client-side é UX; **toda** decisão de autorização e validação business é server-side.
- Criptografia: usar bibliotecas nativas (CryptoKit, Android Tink). Nunca implementar AES/HMAC à mão.

---

## 4. Formato de entrega — Security Checkpoint

Ao gerar código, sempre incluir no final:

```markdown
## 🛡️ OWASP & Security Checkpoint

### Vulnerabilidades mitigadas
- **API1:2023 BOLA** — endpoint `/me/orders/{id}` carrega o pedido e valida `pedido.usuario.id == auth.id`; tentativa de outro user retorna 403.
- **API3:2023 BOPLA** — response DTO `OrderResponse` omite `paymentToken` e `internalNotes` da entidade.
- **A03:2021 Injection (SQL)** — repositório usa Spring Data Query Method (parâmetros bindados).
- **A02:2021 Cryptographic Failures** — CPF persistido cifrado AES-GCM via converter existente.
- **A09:2021 Logging Failures** — `@ToString.Exclude` em `paymentToken`; `log.info` registra `orderNumber` mas não detalhes do cartão.

### Premissas de infra / env
- `JWT_SECRET` ≥ 256 bits em env, distinto por ambiente, rotacionável.
- `APP_ENCRYPTION_KEY` ≥ 32 bytes base64; backup em vault (Vault/AWS KMS).
- `app.cors.allowed-origins` configurado **sem** wildcards no profile `prod`.
- TLS 1.3 terminado no nginx upstream; backend não exposto direto.
- Postgres atrás de VPC privada; conexão exige TLS server-side.
- Redis com `requirepass` setado e ACL bloqueando comandos perigosos (`FLUSHALL`, `CONFIG`, `DEBUG`).
```

A profundidade do checkpoint é proporcional ao escopo. Patch trivial (renomear variável) pode marcar "N/A — sem alteração de superfície". Endpoint novo, mudança de auth, ou integração externa precisam do bloco completo.

---

## 5. Aplicação retroativa e exceções

- **Código já mergeado em `main` antes desta página (Fases 0–3) não é reaberto retroativamente** — vai para `ai-memory/roadmap/appsec-pendencias.md` e endereçado quando o módulo for tocado de novo.
- Toda exceção a esta página (ex.: "rate limit pode ficar in-memory porque só temos 1 réplica") precisa ser registrada como ADR em `ai-memory/decisions/` com prazo de expiração ou condição de revisão.
- Hierarquia de instruções: instruções explícitas do Ali > esta página > defaults do framework. Se Ali pedir algo que viola esta página, primeiro perguntar; nunca silenciosamente baixar a régua.

---

## 6. Referências

- OWASP Web Top 10 (2021): https://owasp.org/Top10/
- OWASP API Security Top 10 (2023): https://owasp.org/API-Security/editions/2023/en/0x11-t10/
- OWASP Mobile Top 10 (2024): https://owasp.org/www-project-mobile-top-10/
- OWASP ASVS 4.0: https://owasp.org/www-project-application-security-verification-standard/
- OWASP Proactive Controls: https://owasp.org/www-project-proactive-controls/
- Spring Security Reference: https://docs.spring.io/spring-security/reference/
- Bean Validation 3.0 (Jakarta): https://beanvalidation.org/3.0/

Cheat sheets relevantes:
- Authentication / JWT / Session Management
- REST Security
- Input Validation
- Cryptographic Storage
- Logging
- Secure Headers / CSP
- Mass Assignment
