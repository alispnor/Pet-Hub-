# Fase 6 — Pendências e checklist

> **Status:** Slice 6.1 ✅ entregue em 2026-05-17. Slice 6.2 🚧 em andamento (backend 9/10 tasks ✅; frontend admin e storefront 0/13). Slices 6.3-6.6 ⏳ pendentes.
> Testes Karma/Jest/JUnit formais continuam dívida acumulada (mesmo bloqueio das Fases 1-5).

## ✅ Slices entregues

### Slice 6.1 — Foundation (`5de670b` → `9978082`)

14 commits sequenciais entregues em 2026-05-17:
- `01-angular 17 standalone scaffold` (`5de670b`)
- `02-tailwind config and design tokens` (`1af64d4`)
- `03-path aliases proxy and dev port 4244` (`8a40b5e`)
- `04-add admin port 4244 to CORS` (`f87556b`)
- `05-auth models and errors` (`3bd4d9a`)
- `06-admin auth service with role validation` (`276ece0`)
- `07-auth and role guards` (`efadd69`)
- `08-auth http interceptor with refresh retry` (`0c014db`)
- `09-app config component routes` (`65a0ec9`)
- `10-shared skeleton toast and dialog components` (`f647c91`)
- `11-admin shell with topbar and sidebar pin` (`6a6d5ec`)
- `12-login page with role validation feedback` (`873d060`)
- `13-dashboard placeholder with role greeting` (`fc77397`)
- `13b-align with backend roles array` (`9978082`) — fix descoberto no smoke

**Decisões congeladas (Slice 6.1):**
- Dois apps Angular separados (storefront em 4242 + admin em 4244), não SPA único com módulos.
- Code sharing entre apps = duplicação (não pacote shared) — velocidade > DRY no momento.
- Visual: mesma paleta `coral`/`graphite` do storefront, layout admin-style (sidebar densa 64px com toggle de pin pra 200px, topbar fina 56px).
- Login = mesmo endpoint `POST /auth/login`; frontend rejeita user sem nenhuma role admin (`ROLE_ADMIN_LOJA | ROLE_GERENTE | ROLE_OPERADOR`) invocando `POST /auth/logout` e disparando `ForbiddenLoginError`.
- Sem opção "manter conectado" no admin (TTL padrão 7 dias — menor janela de exposição).
- Refresh em cookie HttpOnly (mesma estratégia do storefront).
- Estado da sidebar (pinada) persistido em `localStorage` chave `pethub:admin:sidebar:pinned`.
- Backend retorna `usuario.roles: Role[]` (array, não singular). Helper `rolePrincipal(roles)` deriva o badge com prioridade ADMIN_LOJA > GERENTE > OPERADOR.
- `roleGuard` implementado com intersection check (user precisa ter ≥1 das roles permitidas), exportado mas não aplicado em nenhuma rota neste slice — disponível pros slices 6.2+.

## 🚧 Slice 6.2 — Catálogo (em andamento, pausado 2026-05-17)

**Plano completo:** [`docs/superpowers/plans/2026-05-17-admin-slice2-catalog.md`](../../docs/superpowers/plans/2026-05-17-admin-slice2-catalog.md) (23 tasks)
**Spec:** [`docs/superpowers/specs/2026-05-17-admin-slice2-catalog-design.md`](../../docs/superpowers/specs/2026-05-17-admin-slice2-catalog-design.md)

### Tasks concluídas (9 de 23) — backend completo até T10

Sequência de commits em `main`:

| Task | Commit | Descrição |
|---|---|---|
| T01 | `307e69f` | `refactor(backend): slice2 01-move PhotoStorage to common module` |
| T02 | `0e327c6` | `feat(backend): slice2 02-flyway V18 add video_url to produtos` |
| T03 | `d31e5ee` | `feat(backend): slice2 03-add ValidVideoEmbedUrl constraint` |
| T04 | `323339f` | `feat(backend): slice2 04-produto entity and DTOs gain videoUrl field` |
| T05 | `8a4e8c4` | `feat(backend): slice2 05-produto service persists videoUrl on create and update` |
| T06 | `1848131` | `feat(backend): slice2 06-add admin product summary DTO and repository` |
| T07 | `8bfbeca` | `feat(backend): slice2 07-mapper toAdminSummary and service listarAdmin` |
| T08 | `5aef70d` | `feat(backend): slice2 08-admin endpoint to list products with filters` |
| T09 | `6b5e2df` | `feat(backend): slice2 09-multipart image upload endpoint for produtos` |

