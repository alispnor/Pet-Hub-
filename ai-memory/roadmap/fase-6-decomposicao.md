# Fase 6 — Frontend Admin · Decomposição em slices

> **Status estratégico:** Em andamento · Slice 6.1 ✅ entregue em 2026-05-17. Slices 6.2-6.6 ⏳ pendentes.
> **Predecessor:** Fase 5 ✅ concluída em 2026-05-17.
> **Objetivo da fase:** Painel administrativo (back-office) separado do storefront, com dashboards, CRUDs operacionais e relatórios, em `frontend/admin/`. Inclui extensões cross-cutting do storefront que dependem de admin (vídeo de produto, cancelamento pelo cliente, reviews/comentários).

## Visão geral

Segundo app Angular 17 (standalone components + Tailwind), separado do storefront, consumindo as APIs backend já existentes (catalog, customer admin, inventory admin, order admin) + endpoints novos para cupons/promoções/impostos no Slice 6.4. Login dedicado pra usuários admin (roles `OPERADOR`, `GERENTE`, `ADMIN_LOJA`).

**Decisão de arquitetura confirmada:** dois apps Angular separados (storefront + admin), não SPA único com módulos. Razões:
- Carga inicial menor pra clientes finais (não baixam JS de admin).
- Permissões mais simples (sem checks complexos no router).
- Deploy independente (preview/staging do admin sem afetar storefront).
- Padrão de mercado e-commerce (Magento, Shopify, VTEX seguem isso).

## 6 slices, ~36-46h total

### Slice 6.1 — Foundation (~4h)

Scaffold do app + auth admin + shell + dashboard placeholder. Foco em estabelecer fundação reusável pelos slices seguintes.

**Entrega:**
- `frontend/admin/` com Angular 17 + Tailwind + design tokens espelhando o storefront.
- `proxy.conf.json` apontando `/api/v1/*` → `http://localhost:8080`.
- Porta `4243` (storefront usa 4242) — sem conflito.
- AuthService admin: login com email/senha → JWT, checa que role do user é `OPERADOR | GERENTE | ADMIN_LOJA` (cliente comum cai 403 ao tentar entrar). Refresh token em cookie HttpOnly (mesma estratégia do storefront).
- `AdminShellLayout`: topbar com nome do user + role badge + logout; sidebar fixa à esquerda com os 8 grupos de navegação (Dashboard, Catálogo, Estoque, Pedidos, Comercial, Clientes, Relatórios, Configurações) — itens habilitados conforme role.
- `roleGuard(perfis: Role[])` reusável.
- `/admin/dashboard` placeholder (cards com números fake até o slice 6.5).
- `/admin/login`.

**Esforço:** ~4h.

### Slice 6.2 — Catálogo (~6-8h)

CRUDs de Produtos e Categorias. Backend já tem `ProdutoController` e `CategoriaController` com endpoints admin protegidos.

**Entrega:**
- `/admin/produtos` lista paginada com ag-Grid (ou alternativa standalone-friendly — decidir no brainstorm).
- `/admin/produtos/novo` e `/admin/produtos/:sku/editar`.
- Upload de imagens com drag-and-drop (HTML5 drop + multipart upload, sem libs externas se possível).
- **Vídeo de produto** (novidade neste slice):
  - Campo `videoUrl` no `Produto` (Flyway V18+).
  - Admin cola URL externa (YouTube, Vimeo) no edit do produto — sem upload de arquivo interno nesta fase (decisão: simplicidade + custo de storage; armazenamento próprio fica pro backlog quando justificar).
  - Validação: URL deve casar com domínio de provider conhecido (`youtube.com`, `youtu.be`, `vimeo.com`).
  - Frontend storefront: detalhe do produto renderiza `<iframe>` se houver `videoUrl`, abaixo das imagens, com `sandbox` restritivo (`allow-scripts allow-same-origin`).
  - Admin: preview do vídeo embed no form de edição pra confirmar URL válida.
- `/admin/categorias` CRUD simples (lista + form inline, mesmo padrão dos CRUDs do slice 4 do storefront).
- Validações: SKU único, preço > 0, estoque consistente.

