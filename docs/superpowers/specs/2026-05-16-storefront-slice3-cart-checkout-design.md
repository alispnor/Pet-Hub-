# Storefront Slice 3 — Carrinho + Checkout 4 passos

> **Status:** Draft · 2026-05-16
> **Owner:** Ali
> **Phase:** 5 (Frontend Storefront) — Slice 3 de 4
> **Predecessors:** Slice 1 (auth scaffold) e Slice 2 (catálogo) em `origin/main`
> **Successor:** Slice 4 (minha conta + timeline visual)

## 1. Objetivo

Fechar o fluxo de compra completo do storefront: carrinho persistente, wizard de checkout em 4 passos (endereço → frete → pagamento → revisão), tela de sucesso com confete e referência do pedido. Tudo consumindo as APIs já entregues nas Fases 3-4 (`/cart`, `/checkout/preview`, `/checkout/place-order`, `/customers/me/addresses`, `/customers/me/payment-methods`, `/customers/payment-methods/tokenize`, `/shipping/calculate`).

Não-objetivos (ficam para slice 4 ou fases futuras):
- CRUDs completos de endereço/cartão/pet em `/minha-conta/*` (slice 3 entrega só o cadastro inline durante o checkout).
- Timeline visual do pedido em `/minha-conta/pedidos/:numero`.
- Webhook de pagamento assíncrono PIX/BOLETO (entra na Fase 7+ com gateway real).
- Pipe DOMPurify para sanitização de HTML — entra no slice 4.

## 2. Decisões tomadas (durante o brainstorming)

| Decisão | Escolha | Justificativa |
|---|---|---|
| Persistência do wizard | `sessionStorage` sob chave `pethub:checkout:v1` | F5 mantém o passo. `localStorage` exporia dados entre abas/sessões; só-memória quebra UX no F5. |
| Cadastro inline de endereço/cartão | Inline no próprio passo do wizard | Slice 4 (que tem o CRUD `/minha-conta`) ainda não existe. Inline é o único caminho que permite primeira compra. |
| Onde aplicar cupom | Apenas em `/carrinho` | Padrão Magalu/Amazon BR; o cart guarda o código, o `preview` valida, todos os passos seguintes mostram o desconto. |
| Tela `/checkout/sucesso/:numero` | Confete + resumo + CTAs (Acompanhar disabled, Voltar habilitado) | Acompanhar pedido aponta para `/minha-conta/pedidos/:numero` (slice 4). Desabilitar com tooltip evita 404. |
| Tokenização de cartão | Mock client-side via `POST /customers/payment-methods/tokenize` (existente) | Confirmado por Ali. Quando entrar gateway real, fechar `PAY-1` de `appsec-pendencias.md`. |
| Mistura "selecionar método" + "cadastrar cartão" no passo 3 | Mantida numa tela só | UX do wizard já é longa; dividir em 2 telas dobra cliques no primeiro pedido. |

## 3. Arquitetura

### 3.1 Rotas novas

```
/carrinho                       (público — mostra empty/login se não autenticado)
/checkout/endereco              passo 1 (authGuard + checkoutStepGuard(['cart-not-empty']))
/checkout/frete                 passo 2 (authGuard + checkoutStepGuard(['enderecoEntregaId']))
/checkout/pagamento             passo 3 (authGuard + checkoutStepGuard(['opcaoFreteCodigo']))
/checkout/revisao               passo 4 (authGuard + checkoutStepGuard(['formaPagamentoId']))
/checkout/sucesso/:numero       terminal (authGuard)
```

`checkoutStepGuard` redireciona para o passo anterior incompleto.

### 3.2 Services novos (`core/services/`)

| Service | Estado | Responsabilidade |
|---|---|---|
| `CartService` | `cart = signal<CartResponse \| null>` | CRUD do carrinho. Cada mutação re-seta o signal com response do backend (single source of truth, sem otimismo local). |
| `CheckoutService` | (stateless) | Wrapper de `POST /checkout/preview` e `POST /checkout/place-order`. |
| `CheckoutStateService` | `state = signal<CheckoutState>` + `sessionStorage` | Gerencia o estado do wizard. `patch(partial)` + `clear()`. |
| `AddressService` | (stateless) | `GET/POST/PUT/DELETE /customers/me/addresses` + `GET /customers/cep/{cep}`. |
| `PaymentMethodService` | (stateless) | `GET/POST/DELETE /customers/me/payment-methods` + `POST /customers/payment-methods/tokenize`. |

