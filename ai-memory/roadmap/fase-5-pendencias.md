# Fase 5 — Pendências e checklist

> **Status:** Slices 1-4 ✅ entregues em 2026-05-14 a 2026-05-17. **Fase 5 concluída.**
> Testes Karma/Jest formais continuam dívida acumulada (mesmo bloqueio das Fases 1-4).

## ✅ Slices entregues

### Slice 1 — Angular scaffold + Auth (`2778cf8`)
Standalone components Angular 17, Tailwind 3 com tokens do design-system, AuthService (signals) + authInterceptor com auto-refresh em 401 (BehaviorSubject gate) + authGuard. Telas `/login` e `/cadastro` com Reactive Forms.

### Slice 2 — Catálogo (`167a887`)
3 componentes shared (`PriceDisplayComponent`, `ProductCardComponent`, `ShippingCalculatorComponent`) + 3 páginas (home/lista/detalhe). Models tipados espelhando DTOs. Search debounce 350ms, paginação, query params sincronizados.

### Refactor estrutural (`d80e519`)
Migração `features/` → `modules/`. Cada componente em pasta própria com `.ts + .html + .scss`. Path aliases `@core/*`, `@shared/*`, `@modules/*`, `@validators/*`, `@env/*`. MainLayoutComponent em `core/layout/`. ShippingService em `shared/services/`. Modelos do customer (address, payment-method) em `modules/customer/`. Variáveis de 1 letra eliminadas globalmente.

### OWASP audit mapping (`1f85cde`)
Plano OWASP 2025 do guep-crm mapeado para o Pet Hub. 8 dos 21 achados já estavam catalogados; 3 já resolvidos pela stack; 5 novos adicionados (LOG-2, LOG-3, DEP-2, EXC-1, PAY-3) em `appsec-pendencias.md`; sumário consolidado em `AUDITORIA-OWASP-2025-PETHUB.md` na raiz.

### Auth UX (`9bcacc9`, `3298569`)
- `PasswordInputComponent` shared (ControlValueAccessor) com toggle olho. Usado em `/login` e `/cadastro`.
- Checkbox "Manter conectado neste dispositivo (30 dias)" no `/login`.
- Backend: `JwtProperties.refreshTokenTtlExtendedDays` (default 30, validado >= ttlDays). `JwtService.refreshTokenTtl(boolean)`. `LoginRequest.manterConectado: Boolean?`. `AuthEmissionResult` (wrapper service → controller). V17 `refresh_tokens.manter_conectado BOOLEAN`.

### Dev/AppSec ops
- `5b46044 chore(dev)`: `npm start` → `ng serve --host 127.0.0.1 --port 4242 --proxy-config proxy.conf.json`. V16 seed completo (admin/gerente/operador/3 clientes).
- `89e2c8b fix(dev)`: CORS_ALLOWED_ORIGINS no `.env.local.example` ganha `http://127.0.0.1:4242` (sem isso o browser dava 403 "Invalid CORS request").

### Slice 3 — Carrinho + Checkout 4 passos + Sucesso
Commits sequenciais: `ee6110b` (cart page + badge + PDP wire) → `d550766` (address) → `c18c80f` (shipping) → `b94a60e` (payment) → `8c86f97` (review) → `0814d33` (success com confete).

**Decisões congeladas:**
- `sessionStorage['pethub:checkout:v1']` carrega só IDs + idempotencyKey gerada com `crypto.randomUUID()`.
- Cadastro inline de endereço/cartão dentro do wizard (não depende do slice 4).
- Cupom só em `/carrinho`; checkout/preview valida e mostra desconto.
- Tokenize via `POST /customers/payment-methods/tokenize` — PAN/CVV limpos do form imediatamente após sucesso (`formularioCartao.patchValue({ numero: '', cvv: '' })` dentro do `next:` da tokenize, ANTES do `create()` chained).
- CVV continua `type="password"` cru, sem toggle olho (diferente de senha de login).
- Idempotency-key: APPROVED → `state.clear()` (apaga tudo); REJECTED → `patch({ idempotencyKey: null })` para próxima tentativa receber nova key; erro de transporte → preserva key.
- "Estoque mudou" detectado pelo frontend via `error.detail.toLowerCase().includes('estoque insufic')` — backend retorna string em `ProblemDetail.detail`, não há campo `skus[]` estruturado.