**Backend novo:**
- Migração Flyway adicionando coluna `video_url VARCHAR(500)` em `produtos`.
- Validação no `ProdutoController` (validator custom `@ValidVideoEmbedUrl`).
- Endpoints PUT/DELETE no `ProdutoController` se ainda forem só GET.

**Esforço:** ~6-8h.

### Slice 6.3 — Operações (~7-9h)

Estoque + Pedidos admin + cancelamento pelo cliente. Foco no fluxo operacional do dia-a-dia + fechamento de dívida da Fase 5.

**Entrega admin:**
- `/admin/estoque` lista com SKU, quantidade atual, reservas ativas, ajustes manuais (entrada/saída justificada).
- `/admin/pedidos` lista paginada com filtros (status, data, cliente, valor).
- `/admin/pedidos/:numero` detalhe completo com transições de estado (admin pode marcar SEPARACAO → EM_TRANSPORTE → ENTREGUE; também pode cancelar fora da janela do cliente).
- Geração de etiqueta de envio em PDF (jsPDF) — formato A6, código de barras da chave (placeholder até NF-e na Fase 7).
- Audit log básico (timestamp + user + ação + entidade) gravado em tabela `audit_log` (decisão: backend ou banco direto? → brainstorm).

**Entrega cancelamento pelo cliente (novidade — fecha dívida da Fase 5):**
- Endpoint `POST /api/v1/orders/{numero}/cancel` (não-admin — cliente autenticado, ownership check).
- Regra de negócio:
  - Permitido em `PENDENTE_PAGAMENTO` e `PAGAMENTO_APROVADO`.
  - Bloqueado a partir de `SEPARACAO` (precisa contatar admin — admin cancela via tela do `/admin/pedidos`).
  - Bloqueado em estados terminais (`ENTREGUE`, `CANCELADO`, `DEVOLVIDO`, `PAGAMENTO_REJEITADO`).
- Efeitos colaterais:
  - Pedido vai para estado `CANCELADO`.
  - Reserva de estoque cancelada via `InventoryService.cancelarReserva()`.
  - Se já estava `PAGAMENTO_APROVADO`: dispara estorno via `MockPaymentGateway.reembolsar(tentativaPagamentoId)` — método novo no gateway; resposta é mock (sucesso 100% no mock; gateway real fica pra Fase 7+).
  - Audit log: `OrderCancelledByCustomer` com motivo (free text opcional, max 500 chars).
- Storefront: botão "Cancelar pedido" no `/minha-conta/pedidos/:numero` quando status permitir. ConfirmDialog com motivo opcional. Toast no sucesso. Habilita o CTA disabled da Fase 5.

**Backend novo:**
- `POST /admin/orders/{numero}/transition` (admin) se ainda não existir.
- `POST /admin/inventory/adjust` (admin) se ainda não existir.
- `POST /orders/{numero}/cancel` (cliente) — novo endpoint no `OrderController` com ownership check.
- `MockPaymentGateway.reembolsar(tentativaId): RefundResult` — método novo, mock determinístico.

**Esforço:** ~7-9h.

### Slice 6.4 — Comercial (~5-6h)

CRUDs de Cupons, Promoções e Regras de Imposto. **Maioria do backend ainda não existe** — este slice envolve mais Java do que Angular.

**Entrega backend:**
- Módulo `pricing` ganha 3 entidades novas + REST: `Cupom`, `Promocao`, `RegraImposto`.
- Migrações Flyway V18+.
- Validações de negócio (datas de validade, escopo aplicável, conflitos).

**Entrega frontend:**
- 3 CRUDs no admin com padrão lista + form inline.
- Aplicação automática: cupons já entram no fluxo de cart/checkout do storefront (decisão: integrar agora ou ficar isolado? → brainstorm).

**Esforço:** ~5-6h (backend mais pesado).

### Slice 6.5 — Insights (~5-6h)

Dashboards reais + clientes admin + audit log + notificações em tempo real.

**Entrega:**
- `/admin/dashboard` com ApexCharts:
  - Card: pedidos hoje / 7d / 30d
  - Donut: distribuição por status
  - Bar: top 5 produtos vendidos
  - Lista: estoque crítico (qty < threshold)