Shape do `CheckoutState`:

```ts
interface CheckoutState {
  enderecoEntregaId: number | null;
  enderecoCobrancaId: number | null;
  opcaoFreteCodigo: string | null;
  opcaoFreteSnapshot: OpcaoFrete | null;
  formaPagamentoId: number | null;
  parcelas: number;             // default 1
  idempotencyKey: string | null;
}
```

Versão `v1` embutida na key (`pethub:checkout:v1`) permite bump futuro sem quebrar sessões antigas.

### 3.3 Guards

- `authGuard` (já existe) protege todas as rotas de checkout.
- `checkoutStepGuard(requiredKeys: (keyof CheckoutState)[])` — novo. Verifica que cada chave do array está populada; senão redireciona para o passo correspondente. Carrinho vazio em qualquer ponto do wizard → redireciona para `/carrinho`.

### 3.4 Trade-off explícito: signals + round-trip

Cada `+1/-1` num item dispara `PUT /cart/items/{sku}` e re-seta o signal com a resposta. Justificativa: backend é fonte da verdade de subtotal/validações; carrinhos típicos têm <5 itens; otimismo local + reconciliação seria over-engineering. Latência percebida mitigada por estado de loading no stepper (botões desabilitados enquanto requisição rola).

### 3.5 `environment.credentialedPaths` update

Adicionar `'/cart'`, `'/checkout'`, `'/customers'`. Catálogo público e `/shipping/calculate` permanecem **sem** cookie.

## 4. Componentes e fluxo de cada tela

### 4.1 `/carrinho` — público

**`CartPage`** (`features/cart/cart.page.ts`). Layout 2 colunas desktop, 1 coluna mobile.

Sub-componentes:
- `CartItemRow` — foto, nome, preço unitário, stepper qty (`-` / input / `+`), subtotal por linha, "remover".
- `CouponInput` — input + "Aplicar"; se aplicado mostra chip com `×`.
- `CartSummaryCard` — subtotal, aviso "frete calculado no checkout", CTA "Finalizar compra" → `/checkout/endereco`. Reutiliza `ShippingCalculatorComponent` (slice 2) para preview opcional.

Empty state: ícone + "Seu carrinho está vazio" + CTA "Explorar produtos" → `/produtos`.

Não-autenticado: painel "Faça login pra ver seu carrinho" + CTA `/login`.

### 4.2 `/checkout/endereco` — passo 1

**`CheckoutAddressPage`**. Carrega `addressService.list()` no init.

- Lista de endereços como radio cards; auto-seleciona o `padraoEntrega=true`.
- Botão "Adicionar novo endereço" expande form inline:
  - CEP (auto-fill via `/customers/cep/{cep}`), logradouro, número, complemento, bairro, cidade, UF (`<select>`), apelido, tipo.
  - Salvar → `POST /customers/me/addresses` → auto-seleciona o criado.
- Checkbox "Usar mesmo endereço para cobrança" default `true`. Desmarcar exibe segunda lista para escolher cobrança.
- Stepper visual `1•2•3•4` + CTA "Continuar para frete" → persiste `enderecoEntregaId` + `enderecoCobrancaId` no state → navega `/checkout/frete`.

Validação client: `@Pattern` de CEP (8 dígitos), UF enum. Backend revalida.

### 4.3 `/checkout/frete` — passo 2

**`CheckoutShippingPage`**. Chama `POST /shipping/calculate` com `cepDestino` (endereço selecionado) + itens do cart.

- 3 cards radio (SEDEX, TABELADO, MOTOBOY) — transportadora, prazo, valor.
- Auto-seleciona o mais barato. Motoboy não aparece se CEP fora de SP.
- CTA "Continuar para pagamento" → persiste `opcaoFreteCodigo` + snapshot → `/checkout/pagamento`.

