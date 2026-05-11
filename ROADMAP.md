# 🐾 Pet Hub — Roadmap

> Projeto de e-commerce pet tech em 12 fases progressivas. Cada fase entrega uma capacidade completa do sistema, com habilidades técnicas demonstráveis.
>
> Plano detalhado, prompts por fase, modelo de domínio e endpoints estão em [`ai-memory/roadmap/plano-completo.md`](./ai-memory/roadmap/plano-completo.md) (fonte de verdade).

## Estratégia de execução

| Marco | Fases | Esforço estimado | O que entrega |
|---|---|---|---|
| **MVP local** | 0 → 5 | ~80h | E-commerce funcional rodando local com storefront Angular |
| **Portfolio público** | + 6, 7, 8, 9 | ~160h | Admin, NF-e, Docker e arquitetura event-driven |
| **Completo** | + 10, 11, 12 | ~250h | Cloud-native, observabilidade e IA generativa |

Status: ⬜ Pendente · 🚧 Em andamento · ✅ Concluída

---

## Fase 0 — Bootstrap do Monorepo · ✅

**Objetivo:** Estabelecer infraestrutura do repositório (docs, configs, ADRs, templates, estrutura de pastas) sem código de aplicação.

**Entregáveis:**
- README, ROADMAP, ARCHITECTURE, CHANGELOG, CONTRIBUTING
- `docs/brand/BRAND.md` com paleta e tipografia
- ADRs 0001 (monorepo), 0002 (arquitetura evolutiva), 0003 (ecossistema Pet Diary + Pet Hub)
- `.gitignore` unificado, `.editorconfig`, `Makefile` com placeholders
- Templates GitHub: PR template, issue templates (bug + feature)
- GitHub Actions skeleton (`ci.yml` placeholder)
- Estrutura de pastas `backend/`, `frontend/{storefront,admin}/`, `infrastructure/{docker,k8s,helm,terraform}/`, `docs/{adr,diagrams,brand}/`

**Habilidades:** Git workflow, monorepo strategy, documentação técnica, Conventional Commits.

**Esforço:** 2–3h.

---

## Fase 1 — Backend Core: Catálogo + Identidade · 🚧

> **Sessão pausada em 2026-05-11 (~85%).** Falta validar com `mvn clean install` e completar testes. Checklist em [`ai-memory/roadmap/fase-1-pendencias.md`](./ai-memory/roadmap/fase-1-pendencias.md).

**Objetivo:** Backend Spring Boot multi-module com autenticação JWT e catálogo de produtos.

**Entregáveis:**
- Maven multi-module: `common`, `identity`, `catalog`, `application`
- Clean Architecture em cada módulo (domain/application/infrastructure)
- JWT access token (15min) + refresh token (7 dias) com rotação
- BCrypt cost 12, rate limit Bucket4j em `/auth/login`
- Endpoints: register/login/refresh/logout/me + CRUD catálogo (público + admin)
- PostgreSQL 16 + Flyway (V1 schema identity, V2 schema catalog, V3 seed data)
- 6 categorias e 20 produtos pet tech com NCM realista
- Spring Security 6, SpringDoc OpenAPI (Swagger UI)
- Testes unitários (Mockito) e integração (`@SpringBootTest` + Testcontainers)
- `RFC 7807` (Problem Details) via `GlobalExceptionHandler`

**Habilidades:** Java 21, Spring Boot 3, JPA, SQL, REST, Clean Architecture, segurança OWASP.

**Esforço:** 15–20h.

---

## Fase 2 — Cliente: Pets, Endereços, Formas de Pagamento · ⬜

**Objetivo:** Módulo `customer` com dados completos do cliente, compliance LGPD e tokenização de cartões (PCI-DSS).

**Entregáveis:**
- Entidades: `PerfilCliente` (com CPF criptografado AES-GCM), `Pet`, `Endereco`, `FormaPagamento`
- Endpoints CRUD: profile, pets, endereços, formas de pagamento
- Lógica de "endereço padrão" (entrega e cobrança separados)
- Integração ViaCEP com cache Redis (24h)
- `PaymentGateway` interface + `MockPaymentGateway` (tokenização cliente-side)
- Validações: CPF (algoritmo), CEP, data nascimento, telefone BR
- Testes de isolamento (cliente A não vê dados do cliente B → 403)
- Logs com masking de PII