- `/admin/clientes` lista paginada (readonly — admin não cria cliente).
- `/admin/clientes/:id` detalhe com pedidos do cliente + endereços + pets (apenas leitura, com PII mascarada — backend já retorna mascarado via `AdminCustomerController`).
- `/admin/audit-log` lista paginada das ações admin.
- Notificações em tempo real de novos pedidos via Server-Sent Events (SSE — escolhido sobre WebSocket por simplicidade; backend cria endpoint `GET /admin/events/orders` que mantém conexão e empurra eventos).

**Backend novo:**
- `GET /admin/dashboard/kpis` agregando dados de pedidos/estoque.
- `GET /admin/audit-log` paginado.
- `GET /admin/events/orders` SSE.

**Esforço:** ~5-6h.

### Slice 6.6 — Reviews e Avaliação de Produtos (~7-9h)

Sistema completo de reviews/comentários: cliente avalia produto comprado, vê média e distribuição no detalhe, admin modera. Fecha a UX e-commerce do storefront sem depender de NF-e nem de eventos assíncronos.

**Modelo de domínio (backend novo — módulo `reviews` ou dentro de `catalog`):**
- Entidade `Avaliacao`:
  - `id, clienteId, skuProduto, nota (1-5), comentario (text, max 2000, opcional), status (PUBLICADA | OCULTA | SINALIZADA), criadoEm, atualizadoEm`.
  - Constraint único `(clienteId, skuProduto)` — 1 avaliação por cliente por produto.
- Denormalização no `Produto`:
  - `avaliacaoMedia DECIMAL(3,2)` (ex: 4.50) — atualizado por trigger ou via service ao mudar `Avaliacao`.
  - `totalAvaliacoes INT` — idem.
- Regra de elegibilidade: cliente só posta avaliação se tem pedido em status `ENTREGUE` contendo o `skuProduto`. Validador no `AvaliacaoService.criar()`.
- Estados:
  - `PUBLICADA` — default, visível pra todos.
  - `OCULTA` — admin escondeu (abuso/spam). Continua existindo no banco mas não entra na média nem aparece na lista pública.
  - `SINALIZADA` — cliente reportou como ofensivo/inadequado; admin precisa revisar (entra numa fila de moderação).

**Entrega backend (Flyway V19+):**
- Migração: tabela `avaliacoes` + colunas em `produtos` + recalcular agregados via job.
- Endpoints cliente:
  - `POST /api/v1/products/{sku}/reviews` (cria — checa elegibilidade).
  - `PUT /api/v1/products/{sku}/reviews/me` (edita a própria).
  - `DELETE /api/v1/products/{sku}/reviews/me` (deleta a própria).
  - `GET /api/v1/products/{sku}/reviews?page=0&size=10&sort=criadoEm,desc` (lista pública — só `PUBLICADA`).
  - `POST /api/v1/products/{sku}/reviews/{id}/flag` (sinaliza — qualquer logado, body com motivo).
  - `GET /api/v1/customers/me/reviews` (minhas avaliações).
- Endpoints admin:
  - `GET /api/v1/admin/reviews?status=SINALIZADA` (fila de moderação).
  - `POST /api/v1/admin/reviews/{id}/hide` (oculta).
  - `POST /api/v1/admin/reviews/{id}/restore` (volta pra PUBLICADA).
- Recalcular agregados (`avaliacaoMedia`, `totalAvaliacoes`) ao criar/editar/deletar/ocultar/restaurar via `AvaliacaoService`.

**Entrega storefront:**
- Detalhe de produto (`/produtos/:sku`):
  - Seção "Avaliações" mostra média (estrelas + número), total, distribuição em barra horizontal por nota.
  - Lista paginada das últimas reviews públicas (10 por página).
  - Botão "Avaliar este produto" visível se elegível (cliente logado + comprou + ainda não avaliou).
  - Form inline (5 estrelas clicáveis + textarea opcional).
  - Sinalizar review individual via "..." (qualquer logado).
- `/minha-conta/avaliacoes` — listagem das próprias com editar/deletar.
- Componente shared `<app-rating-stars [nota]>` reusável (display) + `<app-rating-input [(nota)]>` (input).