Decisão: usar `/shipping/calculate` (cache Redis 1h) em vez de chamar `/checkout/preview` aqui. `preview` só no passo 4.

### 4.4 `/checkout/pagamento` — passo 3

**`CheckoutPaymentPage`**. Tabs: **Cartão** | **PIX** | **Boleto**.

**Tab Cartão:**
- Lista de cartões cadastrados (radio); auto-seleciona padrão.
- Dropdown de parcelas (1-12).
- "Adicionar novo cartão" inline:
  - Number (16 dígitos formatados `9999 9999 9999 9999`), nome impresso, validade `MM/AA`, CVV.
  - Submit:
    1. `POST /customers/payment-methods/tokenize` (PAN sai aqui, **só aqui**) → `{ token, bandeira, ultimosQuatroDigitos }`.
    2. `POST /customers/me/payment-methods` com `tipo=CARTAO_CREDITO`, `gatewayToken`, `bandeira`, `ultimosQuatroDigitos`, `nomeImpresso`, validade.
    3. Auto-seleciona o novo cartão.

**Tab PIX/Boleto:** card informativo único; submit do passo grava o `formaPagamentoId` correspondente (criado/buscado on-the-fly).

CTA "Continuar para revisão" → persiste `formaPagamentoId` + `parcelas` → `/checkout/revisao`.

### 4.5 `/checkout/revisao` — passo 4

**`CheckoutReviewPage`**. Chama `POST /checkout/preview` com `{ enderecoEntregaId, opcaoFreteCodigo, cupom }`.

- Resumo agrupado por seção (Endereço, Frete, Pagamento, Itens) com link "Editar" voltando ao passo. State preservado.
- Botão "Finalizar compra · `{total}`" — fluxo no §5.

### 4.6 `/checkout/sucesso/:numero` — terminal

**`CheckoutSuccessPage`**. Lê snapshot de `router.getCurrentNavigation().extras.state` ou fallback para `GET /orders/{numero}`.

- Hero: ✓ verde + "Pedido confirmado!" + número `PH-2026-XXXXXX`.
- Resumo: total, método, ETA.
- Confete: `canvas-confetti` disparado 1× no `ngOnInit` (200 partículas, 2.5s).
- CTAs: **"Acompanhar pedido"** disabled com tooltip "Disponível em breve" (slice 4); **"Voltar à loja"** habilitado.

## 5. Idempotência e fluxo de confirmação

```ts
async confirmar() {
  if (!state().idempotencyKey) {
    state.patch({ idempotencyKey: crypto.randomUUID() });
  }
  try {
    const resp = await firstValueFrom(checkoutService.placeOrder({
      ...state(),
      idempotencyKey: state().idempotencyKey,
    }));
    if (resp.status === 'APPROVED') {
      state.clear();
      router.navigate(['/checkout/sucesso', resp.referenciaPedido], { state: { snapshot: resp } });
    } else if (resp.status === 'REJECTED') {
      state.patch({ idempotencyKey: null });
      toast.error(detalheRejeicao(resp));
    }
  } catch (err) {
    // timeout, 5xx, offline — preserva idempotencyKey, deixa o usuário reenviar
    toast.error('Falha ao confirmar. Verifique sua conexão e tente novamente.');
  }
}
```

**Regra:** mesma `idempotencyKey` enquanto o motivo do erro é rede/transporte. Nova key após REJECTED semântico.

Sobre `PENDING`: o `MockPaymentGateway` determinístico (Fase 4) aprova PIX/Boleto direto, então no slice 3 só veremos `APPROVED` ou `REJECTED`. Quando entrar gateway real com webhook assíncrono (Fase 7+), a spec dessa fase adiciona o ramo `PENDING` + tela "aguardando confirmação". Não introduzir lógica especulativa agora.

## 6. Erros mapeados por endpoint