**Habilidades:** Criptografia em repouso, compliance LGPD/PCI-DSS, validações customizadas, AttributeConverter JPA.

**Esforço:** 12–15h.

---

## Fase 3 — Carrinho, Frete, Checkout e Pagamento · ⬜

**Objetivo:** Fluxo completo de compra — a fase mais crítica do e-commerce.

**Entregáveis:**
- Módulos: `cart` (Redis), `pricing` (cupons/promoções/impostos), `shipping`, `payment`, `checkout`
- Carrinho persistido em Redis (TTL 30 dias)
- Cálculo de frete: Correios + tabelado fallback + motoboy local SP
- Cupons (PERCENTUAL/VALOR_FIXO), promoções por categoria/produto/marca, regras de imposto por NCM/UF
- `PaymentGateway`: Mock (90% aprova) + Mercado Pago Sandbox
- PIX (QR Code via ZXing), boleto (PDF mock), cartão tokenizado
- Endpoints: `/checkout/preview` e `/checkout/place-order`
- Cache de cotações de frete em Redis (1h)
- Testes com WireMock para integrações externas

**Habilidades:** Transações, idempotência, BigDecimal, integração com APIs externas, strategy pattern.

**Esforço:** 20–25h.

---

## Fase 4 — Pedidos, Timeline, Estoque · ⬜

**Objetivo:** Materialização de pedidos com máquina de estados e gestão de estoque com reservas.

**Entregáveis:**
- Módulos: `order` (Pedido, PedidoItem, PedidoEvento) e `inventory` (Estoque, Movimentação, Reserva)
- Máquina de estados: PENDENTE_PAGAMENTO → PAGAMENTO_APROVADO → SEPARACAO → EM_TRANSPORTE → ENTREGUE (+ ramos de cancelamento/devolução)
- Hooks de transição que disparam efeitos (confirma reserva, baixa estoque, persiste evento)
- Reserva de estoque com expiração de 15min (`@Scheduled` libera expiradas)
- Geração de número de pedido sequencial por ano (formato `PH-YYYY-NNNNNN`)
- Endpoint `/orders/{numero}/timeline` com tempo decorrido e comparativo com média
- Testes de concorrência em estoque (lock pessimista)

**Habilidades:** Workflow engines, máquinas de estado, controle de concorrência, jobs agendados.

**Esforço:** 15–20h.

---

## Fase 5 — Frontend Storefront (Angular) · ⬜

**Objetivo:** Loja virtual completa consumindo APIs das Fases 1–4.

**Entregáveis:**
- Angular 17+ Standalone Components, TypeScript strict, TailwindCSS
- 19 páginas: home, catálogo, detalhe produto, busca, login/cadastro, minha conta, pets, endereços, cartões, pedidos, carrinho, 4 etapas de checkout, sucesso
- Componentes reutilizáveis: ProductCard, OrderTimeline (visual destaque), CardForm (Stripe/MP Elements)
- AuthService + Interceptors (Bearer token, 401 redirect, 500 toast)
- Tokenização cliente-side de cartão (PCI compliance)
- Acessibilidade WCAG AA, SEO com meta tags + Schema.org
- Testes Jasmine (unit) + Cypress (E2E do checkout)

**Habilidades:** Angular Signals + RxJS, lazy loading, formulários reativos, design responsivo, acessibilidade.

**Esforço:** 25–30h.

---

## Fase 6 — Frontend Admin (Back-Office) · ⬜

**Objetivo:** Painel administrativo separado do storefront com dashboards, CRUDs e relatórios.

**Entregáveis:**
- Segundo app Angular em `frontend/admin/` com login próprio
- 23 telas: dashboard, produtos, categorias, estoque, pedidos, cupons, promoções, regras de imposto, NF-e (Fase 7), clientes, relatórios, configurações
- Dashboard com ApexCharts (KPIs, donut por status, top produtos, estoque crítico)
- Tabelas com ag-Grid (edição inline, bulk actions, filtros avançados)
- Drag-and-drop de imagens de produtos
- Geração de etiqueta de envio (PDF via jsPDF)
- Notificações em tempo real (WebSocket/SSE) para novos pedidos
- Audit log de todas as ações admin
- Roles: ROLE_OPERADOR, ROLE_GERENTE, ROLE_ADMIN_LOJA com guards e directives