## ✅ Validações smoke E2E (2026-05-16, via curl/proxy)

- Login com/sem manterConectado: cookie Max-Age 604800 vs 2592000 ✓
- Cart add/update/remove/coupon/clear ✓
- Cupom inválido aceita anexar; validação real só no preview ✓
- Address ViaCEP autofill ✓
- Shipping retorna 3 opções (motoboy só dentro Grande SP) ✓
- Preview com cupom 10OFF: desconto 10% sobre subtotal ✓
- Place-order APPROVED (PH-2026-000002): cart limpa, estoque decrementa, RESERVA+BAIXA_VENDA registradas com `referencia_pedido` ✓
- Idempotência: mesma key → mesma tentativa, não cobra 2x ✓
- Place-order REJECTED com Luhn-válido `4111 1111 1111 9400` (last4=4000): pedido criado em estado terminal `PAGAMENTO_REJEITADO`, reserva CANCELADA, estoque preservado, cart preservado ✓
- Estoque mudou (UPDATE estoques SET quantidade=0): preview/place-order retornam 422 com detail "Estoque insuficiente para: SKU (disponível=0, solicitado=N)" — `ehErroDeEstoque` detecta ✓

**Faltam validações UX no browser** (foco do smoke manual do Ali): confete dispara 1×, dialog modal "Estoque mudou" abre, stepper indica progresso visual, toggle olho na senha, checkbox manter conectado, navegação back/forward entre passos preserva state.

### Slice 4 — Minha conta + Timeline visual (`c259295` → `c6ee32e`)

20 commits sequenciais entregues em 2026-05-17:
- `01-add dompurify dependency` (`c259295`)
- `02-safe-html pipe with dompurify` (`abf238f`) — substituiu `descricaoSanitizada()` no product-detail
- `03-skeleton shared component` (`e18d8d5`)
- `04-toast service and stack` (`b1b3a8d`)
- `05-confirm dialog with native dialog element` (`4f107a2`)
- `06-perfil model and service` (`c14e20c`)
- `07-pet model and service` (`a30bb69`)
- `08-order model and service` (`a4f7a3c`)
- `09-extend address and payment services` (`5392d6f`)
- `10-account shell with sidebar and drawer` (`7fd4b45`)
- `11-overview page with live cards` (`40cf9ba`)
- `12-perfil page with reactive form` (`5688cb4`)
- `13-enderecos page with inline form` (`8f51fad`)
- `14-cartoes page with tokenization` (`0b23e7a`)
- `15-pets page with photo upload` (`2abe24f`)
- `16-order timeline component (visual highlight)` (`471f432`)
- `17-orders list with url-synced filters` (`88bccf6`)
- `18-order detail page with full timeline` (`f30a8b3`)
- `19-enable acompanhar pedido cta` (`c6ee32e`)
- `20-roadmap fase 5 concluida` (este commit)