| Endpoint | HTTP | Tratamento UI |
|---|---|---|
| `GET /cart` | 401 | interceptor já trata refresh; se falhar, `/login` |
| `POST /cart/items` | 404 (SKU inexistente) | toast "Produto indisponível" + remove CTA no PDP |
| `POST /cart/items` | 422 (qty inválida) | erro inline no stepper |
| `POST /cart/coupon` | 422 (cupom inválido) | erro inline abaixo do input |
| `POST /customers/me/addresses` | 422 | erros campo a campo via `error.errors[]` |
| `GET /customers/cep/{cep}` | 422 | "CEP não encontrado, preencha manualmente" |
| `POST /shipping/calculate` | 422 (CEP inválido) | volta para passo 1, destaca endereço |
| `POST /customers/payment-methods/tokenize` | 422 (Luhn/expiry) | erro inline no form, input do número vermelho |
| `POST /checkout/preview` | 422 (cupom/endereço/cart vazio) | toast + redireciona ao passo correspondente |
| `POST /checkout/place-order` | 409 (idempotency-key conflict) | tratado como sucesso — backend devolve tentativa anterior |
| `POST /checkout/place-order` | 422 (estoque insuficiente) | dialog "Estoque mudou" + lista SKUs + CTA "Ajustar carrinho" |
| Qualquer 5xx ou timeout | — | toast genérico, preserva `idempotencyKey` |

Padrão: usar `error.detail` do Problem Details (RFC 7807); fallback por código HTTP. Stack trace nunca exibido.

### 6.1 Estoque mudou (race entre passo 4 e place-order)

Backend devolve 422 com lista de SKUs em falta. Frontend abre dialog modal "Estoque mudou enquanto você revisava. Item X com qty disponível Y. Ajuste seu carrinho para continuar." → CTA navega `/carrinho` com state preservado.

## 7. AppSec

| Componente | Mitigação | OWASP |
|---|---|---|
| Forms de cartão | PAN/CVV nunca em sessionStorage/localStorage. `[type="password"]` no CVV. `autocomplete="cc-number/cc-cvc/cc-exp"`. Submit limpa campos | PCI-DSS 3.4, A02:2021 |
| Tokenize | PAN trafega só em `POST /customers/payment-methods/tokenize`; nenhum request subsequente carrega PAN | PCI-DSS 3.4 |
| `idempotencyKey` | `crypto.randomUUID()` — entropy do navegador. Persiste em sessionStorage (escopo: mesma aba) | A07:2021 |
| Ownership | Backend valida (`OwnedXxx` helpers) — frontend não duplica; só evita expor controles | API1:2023 BOLA |
| Cookies de auth | `pethub_refresh` HttpOnly + SameSite=Lax + Path=/api/v1/auth (já configurado) | A07:2021 |
| CEP/UF input | CEP validado client (`\d{8}`) e server (`@Pattern`); UF como `<select>` enumerado, não input livre | A03:2021 |
| Cupom input | `\w{1,30}` client + server | A03:2021 |
| `CheckoutSuccessPage` | Snapshot via `router.state` ou `GET /orders/{numero}` (valida ownership). Não confia no `:numero` da URL | API1:2023 BOLA |
| Confete | `canvas-confetti` import estático (sem `eval`) | A06:2021 |
| Auto-fill CEP | Resposta ViaCEP renderizada como `value` de `<input>` (Angular escapa) | A03:2021 |

**Checkpoint do commit** (obrigatório por CLAUDE.md):
- A03:2021 Injection — escape default do Angular + validações client/server.
- A07:2021 Identification and Authentication Failures — cookie HttpOnly, token em memória.
- API1:2023 BOLA — backend valida ownership; frontend não expõe IDs sensíveis na URL além do que o backend protege.
- PCI-DSS 3.4 — PAN nunca persistido client-side; só trafega no tokenize.

**Premissas de infra/env** (não cobertas neste slice):
- Backend continua com defaults (CORS dev, JWT secret do `.env.local`).
- `pethub_refresh` cookie já está HttpOnly desde slice 1.
- Pendências `JWT-1` (iss/aud), `CORS-1` (validador prod), `VAL-1` (CSP nginx) continuam abertas — tornam-se bloqueantes só no caminho para staging.

## 8. Critérios de pronto