**Habilidades:** Dashboards complexos, data grids, gráficos, exportação (Excel/PDF), permissionamento granular.

**Esforço:** 25–30h.

---

## Fase 7 — NF-e e Emissão Fiscal · ⬜

**Objetivo:** Emissão de NF-e (modelo 55) com abordagem dupla: mock estrutural + Focus NFe sandbox.

**Entregáveis:**
- Módulo `invoice` com entidades: `NotaFiscal`, `NotaFiscalItem`, `DadosEmissor`, `CertificadoDigital`
- `NFeProvider` abstrato + `MockNFeProvider` (XML válido conforme schema v4.00) + `FocusNFeProvider` (sandbox)
- Cálculo de CFOP por UF origem/destino + impostos por NCM
- Geração de DANFE PDF (template com código de barras da chave de acesso)
- Endpoints: emitir, cancelar (24h), reemitir, baixar XML/DANFE
- Consumo do evento `OrderPaid` (síncrono nesta fase; assíncrono via Kafka na Fase 9)
- Frontend admin: tela `/admin/fiscal/notas` funcional

**Habilidades:** Compliance fiscal BR, integração XML, JAXB, geração de PDF, regulação SEFAZ.

**Esforço:** 15–20h.

---

## Fase 8 — Docker · ⬜

**Objetivo:** Containerização production-grade de toda a stack.

**Entregáveis:**
- `backend/Dockerfile` multi-stage com Maven cache e distroless Java 21 (< 200MB)
- `frontend/storefront/Dockerfile` + `frontend/admin/Dockerfile` com Nginx alpine (< 60MB cada)
- `nginx.conf`: SPA fallback, gzip+brotli, cache imutável, headers de segurança (CSP, HSTS)
- `docker-compose.yml` + `.override.yml` (dev hot reload) + `.prod.yml`
- Serviços: postgres + redis + backend + storefront + admin + mailhog + pgadmin
- Init script para extensão `pgvector` (preparação Fase 10)
- `Makefile` completo: up/up-dev/up-prod/down/logs/backend-shell/db-shell etc.
- Healthchecks reais, usuário não-root, logs JSON via Logback
- `DOCKER.md` com troubleshooting

**Habilidades:** Multi-stage builds, otimização de imagens, orquestração compose, segurança de containers.

**Esforço:** 4–6h.

---

## Fase 9 — Kafka (Eventos de Domínio) + RabbitMQ (Notificações) · ⬜

**Objetivo:** Evolução para arquitetura event-driven com padrão Outbox e mensageria especializada.

**Entregáveis:**
- Extração de `order` e `notification` como deployables separados
- Kafka com Schema Registry + Avro: tópicos `pethub.orders.events`, `pethub.inventory.events`, `pethub.payments.events`, `pethub.invoices.events`
- Padrão Outbox em todos os serviços que publicam eventos (tabela `outbox_events` + job)
- Idempotência: producer com `acks=all`, consumers com tabela `processed_events`
- DLT (Dead Letter Topic) para eventos com falha persistente
- RabbitMQ com topic exchange `notifications.topic` + DLX
- Bridge Kafka → RabbitMQ para notificações
- Templates de email via Thymeleaf, preferências de notificação com cache Redis
- Testes E2E da saga (criar pedido → confirmar → estoque → NF-e → notificação)

**Habilidades:** Event-driven architecture, sagas coreografadas, padrões de mensageria (Outbox, idempotência, DLQ), Avro/Schema Registry.

**Esforço:** 20–25h.

---

## Fase 10 — IA Generativa (Recomendações + Chatbot) · ⬜

**Objetivo:** `recommendation-service` com Anthropic Claude API, RAG via PGVector e function calling.

