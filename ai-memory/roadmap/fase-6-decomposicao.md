# Fase 6 — Frontend Admin · Decomposição em slices

> **Status estratégico:** Planejada · 2026-05-17. Implementação por slices entregáveis.
> **Predecessor:** Fase 5 ✅ concluída em 2026-05-17.
> **Objetivo da fase:** Painel administrativo (back-office) separado do storefront, com dashboards, CRUDs operacionais e relatórios, em `frontend/admin/`.

## Visão geral

Segundo app Angular 17 (standalone components + Tailwind), separado do storefront, consumindo as APIs backend já existentes (catalog, customer admin, inventory admin, order admin) + endpoints novos para cupons/promoções/impostos no Slice 6.4. Login dedicado pra usuários admin (roles `OPERADOR`, `GERENTE`, `ADMIN_LOJA`).

**Decisão de arquitetura confirmada:** dois apps Angular separados (storefront + admin), não SPA único com módulos. Razões:
- Carga inicial menor pra clientes finais (não baixam JS de admin).
- Permissões mais simples (sem checks complexos no router).
- Deploy independente (preview/staging do admin sem afetar storefront).
- Padrão de mercado e-commerce (Magento, Shopify, VTEX seguem isso).

## 5 slices, 25-30h total

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

### Slice 6.2 — Catálogo (~5-6h)

CRUDs de Produtos e Categorias. Backend já tem `ProdutoController` e `CategoriaController` com endpoints admin protegidos.

**Entrega:**
- `/admin/produtos` lista paginada com ag-Grid (ou alternativa standalone-friendly — decidir no brainstorm).
- `/admin/produtos/novo` e `/admin/produtos/:sku/editar`.
- Upload de imagens com drag-and-drop (HTML5 drop + multipart upload, sem libs externas se possível).
- `/admin/categorias` CRUD simples (lista + form inline, mesmo padrão dos CRUDs do slice 4 do storefront).
- Validações: SKU único, preço > 0, estoque consistente.

**Backend novo (se necessário):** alguns endpoints PUT/DELETE no `ProdutoController` se ainda forem só GET.

**Esforço:** ~5-6h.

### Slice 6.3 — Operações (~6-7h)

Estoque + Pedidos admin. Foco no fluxo operacional do dia-a-dia.

**Entrega:**
- `/admin/estoque` lista com SKU, quantidade atual, reservas ativas, ajustes manuais (entrada/saída justificada).
- `/admin/pedidos` lista paginada com filtros (status, data, cliente, valor).
- `/admin/pedidos/:numero` detalhe completo com transições de estado (admin pode marcar SEPARACAO → EM_TRANSPORTE → ENTREGUE).
- Geração de etiqueta de envio em PDF (jsPDF) — formato A6, código de barras da chave (placeholder até NF-e na Fase 7).
- Audit log básico (timestamp + user + ação + entidade) gravado em tabela `audit_log` (decisão: backend ou banco direto? → brainstorm).

**Backend novo:** endpoint `POST /admin/orders/{numero}/transition` se ainda não existir; `POST /admin/inventory/adjust` se ainda não existir.

**Esforço:** ~6-7h.

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

## Itens fora da Fase 6

- **NF-e e emissão fiscal:** Fase 7 dedicada (módulo `invoice`, geração de DANFE, integração com Focus NFe sandbox).
- **Notificações em produção (real-time produção):** Fase 9 com Kafka.
- **Cancelar pedido pelo cliente:** continua dívida da Fase 5 (regras de negócio incompletas).
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
- 6.1 → fundação que todos usam.
- 6.2 → primeiro uso de ag-Grid/tabelas, valida pattern.
- 6.3 → primeiro uso de PDF + transições; reusa pattern de tabela.
- 6.4 → mais backend; pode rodar em paralelo se outro dev pegar.
- 6.5 → consome tudo o que veio antes pra mostrar agregações.

## Critérios para "Fase 6 entregue"

- [ ] Todos os 5 slices commitados.
- [ ] `frontend/admin/` build verde.
- [ ] Smoke programático em cada rota (HTTP 200 logado, 401/403 sem auth/role).
- [ ] Audit log capturando ações admin reais.
- [ ] Dashboard mostrando KPIs reais (não fakes).
- [ ] ROADMAP.md: Fase 6 ✅.
- [ ] `ai-memory/roadmap/fase-6-pendencias.md` com histórico de commits e dívidas.

## Próximo passo imediato

Brainstorm + spec + implementação de **Slice 6.1 (Foundation)**.