1. Smoke E2E manual completo (§10) passa na sequência sem reset de browser.
2. `mvn -B -ntp -pl application -DskipTests compile` verde.
3. `npx ng build --configuration production` no storefront termina sem erro.
4. README raiz + README do storefront atualizados (já feito nesta sessão).
5. Conventional Commit: `feat(storefront): slice 3 — carrinho + checkout 4 passos + sucesso com confete`.
6. Body do commit inclui seção `🛡️ OWASP & Security Checkpoint` listando A07/A03/API1/PCI-DSS 3.4.
7. `project_fase5_em_andamento.md` (memory) atualizado marcando slice 3 como entregue.

## 9. Arquivos novos

```
src/app/
├── core/
│   ├── guards/
│   │   └── checkout-step.guard.ts                       [NOVO]
│   ├── models/
│   │   ├── cart.ts                                      [NOVO]
│   │   ├── checkout.ts                                  [NOVO]
│   │   ├── address.ts                                   [NOVO]
│   │   └── payment-method.ts                            [NOVO]
│   └── services/
│       ├── cart.service.ts                              [NOVO]
│       ├── checkout.service.ts                          [NOVO]
│       ├── checkout-state.service.ts                    [NOVO]
│       ├── address.service.ts                           [NOVO]
│       └── payment-method.service.ts                    [NOVO]
├── features/
│   ├── cart/
│   │   └── cart.page.ts                                 [NOVO]
│   └── checkout/
│       ├── address.page.ts                              [NOVO]
│       ├── shipping.page.ts                             [NOVO]
│       ├── payment.page.ts                              [NOVO]
│       ├── review.page.ts                               [NOVO]
│       └── success.page.ts                              [NOVO]
└── shared/components/
    ├── cart-item-row/cart-item-row.component.ts         [NOVO]
    ├── coupon-input/coupon-input.component.ts           [NOVO]
    └── stepper/stepper.component.ts                     [NOVO]
```

Alterações em arquivos existentes:
- `src/app/app.routes.ts` — adiciona 6 rotas.
- `src/environments/environment.ts` (e `environment.development.ts`) — `credentialedPaths` ganha `/cart`, `/checkout`, `/customers`.
- `package.json` — adiciona `canvas-confetti` + `@types/canvas-confetti`.

## 10. Smoke E2E manual (checklist obrigatório antes do commit)

Setup: infra + backend + storefront rodando (instruções no README). Login Maria (`maria.fase2@pethub.com / Senha@123`).

### Carrinho
- [ ] PDP `/produtos/COLLAR-PRO-001` → "Adicionar" → contador no header incrementa.
- [ ] `/carrinho` mostra item, foto, preço, subtotal correto.
- [ ] Stepper `+`: dispara `PUT /cart/items/{sku}`; subtotal recalcula.
- [ ] Stepper `-` em qty=1: remove (`DELETE`); item some.
- [ ] Cupom `10OFF` aplicado → chip aparece.
- [ ] Cupom `EXPIRED` aplicado → backend aceita anexar (validação real no preview).
- [ ] Remover cupom via `×` → chip some.
- [ ] Logout → re-login → cart persiste (Redis TTL 30d).
- [ ] Não-autenticado em `/carrinho` → CTA login (sem 401 cru).

### Checkout passo 1 — endereço
- [ ] Sem endereço cadastrado → lista vazia + form inline aberto.
- [ ] CEP `01310-100` → auto-fill correto.
- [ ] CEP `00000-000` → 422 silencioso, campos manuais editáveis.
- [ ] Salvar → auto-seleciona, badge "padrão".
- [ ] Voltar/avançar mantém seleção (state).
- [ ] Desmarcar "mesmo para cobrança" → mostra segunda lista.

### Checkout passo 2 — frete
- [ ] Acesso direto sem passo 1 → redireciona passo 1.
- [ ] 3 cards (SEDEX, TABELADO, MOTOBOY se SP) com prazo/valor.
- [ ] CEP fora de SP → motoboy não aparece.
- [ ] Auto-seleção do mais barato.