**Entregáveis:**
- Spring AI + provider abstrato (`LLMProvider` → Anthropic/OpenAI)
- Endpoints: `/ai/recommendations`, `/ai/chat` (multi-turno com Redis), `/ai/products/{id}/generate-description`, `/ai/reviews/analyze`
- RAG do catálogo: job de embeddings + tabela `product_embeddings` (PGVector)
- Re-indexação automática via Kafka event quando produto é atualizado
- Chatbot com function calling: `consultar_pedido`, `buscar_produtos`, `politica_de`
- Job diário de análise de sentimento de reviews
- Frontend: botão flutuante de chat (SSE streaming), seção "Recomendado para [pet]" na home
- Rate limiting (10 req/min/user), logs de custo, proteção contra prompt injection
- `AI.md` com custos estimados, exemplos de prompts e considerações éticas

**Habilidades:** Integração com LLMs, prompt engineering, RAG, function calling, embeddings, uso responsável de IA.

**Esforço:** 12–15h.

---

## Fase 11 — Kubernetes · ⬜

**Objetivo:** Manifests K8s production-grade para toda a stack.

**Entregáveis:**
- Estrutura `infrastructure/k8s/` com base + overlays (dev/prod) via Kustomize
- Helm chart umbrella `pethub/` com subcharts por serviço
- Deployments com probes (liveness, readiness, startup), recursos, HPA, PDB
- StatefulSets: Postgres (com PVC + extensão pgvector), Kafka (Strimzi ou bitnami KRaft), RabbitMQ
- Service Mesh básico (sem Istio; apenas Service + Ingress)
- Ingress Nginx com host `pethub.local`, paths roteados
- SecurityContext: runAsNonRoot, readOnlyRootFilesystem
- Anotações Prometheus para scrape
- Scripts `k8s-setup.sh` e `k8s-teardown.sh`
- `K8S.md` com troubleshooting

**Habilidades:** Kubernetes (pods, deployments, services, ingress, HPA, StatefulSets), Kustomize, Helm.

**Esforço:** 12–15h.

---

## Fase 12 — Cloud + CI/CD + Observabilidade · ⬜

**Objetivo:** Deploy em cloud pública (AWS ou Oracle), CI/CD automatizado e observabilidade dos três pilares.

**Entregáveis:**
- Terraform em `infrastructure/terraform/` com módulos: vpc, eks/oke, rds, msk, ecr, iam
- Backend remoto (S3+DynamoDB ou OCI Object Storage)
- GitHub Actions: `ci.yml`, `cd-dev.yml`, `cd-prod.yml` (com OIDC, scan Trivy, approval prod)
- Stack de observabilidade: Prometheus + Grafana + Loki + Tempo/Jaeger + OpenTelemetry agent
- Dashboards Grafana: JVM, HTTP latency, Kafka lag, RabbitMQ depth, business metrics
- Métricas de negócio com Micrometer: pedidos_criados_total, valor_vendas_total, tempo_pedido_pago_seconds, etc.
- Trace correlation IDs propagados via Kafka/RabbitMQ headers
- HTTPS obrigatório, IAM com least privilege, network policies
- `CLOUD.md` com custos estimados e instruções de destroy
- README final repaginado + ARCHITECTURE com modelo C4
- Vídeo demo + script (`DEMO.md`)

**Habilidades:** Terraform/IaC, cloud arquitetura, GitHub Actions, OIDC, observabilidade SRE (métricas/logs/traces), gestão de custos.

**Esforço:** 15–20h.

---

## Documentos relacionados

- [`ai-memory/roadmap/plano-completo.md`](./ai-memory/roadmap/plano-completo.md) — plano estratégico V2 detalhado (fonte de verdade)
- [`ai-memory/roadmap/prompt-execucao-fase-0-1.md`](./ai-memory/roadmap/prompt-execucao-fase-0-1.md) — prompt operacional para executar Fases 0 + 1
- [`ai-memory/roadmap/prompt-ativacao-sessao.md`](./ai-memory/roadmap/prompt-ativacao-sessao.md) — bloco para colar no início de cada sessão
- [`ARCHITECTURE.md`](./ARCHITECTURE.md) — arquitetura técnica do sistema
- [`docs/adr/`](./docs/adr/) — Architecture Decision Records