**O que está pronto no backend:**
- `PhotoStorage` (interface + `LocalPhotoStorage` + `PhotoStorageProperties`) movido para `backend/common/storage/` — reutilizável por catalog (produtos) e customer (pets).
- Flyway V18 adicionou `produtos.video_url VARCHAR(500)` (nullable, sem default).
- `@ValidVideoEmbedUrl` em `common/validation/` — regex whitelist YouTube (`/watch?v=`, `youtu.be/`) + Vimeo (numérico). Null/blank passa. Defesa anti-SSRF: backend nunca faz HTTP request à URL.
- Entidade `Produto` tem campo `videoUrl`; `ProdutoRequest` valida com `@ValidVideoEmbedUrl @Size(max=500)`; `ProdutoDetailResponse` expõe `videoUrl`; MapStruct auto-mapeia pelo nome.
- `ProdutoService.criar/atualizar` persistem `videoUrl`.
- `ProdutoAdminSummaryResponse` DTO + `ProdutoRepository.filtrar(q, categoriaId, ativo, pageable)` (admin merged no mesmo repo — convenção do `PedidoRepository.searchAdmin`).
- `ProdutoMapper.toAdminSummary` + helpers `@Named("totalImagens")` e `@Named("temVideo")`.
- `ProdutoService.listarAdmin` com normalização de query (trim + blank→null).
- Endpoint `GET /api/v1/admin/catalog/products?q=&categoriaId=&ativo=&page=&size=&sort=` (default `sort=nome,asc`, role `ADMIN_LOJA|GERENTE|OPERADOR`).
- Endpoint `POST /api/v1/admin/catalog/products/{id}/images/upload` (multipart `file` + query `principal=`, role `ADMIN_LOJA|GERENTE`). Reusa `PhotoStorage.store("products", id, file)`.

**Decisões congeladas no backend:**
- `ProdutoRequest` permanece DTO único pra criar/atualizar (não dois DTOs como a spec original sugeria).
- Origem real do enum: `NACIONAL | IMPORTADO_DIRETO | IMPORTADO_INDIRETO` (spec inicial assumia outro conjunto).
- Identifiers em inglês no `common/validation/` (`PATTERN`, `MAX_LENGTH`, `value`) por consistência com `CpfValidator`/`NcmValidator` — divergente do plano original que tinha PT.
- Admin filter merged em `ProdutoRepository.filtrar` (não criou `ProdutoAdminRepository` separado).
- Upload multipart real (não URL-only) com storage local em `var/uploads/products/` — não escala em K8s; revisita em Fase 11 (S3/MinIO).
- N+1 lazy load em `categoria` e `imagens` aceito na listagem admin (consistente com `AdminCustomerService.toSummary`); revisita quando >10k produtos.

### Tasks pendentes (14 de 23) — retomar a partir de T10

A enumeração e o conteúdo de cada task ficam no plano (`2026-05-17-admin-slice2-catalog.md`). Resumo:

#### Backend (1 task restante)
- **T10** — Categoria `restaurar` endpoint (`POST /admin/catalog/categories/{id}/restaurar` + `CategoriaService.restaurar`).
- **T11** — Backend full reactor build + smoke E2E (curl): coluna existe, login admin, list admin, criar com videoUrl válida + inválida (422), upload multipart de pixel JPG, desativar + restaurar categoria. Limpar dados de teste. Sem commit (smoke only).

#### Frontend admin (9 tasks)
- **T12** — models `produto.ts` + `categoria.ts` (Origem real, `construirArvore/achatarArvore/descendentesId`).
- **T13** — services `produto.service.ts` + `categoria.service.ts`.
- **T14** — `shared/utils/video-embed.ts` + `shared/pipes/safe-resource-url.pipe.ts` no admin.
- **T15** — `VideoUrlInputComponent` (input + validação inline + iframe sandboxed preview).
- **T16** — `ImageDropZoneComponent` (DnD nativo + multipart sequencial via `concatMap` + validação 5MB/tipos).
- **T17** — `ProdutosListaPage` (tabela custom Tailwind, filtros URL-sync, sort, toggle ativo inline, paginação).
- **T18** — `ProdutoFormPage` (form 2 colunas, modo via rota, SKU/preco disabled em editar, preço update dedicado).
- **T19** — `CategoriasPage` (lista plana indentada, form inline, desativar/restaurar).
- **T20** — Routes + roleGuard + AdminShellPage sidebar update (Catálogo, Categorias habilitados).

#### Frontend storefront (2 tasks)
- **T21** — `safe-resource-url.pipe.ts` + helper `urlEmbed` no storefront.
- **T22** — `ProdutoDetail.videoUrl` + seção condicional de vídeo no PDP com `<iframe sandbox>`.

#### Wrap-up (1 task)
- **T23** — Smoke E2E manual (admin + storefront), atualizar este arquivo + ROADMAP, commit docs final.

### Observações importantes ao retomar T10+

1. **Plano markdown desatualizado em 2 pontos** (já implementados de forma diferente, código está correto):
   - T07 do plano fala em injetar `ProdutoAdminRepository` — não existe; está merged em `ProdutoRepository.filtrar`. `ProdutoService.listarAdmin` chama `produtoRepository.filtrar(...)`.
   - T03 do plano usava identifiers PT (`PADRAO`, `TAMANHO_MAXIMO`, `valor`) — código usa EN (`PATTERN`, `MAX_LENGTH`, `value`) por convenção do pacote `common/validation/`.

