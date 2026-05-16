# Pet Hub — Storefront (Angular 17)

SPA do cliente do Pet Hub: catálogo, carrinho, checkout, minha conta. Veja o README raiz do repo para o panorama do produto e o roadmap completo.

## Stack

- Angular 17 (standalone components, signals)
- TailwindCSS 3 com tokens de `ai-memory/design-system.md`
- Reactive Forms
- Proxy `/api/*` → `http://localhost:8080` (backend Spring Boot)

## Pré-requisitos

- Node 18+
- Backend Pet Hub rodando em `http://localhost:8080` (veja [`../../README.md`](../../README.md))

## Desenvolvimento

```bash
# Primeira vez:
npm install

# Subir o dev server na porta 4242 com proxy para o backend:
npx ng serve --host 127.0.0.1 --port 4242 --proxy-config proxy.conf.json
```

App em http://127.0.0.1:4242 — auto-reload a cada save.

### Por que porta 4242 e não 4200?

O `CORS_ALLOWED_ORIGINS` no `.env.local` da raiz já libera `http://127.0.0.1:4242`. Se rodar em outra porta, ajuste essa env var **e** reinicie o backend.

## Credenciais seed (ambiente dev)

| Tipo | Email | Senha |
|---|---|---|
| Cliente | `maria.fase2@pethub.com` | `Senha@123` |
| Admin | `admin@pethub.com` | `Admin@123` |

> O storefront é a SPA do **cliente**. Login com conta admin funciona, mas não há telas administrativas aqui — admin SPA é entregue na Fase 6.

## Arquitetura

```
src/app/
├── app.config.ts            # providers (HttpClient + interceptor + router)
├── app.routes.ts            # rotas lazy
├── core/
│   ├── guards/              # authGuard
│   ├── interceptors/        # authInterceptor (Bearer + auto-refresh em 401)
│   ├── models/              # tipos espelhando DTOs do backend
│   └── services/            # AuthService, CatalogService, ShippingService
├── features/                # uma pasta por feature, com .page.ts dentro
│   ├── auth/                # login + cadastro
│   ├── catalog/             # listagem + detalhe
│   └── home/                # destaques
└── shared/
    ├── components/          # PriceDisplay, ProductCard, ShippingCalculator
    └── layout/              # MainLayoutComponent (header + footer)
```

### Convenções

- **Componentes standalone**, nada de NgModules.
- **Signals** para estado reativo (não BehaviorSubject) — exceto onde rxjs já é natural (formulários, HTTP).
- **Lazy-loaded routes**: `loadComponent: () => import(...).then(m => m.XxxPage)`.
- **`withCredentials`**: só nas rotas listadas em `environment.credentialedPaths`. Catálogo público NÃO carrega cookie de refresh.
- **Tokens de design**: classes Tailwind sempre, nunca hex direto. Cores/spacing/tipografia em `ai-memory/design-system.md`.

## AppSec — práticas obrigatórias

- Access token **só em memória** (`signal`), nunca em `localStorage`/`sessionStorage`.
- Refresh token via **cookie HttpOnly** emitido pelo backend (`Path=/api/v1/auth`). O JS nunca lê.
- HTML dinâmico do servidor passa por escape antes de qualquer `[innerHTML]` (DOMPurify pipe entra no slice 4).
- Inputs sensíveis (CEP, CPF, número de cartão) são validados client-side **e** revalidados no backend (defesa em profundidade).
- Detalhes completos no `ai-memory/architecture/appsec-guidelines.md` da raiz.

## Build de produção

```bash
npx ng build
# Artefatos em dist/storefront/
```

## Status (Fase 5)

| Slice | Status | Conteúdo |
|---|---|---|
| 1 — Scaffold + Auth | ✅ entregue | Angular scaffold, AuthService + interceptor + guard, /login, /cadastro |
| 2 — Catálogo | ✅ entregue | /, /produtos, /produtos/:sku, calculadora de frete |
| 3 — Carrinho + Checkout | 🚧 em andamento | /carrinho, /checkout/{endereco,frete,pagamento,revisao}, /checkout/sucesso/:numero |
| 4 — Minha conta + Timeline visual | ⏳ próximo | CRUDs + OrderTimelineComponent |

Veja [`../../ai-memory/roadmap/`](../../ai-memory/roadmap/) para histórico detalhado das fases.