**Entrega admin:**
- `/admin/reviews` — fila com filtro `status` (default SINALIZADA), ações Hide / Restore.
- Coluna "Produto" linka pro `/admin/produtos/:sku/editar`.
- Coluna "Cliente" mostra nome mascarado (consistente com PII admin existente).

**Princípios:**
- Sem moderação automática nesta fase (ex: detecção de spam por IA fica pra Fase 11).
- Email transacional ao admin quando review é SINALIZADA fica pra Fase 9 (notification service).

**Esforço:** ~7-9h (backend mais pesado pelo agregado + 3 superfícies).

## Itens fora da Fase 6

- **NF-e e emissão fiscal:** Fase 7 dedicada (módulo `invoice`, geração de DANFE, integração com Focus NFe sandbox).
- **Notificações em produção (real-time produção):** Fase 9 com Kafka.
- **Upload de vídeo interno do produto:** apenas URL externa no Slice 6.2; armazenamento próprio fica pro backlog quando justificar custo de storage/CDN.
- **Estorno real via gateway:** mock no Slice 6.3 (cancelamento); gateway real entra com Fase 7+.
- **Moderação automatizada de reviews (anti-spam, NLP):** Fase 11 (IA).
- **Email transacional ao admin para reviews SINALIZADAS:** Fase 9 (notification service).
- **Mudar email/senha admin:** fluxo dedicado fora deste escopo.

## Princípios reusados da Fase 5

- Standalone components Angular 17, sem NgModules.
- Tailwind com design tokens espelhando storefront (mesmas cores `coral`, `graphite`).
- Reactive Forms.
- Signals para estado local; sem libs externas de state (NgRx, etc.).
- Toast service + ConfirmDialog `<dialog>` nativo extraídos do storefront e re-implementados no admin (ou movidos pra um pacote shared — decidir no slice 6.1).
- Testes formais continuam como dívida acumulada (mesmo bloqueio fases 1-5). Foco em smoke programático + build verde por task.
- Conventional Commits com escopo `feat(admin):` em vez de `feat(storefront):`.

## Princípios novos da Fase 6

- **Permissionamento por role no router** — `roleGuard([...])` em cada rota.
- **Audit log obrigatório** em todas as mutations admin (Slice 6.3+).
- **Sem confiança em dados do request body** — admin tem privilégio elevado, validar tudo no backend mesmo que UI já valide.
- **Mascarar PII em logs do frontend** — `console.error` nunca leva CPF/email completos.

## Ordem recomendada de execução

Slices sequenciais. Cada um depende minimamente do anterior:
- 6.1 → fundação que todos usam ✅ entregue.
- 6.2 → primeiro uso de ag-Grid/tabelas, valida pattern; introduz vídeo de produto.
- 6.3 → primeiro uso de PDF + transições; reusa pattern de tabela; fecha dívida do cancelamento pelo cliente.
- 6.4 → mais backend; pode rodar em paralelo se outro dev pegar.
- 6.5 → consome tudo o que veio antes pra mostrar agregações (KPIs, audit log UI, SSE).
- 6.6 → cross-cutting (backend + storefront + admin); pode ser adiado pra depois da Fase 7 se prazo apertar, mas evita refazer queries do produto duas vezes se feito agora.

## Critérios para "Fase 6 entregue"

- [ ] Todos os 6 slices commitados.
- [ ] `frontend/admin/` build verde.
- [ ] Smoke programático em cada rota (HTTP 200 logado, 401/403 sem auth/role).
- [ ] Audit log capturando ações admin reais.
- [ ] Dashboard mostrando KPIs reais (não fakes).
- [ ] Vídeo de produto renderizando no storefront quando admin preenche URL.
- [ ] Cliente consegue cancelar pedido nos estados permitidos; admin cancela fora da janela.
- [ ] Reviews funcionando E2E: cliente posta, vê média, distribuição; admin modera SINALIZADA.
- [ ] ROADMAP.md: Fase 6 ✅.
- [ ] `ai-memory/roadmap/fase-6-pendencias.md` com histórico de commits e dívidas.

## Próximo passo imediato

Slice 6.2 (Catálogo + vídeo de produto). Spec + plan + implementação em sessão fresca.