### Checkout passo 3 — pagamento
- [ ] Acesso direto sem passo 2 → redireciona passo 2.
- [ ] Tab "Cartão" sem cartão cadastrado → form inline.
- [ ] Cartão `4111 1111 1111 1111` → tokenize 200, last4=`1111`, auto-selecionado.
- [ ] Cartão `4111 1111 1111 1112` (Luhn falha) → tokenize 422, erro inline.
- [ ] DevTools Network: PAN só na request `/tokenize`; nenhum request subsequente carrega PAN.
- [ ] DevTools Application: `sessionStorage` não contém PAN/CVV.
- [ ] Tabs PIX/Boleto → card informativo.

### Checkout passo 4 — revisão
- [ ] `POST /checkout/preview` 1x → breakdown completo.
- [ ] "Editar endereço" → passo 1 → trocar → voltar passo 4 → preview recalcula.
- [ ] Cupom `10OFF` → desconto aparece.
- [ ] Confirmar com `1111` → 200 + redirect `/checkout/sucesso/PH-2026-XXXXXX`.
- [ ] Carrinho limpa após APPROVED.
- [ ] `idempotencyKey` no sessionStorage antes do submit; some após APPROVED.
- [ ] Cartão `4000` (rejeita determinístico) → toast erro, permanece na revisão, nova `idempotencyKey`.

### Idempotência
- [ ] Slow 3G + "Finalizar" → request demora, sem clicar de novo.
- [ ] Offline no meio → toast erro, `idempotencyKey` preservada → online + reclicar = mesma key.
- [ ] Duplo-clique sem throttle → loading bloqueia 2º clique; só 1 request.

### Estoque mudou (race simulada)
- [ ] `UPDATE estoque SET quantidade = 0 WHERE produto_id = 1` durante revisão.
- [ ] Clicar "Finalizar" → 422 → dialog "Estoque mudou" → CTA "Ajustar carrinho".
- [ ] Restaurar: `UPDATE estoque SET quantidade = 50 WHERE produto_id = 1`.

### Sucesso
- [ ] Mostra número + total + método.
- [ ] Confete dispara 1× (não em loop).
- [ ] "Acompanhar pedido" disabled + tooltip.
- [ ] "Voltar à loja" → `/`.

### AppSec spot-check
- [ ] Cookies: `pethub_refresh` HttpOnly + SameSite=Lax.
- [ ] SessionStorage: `pethub:checkout:v1` existe durante wizard, sem PAN/CVV/token.
- [ ] LocalStorage: vazio ou só user-prefs.
- [ ] Network `/place-order` body: tem `formaPagamentoId`, não tem campos de cartão.
- [ ] Console: zero erros não-handled.

## 11. Dívida explícita (para `fase-5-pendencias.md` no fechamento)

Testes Karma/Jest formais que ficam pendentes:
- `CartServiceTest`: addItem, updateQty, applyCoupon, error mapping.
- `CheckoutStateServiceTest`: persist/load do sessionStorage, version key v1, clear.
- `CheckoutStepGuardTest`: cada combinação de state vs passo destino.
- `CheckoutReviewPageTest`: preview success/422, place-order APPROVED/REJECTED/PENDING, idempotency-key replay, dialog de estoque mudou.
- `PaymentMethodFormTest`: Luhn client-side, mascaramento PAN, tokenize wired.
- Cypress/Playwright E2E: automatizar §10.

Mesmo bloqueio que mantém testes do backend como dívida (sem CI rodando ainda) — fica para a sessão de fechamento da Fase 5 + setup de CI da Fase 9.

## 12. Referências

- API DTOs do backend: `backend/cart/`, `backend/checkout/`, `backend/customer/` (DTOs já validados E2E nas Fases 2-4).
- Slices anteriores: `7c37102 feat(identity)`, `2778cf8 feat(storefront) slice 1`, `167a887 feat(storefront) slice 2`.
- Roadmap fonte de verdade: `ai-memory/roadmap/plano-completo.md` (Fase 5).
- AppSec guidelines: `ai-memory/architecture/appsec-guidelines.md`.
- Design tokens: `ai-memory/design-system.md`.