2. **Follow-up não-bloqueante registrado no review do T09:** duplicação entre `adicionarImagem(URL)` e `adicionarImagemUpload(multipart)` — bloco de `if (principal)` + builder + save é byte-equivalente. Extrair helper privado `anexarImagem(Produto, String url, boolean principal)` quando alguém tocar de novo nesses métodos.

3. **Verificação de aplicação da V18 ainda não foi feita** — o build container compila o JAR mas a migration só roda quando o backend container subir com o JAR atualizado. T11 cobre isso.

## ⏳ Slices 6.3-6.6 pendentes

Plano em [`ai-memory/roadmap/fase-6-decomposicao.md`](./fase-6-decomposicao.md).

- **6.3 Operações** — Estoque admin, Pedidos admin (transições + etiqueta PDF), cancelamento pelo cliente (fecha dívida da Fase 5), audit log. ~7-9h.
- **6.4 Comercial** — Cupons, Promoções, Regras de Imposto (3 entidades novas no `pricing` + CRUDs admin). ~5-6h.
- **6.5 Insights** — Dashboard ApexCharts real, clientes admin, audit log UI, SSE de novos pedidos. ~5-6h.
- **6.6 Reviews** — Sistema de avaliações (backend + storefront + admin moderação). ~7-9h.

## ⚠️ Dívida técnica

### Testes formais
Mesmo bloqueio das fases 1-5 — sem CI rodando ainda. Pacote sugerido pra Slice 6.1 + 6.2 quando entrar TDD na Fase 11+:
- `AdminAuthService` — login rejeita ROLE_CLIENTE, login aceita admin/gerente/operador, refresh gate compartilha observable, `rolePrincipal` prioriza ADMIN_LOJA.
- `roleGuard` — sem login → /login, com role insuficiente → /dashboard, com role permitida → true.
- `authInterceptor` — anexa Bearer, 401 dispara refresh + retry, falha refresh chama logout.
- `LoginPage` — toast correto pra 401, ForbiddenLoginError, erro genérico.
- `AdminShellPage` — sidebar pin persiste localStorage, drawer ESC fecha.
- `ProdutoService.listarAdmin` — filtros aplicados corretamente, normalização de query, paginação.
- `ProdutoMapper.toAdminSummary` — `temVideo` reflete nullness + blank, `totalImagens` para null/empty.
- `LocalPhotoStorage` — rejeita tipo desconhecido, rejeita >5MB, gera UUID-named filename, cria diretório de namespace.

### AppSec do Slice 6.2 (em andamento)
- SSRF mitigado por design (backend não consome `videoUrl`). Validador whitelist + (futuro) iframe sandbox = defesa em profundidade.
- Storage local de imagens fica como dívida até Fase 11 (K8s requer storage externo: S3/MinIO/GCS).
- Content-Type-only validation em uploads é spoofável (cliente seta cabeçalho). Magic-byte sniffing (Apache Tika ou similar) fica como melhoria de longo prazo — endpoint é admin-gated, baixo risco prático.

### AppSec do Slice 6.1
- LOG-2 (mascaramento de PII em logs do frontend) — endereçar na transição para staging.
- JWT-1, CORS-1, VAL-1 — bloqueantes só no caminho para staging.

### "Esqueci minha senha"
Disabled no slice 6.1; backlog sem prazo definido.

## 🚀 Comando rápido para retomar

```bash
cd /home/ali/projects/pet-hub
git log --oneline | head -20

# Infra (Postgres 5433, Redis 6380, pgAdmin 5050)
docker compose --env-file .env.local -f infrastructure/docker/docker-compose.dev.yml up -d

# Backend (precisa de novo JAR antes — rodar mvn -DskipTests package primeiro pra T10+ aplicar V18)
docker run --rm -d --name pethub-backend --network host \
  --env-file .env.local \
  -v "$PWD/backend/application/target/pet-hub-backend.jar:/app/app.jar:ro" \
  -v "$PWD/backend/application/var:/app/var" \
  -w /app eclipse-temurin:21-jre java -jar /app/app.jar

# Storefront
cd frontend/storefront && npm start   # http://127.0.0.1:4242

# Admin
cd frontend/admin && npm start        # http://127.0.0.1:4244
```

Storefront em :4242, admin em :4244 — rodam lado a lado.

**Credenciais admin do seed:**
- `admin@pethub.com / Admin@123` (ROLE_ADMIN_LOJA — V3)
- `gerente@pethub.com / Gerente@123` (ROLE_GERENTE — V16)
- `operador@pethub.com / Operador@123` (ROLE_OPERADOR — V16)

**Próximo passo: Slice 6.2 retoma em T10 (Categoria restaurar) → T11 (smoke backend) → T12+ (frontend admin).**