**Decisões congeladas (Slice 4):**
- `/minha-conta` é um shell com sidebar vertical em md+ e drawer hambúrguer no mobile, com 7 sub-rotas (`''`, `perfil`, `enderecos`, `cartoes`, `pets`, `pedidos`, `pedidos/:numero`).
- Overview com 5 cards carregados via `forkJoin` + `catchError` por chamada — falha isolada por card, retry manual.
- CRUDs com lista + form inline expansível (sem modais aninhados).
- Cartões: sem edição — só add (com tokenize), remove, tornar padrão. PAN/CVV limpos do form imediatamente após sucesso da tokenize.
- Pets: upload de foto pós-save, preview client-side via `URL.createObjectURL()`, limite 5MB.
- `OrderTimelineComponent` híbrido: horizontal em `≥ md`, vertical em mobile via media query no `.scss`. ATUAL com `animate-pulse` + ring; CANCELADO/REJEITADO em vermelho; DEVOLVIDO em âmbar. `prefers-reduced-motion` desabilita pulse.
- Lista de pedidos: filtros (status, data início/fim) sincronizados com query params; **sem mini-timeline** na lista (custo de payload).
- Detalhe de pedido: 2 chamadas paralelas (detalhe + timeline) via `forkJoin`; snapshots de endereço tipados como `EnderecoSnapshot`.
- `SafeHtmlPipe` (DOMPurify) com whitelist conservadora: `<p>`, `<br>`, `<strong>`, `<em>`, `<ul>`, `<ol>`, `<li>`, `<a>`. Substitui escape manual no `product-detail`.
- Toast e ConfirmDialog: services com signal + componentes hostados uma vez no `MainLayoutComponent` (sem libs externas).
- Skeleton extraído pra `shared/components/skeleton/`.

**Validações smoke programáticas (2026-05-17):**
- `npm run build` verde após cada task (20 builds bem-sucedidos).
- `GET /minha-conta` retorna 200 com storefront servindo o shell.
- `GET /actuator/health` retorna 200 (backend funcionando).
- Validação UX completa no browser fica como dívida do Ali (smoke checklist §9 do spec).

## ⚠️ Dívida técnica restante

### Testes formais
Mesmo bloqueio das Fases 1-4 — sem CI rodando ainda. Pendências de Karma/Jest catalogadas em §11 da spec do slice 3 (`docs/superpowers/specs/2026-05-16-storefront-slice3-cart-checkout-design.md`). Pacote completo:
- `CartServiceTest` — addItem/updateQty/applyCoupon/clearLocal/clear + error mapping.
- `CheckoutStateServiceTest` — persist/load do sessionStorage, version key v1, clear, ensureIdempotencyKey.
- `CheckoutStepGuardTest` — cada combinação de state vs passo destino.
- `CheckoutReviewPageTest` — APPROVED/REJECTED/transport-error idempotency replay + dialog estoque.
- `PaymentMethodFormTest` — Luhn client-side, mascaramento PAN, tokenize wired, autocomplete cc-* attributes presentes.
- Cypress/Playwright E2E — automatizar o smoke checklist da §10 da spec.

### AppSec (`appsec-pendencias.md`)
JWT-1, CORS-1, VAL-1 viram bloqueantes só no caminho para staging. LOG-2 (mascaramento de PII em logs) é bom endereçar na transição para Fase 6 (admin terá audit log que vai logar mais coisa).

### CTA "Acompanhar pedido" disabled
A tela de sucesso do slice 3 mostra o botão `disabled` com tooltip "Disponível em breve". Habilitar quando `/minha-conta/pedidos/:numero` (slice 4) entrar.

## 🚀 Comando rápido para retomar

```bash
cd /home/ali/projects/pet-hub
git log --oneline -15
git status

# Sobe infra
docker compose --env-file .env.local -f infrastructure/docker/docker-compose.dev.yml up -d

# Backend em container (host sem JDK/Maven nativo)
cd backend && docker run --rm -d --name pethub-app-dev \
  --env-file /home/ali/projects/pet-hub/.env.local \
  -v "$PWD":/workspace -v /home/ali/.m2:/root/.m2 \
  --network host -w /workspace \
  maven:3.9-eclipse-temurin-21 \
  mvn -B -ntp -pl application spring-boot:run -DskipTests

# Storefront
cd /home/ali/projects/pet-hub/frontend/storefront && npm start
```

Storefront em http://127.0.0.1:4242. Matriz completa de usuários teste no README raiz. Cartões: `4111 1111 1111 1111` aprova; `4111 1111 1111 9400` (Luhn-válido last4=4000) rejeita.

**Fase 5 entregue. Próxima: Fase 6 (Admin back-office).**
