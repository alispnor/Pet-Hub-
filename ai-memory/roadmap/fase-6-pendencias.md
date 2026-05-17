# Fase 6 — Pendências e checklist

> **Status:** Slice 6.1 ✅ entregue em 2026-05-17. Slices 6.2-6.5 ⏳ pendentes.
> Testes Karma/Jest formais continuam dívida acumulada (mesmo bloqueio das Fases 1-5).

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

**Validações smoke programáticas (2026-05-17):**
- `npm run build` verde em todas as 14 tasks.
- `GET http://127.0.0.1:4244/` → 200 (ng serve respondendo).
- `GET http://127.0.0.1:4244/login` → 200.
- `POST /api/v1/auth/login` com `admin@pethub.com / Admin@123` → 200, payload contém `roles: ["ROLE_ADMIN_LOJA"]`.
- Storefront :4242 continua respondendo (sem conflito).

**Validações UX no browser (dívida do Ali — smoke checklist §5 do spec):**
- Login com admin/gerente/operador → dashboard com badge correto.
- Login com cliente do seed → toast "Sem permissão".
- F5 mantém sessão (refresh cookie).
- Sidebar pin persiste após F5.
- Drawer mobile, ESC fecha, hambúrguer abre.

## ⏳ Slices 6.2-6.5 pendentes

Plano em [`ai-memory/roadmap/fase-6-decomposicao.md`](./fase-6-decomposicao.md).

- **6.2 Catálogo** — Produtos (lista+detalhe+criar/editar, DnD imagens), Categorias (CRUD). ~5-6h.
- **6.3 Operações** — Estoque, Pedidos admin (transições, etiqueta PDF), audit log básico. ~6-7h.
- **6.4 Comercial** — Cupons, Promoções, Regras de Imposto (CRUDs + endpoints backend novos). ~5-6h.
- **6.5 Insights** — Dashboard com ApexCharts, Clientes admin, Audit log UI, SSE para novos pedidos. ~5-6h.

## ⚠️ Dívida técnica

### Testes formais
Mesmo bloqueio das fases 1-5 — sem CI rodando ainda. Pacote sugerido pro slice 6.1 quando entrar TDD:
- `AdminAuthService` — login rejeita ROLE_CLIENTE, login aceita admin/gerente/operador, refresh gate compartilha observable, `rolePrincipal` prioriza ADMIN_LOJA.
- `roleGuard` — sem login → /login, com role insuficiente → /dashboard, com role permitida → true.
- `authInterceptor` — anexa Bearer, 401 dispara refresh + retry, falha refresh chama logout.
- `LoginPage` — toast correto pra 401, ForbiddenLoginError, erro genérico.
- `AdminShellPage` — sidebar pin persiste localStorage, drawer ESC fecha.

### AppSec
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

# Backend
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

**Próximo passo: Slice 6.2 (Catálogo — produtos + categorias).**
