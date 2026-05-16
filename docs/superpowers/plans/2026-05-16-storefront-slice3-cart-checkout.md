# Storefront Slice 3 — Cart + Checkout 4-step Wizard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver the cart page, the 4-step checkout wizard (address → shipping → payment → review), and the success page with confetti on top of the existing Angular 17 storefront.

**Architecture:** Standalone components + signals for state; `sessionStorage` (key `pethub:checkout:v1`) backs the wizard state across F5; backend (already complete since Phase 4) remains the source of truth for cart, preview, and place-order. Inline registration of address/card inside the wizard avoids depending on slice 4. Coupon is applied only at `/carrinho`; the wizard surfaces the discount via `/checkout/preview`.

**Tech Stack:** Angular 17 standalone, signals, Reactive Forms, TailwindCSS, `canvas-confetti`. Proxies `/api/*` to Spring Boot at `localhost:8080` via `proxy.conf.json`. Auth interceptor (slice 1) handles 401 refresh transparently; auth guard (slice 1) protects routed pages.

**Spec:** [`docs/superpowers/specs/2026-05-16-storefront-slice3-cart-checkout-design.md`](../specs/2026-05-16-storefront-slice3-cart-checkout-design.md)

**Conventions reused from slices 1-2:**
- Page files named `*.page.ts` under `features/<feature>/`.
- Services under `core/services/`, models under `core/models/`, guards under `core/guards/`.
- Tailwind utility classes only; no inline styles.
- `withCredentials` is already wired for `/cart`, `/checkout`, `/customers`, `/orders` via `environment.credentialedPaths`.
- Backend RFC 7807 Problem Details: error body has `{ status, detail, errors? }` — UI consumes `error.detail` first, falls back to HTTP-code messages.
- **Testes formais Karma/Jest seguem dívida acumulada da Fase 5** (spec §11). Cada task aqui usa `ng build` como typecheck e o smoke E2E manual da Task 15 como aceitação final. Não introduzir Karma neste slice — abre escopo.

**Working directory:** `/home/ali/projects/pet-hub/frontend/storefront/` (todas as paths abaixo são relativas a esse diretório salvo nota em contrário).

**Verification harness (recall from README):**
```bash
# 1. infra
docker compose --env-file /home/ali/projects/pet-hub/.env.local \
  -f /home/ali/projects/pet-hub/infrastructure/docker/docker-compose.dev.yml up -d
# 2. backend (já está rodando se você seguiu o setup do README)
docker ps --filter name=pethub-app-dev
# 3. storefront
cd /home/ali/projects/pet-hub/frontend/storefront
npx ng serve --host 127.0.0.1 --port 4242 --proxy-config proxy.conf.json
```
Storefront → http://127.0.0.1:4242 · Swagger → http://localhost:8080/swagger-ui.html · login Maria `maria.fase2@pethub.com / Senha@123`.

---

## File Structure

### New files
```
src/app/
├── core/
│   ├── guards/
│   │   └── checkout-step.guard.ts                   [Task 6]
│   ├── models/
│   │   ├── cart.ts                                  [Task 1]
│   │   ├── checkout.ts                              [Task 1]
│   │   ├── address.ts                               [Task 1]
│   │   └── payment-method.ts                        [Task 1]
│   └── services/
│       ├── address.service.ts                       [Task 2]
│       ├── payment-method.service.ts                [Task 2]
│       ├── checkout.service.ts                      [Task 3]
│       ├── cart.service.ts                          [Task 4]
│       └── checkout-state.service.ts                [Task 5]
├── features/
│   ├── cart/
│   │   └── cart.page.ts                             [Task 9]
│   └── checkout/
│       ├── address.page.ts                          [Task 10]
│       ├── shipping.page.ts                         [Task 11]
│       ├── payment.page.ts                          [Task 12]
│       ├── review.page.ts                           [Task 13]
│       └── success.page.ts                          [Task 14]
└── shared/components/
    ├── stepper/stepper.component.ts                 [Task 7]
    ├── cart-item-row/cart-item-row.component.ts     [Task 8]
    └── coupon-input/coupon-input.component.ts       [Task 8]
```

### Modified files
- `src/app/app.routes.ts` — add 6 new routes (cart + 5 checkout pages). [Task 9 + Task 14]
- `src/app/shared/layout/main-layout.component.ts` — add cart badge. [Task 9]
- `src/app/features/catalog/product-detail.page.ts` — replace mocked `adicionarAoCarrinho()` with real `CartService` call. [Task 9]
- `package.json` — add `canvas-confetti` + `@types/canvas-confetti`. [Task 1]

`environment.credentialedPaths` already covers `/cart`, `/checkout`, `/customers`, `/orders` — **no change needed**.

---

## Task 1: Foundation — models + dependency

**Files:**
- Create: `src/app/core/models/cart.ts`
- Create: `src/app/core/models/checkout.ts`
- Create: `src/app/core/models/address.ts`
- Create: `src/app/core/models/payment-method.ts`
- Modify: `package.json` (add canvas-confetti)

- [ ] **Step 1: Create cart models**

Path: `src/app/core/models/cart.ts`

```ts
/**
 * Espelha os records do backend (cart/domain/*.java).
 * BigDecimal vira number aqui; sempre formatar com pipe `currency`.
 */
export interface CartItem {
  produtoId: number;
  sku: string;
  nome: string;
  imagemUrl: string | null;
  qty: number;
  precoUnitario: number;
  subtotal: number;
}

export interface CartCoupon {
  codigo: string;
  descontoAplicado: number;
}

export interface CartResponse {
  userId: number;
  items: CartItem[];
  cupom: CartCoupon | null;
  subtotal: number;
  totalItens: number;
  atualizadoEm: string;        // ISO LocalDateTime
}

export interface AddItemRequest {
  sku: string;
  qty: number;
}

export interface UpdateQtyRequest {
  qty: number;
}

export interface ApplyCouponRequest {
  codigo: string;
}
```

- [ ] **Step 2: Create checkout models**

Path: `src/app/core/models/checkout.ts`

```ts
import { CartItem } from './cart';
import { OpcaoFrete } from './shipping';

/** Quebra de impostos retornada pelo backend (pricing/dto/ImpostosCalculados). */
export interface ImpostosCalculados {
  valorImpostos: number;
  detalhes: Array<{ ncm: string; uf: string; aliquotaPercentual: number; valor: number }>;
}

export interface CheckoutPreviewRequest {
  enderecoEntregaId: number;
  opcaoFreteCodigo: string;
  cupom?: string | null;
}

export interface CheckoutPreviewResponse {
  itens: CartItem[];
  subtotal: number;
  descontoPromocoes: number;
  descontoCupom: number;
  impostosCalculados: ImpostosCalculados;
  freteEscolhido: OpcaoFrete;
  valorTotal: number;
  cupomCodigo: string | null;
}

export type PaymentMethodKind = 'CARTAO_CREDITO' | 'CARTAO_DEBITO' | 'PIX' | 'BOLETO';
export type PaymentStatus = 'PROCESSING' | 'APPROVED' | 'REJECTED' | 'REFUNDED';

export interface PlaceOrderRequest {
  enderecoEntregaId: number;
  enderecoCobrancaId: number;
  opcaoFreteCodigo: string;
  formaPagamentoId: number;
  cupom?: string | null;
  parcelas?: number | null;
  idempotencyKey: string;
}

export interface PlaceOrderResponse {
  tentativaPagamentoId: number;
  referenciaPedido: string;
  pedidoId: number | null;
  metodo: PaymentMethodKind;
  status: PaymentStatus;
  valorTotal: number;
  gatewayTransactionId: string | null;
  qrCode: string | null;
  boletoUrl: string | null;
}

/**
 * Stored in sessionStorage between wizard steps.
 * Bump the storage key to v2 when this shape changes incompatibly.
 */
export interface CheckoutState {
  enderecoEntregaId: number | null;
  enderecoCobrancaId: number | null;
  opcaoFreteCodigo: string | null;
  opcaoFreteSnapshot: OpcaoFrete | null;
  formaPagamentoId: number | null;
  parcelas: number;
  idempotencyKey: string | null;
}

export const EMPTY_CHECKOUT_STATE: CheckoutState = {
  enderecoEntregaId: null,
  enderecoCobrancaId: null,
  opcaoFreteCodigo: null,
  opcaoFreteSnapshot: null,
  formaPagamentoId: null,
  parcelas: 1,
  idempotencyKey: null,
};
```

- [ ] **Step 3: Create address models**

Path: `src/app/core/models/address.ts`

```ts
export type UnidadeFederativa =
  | 'AC' | 'AL' | 'AP' | 'AM' | 'BA' | 'CE' | 'DF' | 'ES' | 'GO' | 'MA'
  | 'MT' | 'MS' | 'MG' | 'PA' | 'PB' | 'PR' | 'PE' | 'PI' | 'RJ' | 'RN'
  | 'RS' | 'RO' | 'RR' | 'SC' | 'SP' | 'SE' | 'TO';

export type TipoEndereco = 'RESIDENCIAL' | 'COMERCIAL';

export interface EnderecoResponse {
  id: number;
  apelido: string;
  cep: string;
  logradouro: string;
  numero: string | null;
  complemento: string | null;
  bairro: string;
  cidade: string;
  uf: UnidadeFederativa;
  pais: string;
  tipo: TipoEndereco;
  padraoEntrega: boolean;
  padraoCobranca: boolean;
  ativo: boolean;
}

export interface CreateEnderecoRequest {
  apelido: string;
  cep: string;                   // 8 dígitos numéricos (sem máscara)
  logradouro: string;
  numero?: string;
  complemento?: string;
  bairro: string;
  cidade: string;
  uf: UnidadeFederativa;
  tipo: TipoEndereco;
  padraoEntrega?: boolean;
  padraoCobranca?: boolean;
}

export interface ViaCepResponse {
  cep: string;
  logradouro: string;
  complemento: string;
  bairro: string;
  cidade: string;
  uf: string;
  erro: boolean | null;
}

export const UFS: readonly UnidadeFederativa[] = [
  'AC','AL','AP','AM','BA','CE','DF','ES','GO','MA',
  'MT','MS','MG','PA','PB','PR','PE','PI','RJ','RN',
  'RS','RO','RR','SC','SP','SE','TO',
];
```

- [ ] **Step 4: Create payment-method models**

Path: `src/app/core/models/payment-method.ts`

```ts
export type Bandeira = 'VISA' | 'MASTER' | 'AMEX' | 'ELO' | 'HIPERCARD' | 'OUTRO';
export type TipoPagamento = 'CARTAO_CREDITO' | 'CARTAO_DEBITO' | 'PIX' | 'BOLETO';

export interface FormaPagamentoResponse {
  id: number;
  tipo: TipoPagamento;
  apelido: string | null;
  bandeira: Bandeira | null;
  ultimosQuatroDigitos: string | null;
  nomeImpresso: string | null;
  validadeMes: number | null;
  validadeAno: number | null;
  padrao: boolean;
  ativo: boolean;
}

export interface CreateFormaPagamentoRequest {
  tipo: TipoPagamento;
  apelido?: string;
  gatewayToken?: string;
  bandeira?: Bandeira;
  ultimosQuatroDigitos?: string;
  nomeImpresso?: string;
  validadeMes?: number;
  validadeAno?: number;
  padrao?: boolean;
}

export interface TokenizeCardRequest {
  numero: string;                  // 13–19 dígitos, sem máscara
  cvv: string;                     // 3–4 dígitos
  nomeImpresso: string;
  validadeMes: number;             // 1–12
  validadeAno: number;             // 2024–2100
}

export interface TokenizeCardResponse {
  token: string;
  bandeira: Bandeira;
  ultimosQuatroDigitos: string;
}
```

- [ ] **Step 5: Add canvas-confetti dependency**

Run from `frontend/storefront/`:
```bash
npm install canvas-confetti
npm install --save-dev @types/canvas-confetti
```

Expected: both packages added to `package.json` + `package-lock.json` updated; no errors.

- [ ] **Step 6: Typecheck**

Run from `frontend/storefront/`:
```bash
npx ng build --configuration development
```
Expected: BUILD SUCCESS (only the new model files exist; no consumers yet, so output identical to prior builds modulo the lockfile).

- [ ] **Step 7: Commit**

```bash
cd /home/ali/projects/pet-hub && git add frontend/storefront/src/app/core/models frontend/storefront/package.json frontend/storefront/package-lock.json
git commit -m "feat(storefront): add cart/checkout/address/payment models + canvas-confetti dep"
```

---

## Task 2: AddressService + PaymentMethodService (stateless)

**Files:**
- Create: `src/app/core/services/address.service.ts`
- Create: `src/app/core/services/payment-method.service.ts`

- [ ] **Step 1: Create AddressService**

Path: `src/app/core/services/address.service.ts`

```ts
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  CreateEnderecoRequest,
  EnderecoResponse,
  ViaCepResponse,
} from '../models/address';

@Injectable({ providedIn: 'root' })
export class AddressService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiBase;

  list(): Observable<EnderecoResponse[]> {
    return this.http.get<EnderecoResponse[]>(`${this.api}/customers/me/addresses`);
  }

  create(req: CreateEnderecoRequest): Observable<EnderecoResponse> {
    return this.http.post<EnderecoResponse>(`${this.api}/customers/me/addresses`, req);
  }

  /** CEP com 8 dígitos numéricos, sem máscara. */
  lookupCep(cep: string): Observable<ViaCepResponse> {
    return this.http.get<ViaCepResponse>(`${this.api}/customers/cep/${cep}`);
  }
}
```

- [ ] **Step 2: Create PaymentMethodService**

Path: `src/app/core/services/payment-method.service.ts`

```ts
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  CreateFormaPagamentoRequest,
  FormaPagamentoResponse,
  TokenizeCardRequest,
  TokenizeCardResponse,
} from '../models/payment-method';

@Injectable({ providedIn: 'root' })
export class PaymentMethodService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiBase;

  list(): Observable<FormaPagamentoResponse[]> {
    return this.http.get<FormaPagamentoResponse[]>(`${this.api}/customers/me/payment-methods`);
  }

  create(req: CreateFormaPagamentoRequest): Observable<FormaPagamentoResponse> {
    return this.http.post<FormaPagamentoResponse>(`${this.api}/customers/me/payment-methods`, req);
  }

  /**
   * Trafega PAN + CVV. Deve ser a única request que carrega esses campos.
   * Resposta: token opaco + bandeira + últimos 4. Persistir só o resultado.
   */
  tokenize(req: TokenizeCardRequest): Observable<TokenizeCardResponse> {
    return this.http.post<TokenizeCardResponse>(
      `${this.api}/customers/payment-methods/tokenize`,
      req,
    );
  }
}
```

- [ ] **Step 3: Typecheck**

```bash
npx ng build --configuration development
```
Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
cd /home/ali/projects/pet-hub && git add frontend/storefront/src/app/core/services/address.service.ts frontend/storefront/src/app/core/services/payment-method.service.ts
git commit -m "feat(storefront): add AddressService and PaymentMethodService"
```

---

## Task 3: CheckoutService (preview + place-order)

**Files:**
- Create: `src/app/core/services/checkout.service.ts`

- [ ] **Step 1: Create CheckoutService**

Path: `src/app/core/services/checkout.service.ts`

```ts
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  CheckoutPreviewRequest,
  CheckoutPreviewResponse,
  PlaceOrderRequest,
  PlaceOrderResponse,
} from '../models/checkout';

@Injectable({ providedIn: 'root' })
export class CheckoutService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiBase;

  preview(req: CheckoutPreviewRequest): Observable<CheckoutPreviewResponse> {
    return this.http.post<CheckoutPreviewResponse>(`${this.api}/checkout/preview`, req);
  }

  placeOrder(req: PlaceOrderRequest): Observable<PlaceOrderResponse> {
    return this.http.post<PlaceOrderResponse>(`${this.api}/checkout/place-order`, req);
  }
}
```

- [ ] **Step 2: Typecheck**
```bash
npx ng build --configuration development
```
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**
```bash
cd /home/ali/projects/pet-hub && git add frontend/storefront/src/app/core/services/checkout.service.ts
git commit -m "feat(storefront): add CheckoutService (preview + place-order)"
```

---

## Task 4: CartService (signal-backed, backend is source of truth)

**Files:**
- Create: `src/app/core/services/cart.service.ts`

- [ ] **Step 1: Create CartService**

Path: `src/app/core/services/cart.service.ts`

```ts
import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, tap } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  AddItemRequest,
  ApplyCouponRequest,
  CartResponse,
  UpdateQtyRequest,
} from '../models/cart';

/**
 * CartService — fonte da verdade do carrinho no client.
 *
 * Estratégia: backend (Redis TTL 30d) é a fonte da verdade. Cada mutação
 * dispara um HTTP e re-seta o signal com a resposta. Sem otimismo local —
 * subtotal/validações vêm prontos da API.
 */
@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiBase;

  private readonly _cart = signal<CartResponse | null>(null);

  readonly cart = this._cart.asReadonly();
  readonly totalItens = computed(() => this._cart()?.totalItens ?? 0);
  readonly subtotal = computed(() => this._cart()?.subtotal ?? 0);
  readonly isEmpty = computed(() => (this._cart()?.items.length ?? 0) === 0);

  load(): Observable<CartResponse> {
    return this.http
      .get<CartResponse>(`${this.api}/cart`)
      .pipe(tap((c) => this._cart.set(c)));
  }

  add(sku: string, qty: number): Observable<CartResponse> {
    const body: AddItemRequest = { sku, qty };
    return this.http
      .post<CartResponse>(`${this.api}/cart/items`, body)
      .pipe(tap((c) => this._cart.set(c)));
  }

  updateQty(sku: string, qty: number): Observable<CartResponse> {
    const body: UpdateQtyRequest = { qty };
    return this.http
      .put<CartResponse>(`${this.api}/cart/items/${encodeURIComponent(sku)}`, body)
      .pipe(tap((c) => this._cart.set(c)));
  }

  remove(sku: string): Observable<CartResponse> {
    return this.http
      .delete<CartResponse>(`${this.api}/cart/items/${encodeURIComponent(sku)}`)
      .pipe(tap((c) => this._cart.set(c)));
  }

  applyCoupon(codigo: string): Observable<CartResponse> {
    const body: ApplyCouponRequest = { codigo };
    return this.http
      .post<CartResponse>(`${this.api}/cart/coupon`, body)
      .pipe(tap((c) => this._cart.set(c)));
  }

  removeCoupon(): Observable<CartResponse> {
    return this.http
      .delete<CartResponse>(`${this.api}/cart/coupon`)
      .pipe(tap((c) => this._cart.set(c)));
  }

  /** Limpa local sem chamar backend — usado após place-order APPROVED (o backend já zerou). */
  clearLocal(): void {
    this._cart.set(null);
  }

  /** Limpa local + chama backend. Para "esvaziar carrinho" pelo usuário (não usado no slice 3). */
  clear(): Observable<void> {
    return this.http.delete<void>(`${this.api}/cart`).pipe(tap(() => this._cart.set(null)));
  }
}
```

- [ ] **Step 2: Typecheck**
```bash
npx ng build --configuration development
```
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**
```bash
cd /home/ali/projects/pet-hub && git add frontend/storefront/src/app/core/services/cart.service.ts
git commit -m "feat(storefront): add CartService backed by signals + Redis-backed cart API"
```

---

## Task 5: CheckoutStateService (sessionStorage wizard state)

**Files:**
- Create: `src/app/core/services/checkout-state.service.ts`

- [ ] **Step 1: Create CheckoutStateService**

Path: `src/app/core/services/checkout-state.service.ts`

```ts
import { Injectable, signal } from '@angular/core';

import { CheckoutState, EMPTY_CHECKOUT_STATE } from '../models/checkout';

const STORAGE_KEY = 'pethub:checkout:v1';

/**
 * Estado do wizard de checkout. Persiste em sessionStorage (mesma aba),
 * sobrevive a F5, some quando a aba fecha ou após place-order APPROVED.
 *
 * AppSec: dados aqui são apenas IDs (endereço, frete, forma de pagamento) +
 * idempotencyKey gerada com crypto.randomUUID(). NUNCA persistir PAN, CVV
 * ou gatewayToken aqui.
 */
@Injectable({ providedIn: 'root' })
export class CheckoutStateService {
  private readonly _state = signal<CheckoutState>(this.loadFromStorage());

  readonly state = this._state.asReadonly();

  /** Atualiza um subset do state e persiste imediatamente. */
  patch(partial: Partial<CheckoutState>): void {
    const next = { ...this._state(), ...partial };
    this._state.set(next);
    this.persist(next);
  }

  /** Garante que existe uma idempotencyKey; cria com crypto.randomUUID() se faltar. */
  ensureIdempotencyKey(): string {
    const current = this._state().idempotencyKey;
    if (current) return current;
    const key = crypto.randomUUID();
    this.patch({ idempotencyKey: key });
    return key;
  }

  /** Limpa em memória e em sessionStorage. */
  clear(): void {
    this._state.set({ ...EMPTY_CHECKOUT_STATE });
    if (typeof sessionStorage !== 'undefined') {
      sessionStorage.removeItem(STORAGE_KEY);
    }
  }

  private loadFromStorage(): CheckoutState {
    if (typeof sessionStorage === 'undefined') return { ...EMPTY_CHECKOUT_STATE };
    try {
      const raw = sessionStorage.getItem(STORAGE_KEY);
      if (!raw) return { ...EMPTY_CHECKOUT_STATE };
      const parsed = JSON.parse(raw) as Partial<CheckoutState>;
      // Mescla com defaults para tolerar adições de campo no slice 4
      return { ...EMPTY_CHECKOUT_STATE, ...parsed };
    } catch {
      return { ...EMPTY_CHECKOUT_STATE };
    }
  }

  private persist(state: CheckoutState): void {
    if (typeof sessionStorage === 'undefined') return;
    try {
      sessionStorage.setItem(STORAGE_KEY, JSON.stringify(state));
    } catch {
      // sessionStorage cheio ou bloqueado — degradação silenciosa, UX volta a "só memória"
    }
  }
}
```

- [ ] **Step 2: Typecheck**
```bash
npx ng build --configuration development
```
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**
```bash
cd /home/ali/projects/pet-hub && git add frontend/storefront/src/app/core/services/checkout-state.service.ts
git commit -m "feat(storefront): add CheckoutStateService persisting wizard state in sessionStorage"
```

---

## Task 6: checkoutStepGuard

**Files:**
- Create: `src/app/core/guards/checkout-step.guard.ts`

- [ ] **Step 1: Create the guard**

Path: `src/app/core/guards/checkout-step.guard.ts`

```ts
import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';

import { CheckoutStateService } from '../services/checkout-state.service';
import { CartService } from '../services/cart.service';
import { CheckoutState } from '../models/checkout';

/**
 * Cada chave precisa estar populada no CheckoutState para o passo correspondente.
 *
 *  /checkout/endereco   → ['cart-not-empty']
 *  /checkout/frete      → ['enderecoEntregaId']
 *  /checkout/pagamento  → ['opcaoFreteCodigo']
 *  /checkout/revisao    → ['formaPagamentoId']
 *
 * Carrinho vazio em qualquer passo → /carrinho.
 * Pré-requisito faltando → redireciona para o último passo válido.
 */
type StepRequirement = keyof CheckoutState | 'cart-not-empty';

export function checkoutStepGuard(required: StepRequirement[]): CanActivateFn {
  return () => {
    const cart = inject(CartService);
    const state = inject(CheckoutStateService);
    const router = inject(Router);

    if (cart.isEmpty()) {
      router.navigate(['/carrinho']);
      return false;
    }

    const s = state.state();
    for (const req of required) {
      if (req === 'cart-not-empty') continue;
      if (!s[req]) {
        router.navigate([destinoPara(req)]);
        return false;
      }
    }
    return true;
  };
}

function destinoPara(missing: keyof CheckoutState): string {
  switch (missing) {
    case 'enderecoEntregaId':
    case 'enderecoCobrancaId':
      return '/checkout/endereco';
    case 'opcaoFreteCodigo':
    case 'opcaoFreteSnapshot':
      return '/checkout/frete';
    case 'formaPagamentoId':
    case 'parcelas':
      return '/checkout/pagamento';
    default:
      return '/carrinho';
  }
}
```

- [ ] **Step 2: Typecheck**
```bash
npx ng build --configuration development
```
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**
```bash
cd /home/ali/projects/pet-hub && git add frontend/storefront/src/app/core/guards/checkout-step.guard.ts
git commit -m "feat(storefront): add checkoutStepGuard for wizard navigation"
```

---

## Task 7: Shared component — StepperComponent

**Files:**
- Create: `src/app/shared/components/stepper/stepper.component.ts`

- [ ] **Step 1: Create StepperComponent**

Path: `src/app/shared/components/stepper/stepper.component.ts`

```ts
import { Component, computed, input } from '@angular/core';
import { CommonModule } from '@angular/common';

interface Step {
  label: string;
  /** Rota correspondente (opcional — usado se um passo concluído for clicável no futuro). */
  route?: string;
}

/**
 * Barra visual de progresso 1•2•3•4 do checkout.
 *
 *  active = 1-based index do passo atual.
 *  Passos antes do active ficam check; o active fica destacado; depois ficam mutados.
 */
@Component({
  selector: 'app-stepper',
  standalone: true,
  imports: [CommonModule],
  template: `
    <ol class="flex w-full items-center gap-2 sm:gap-4">
      <li *ngFor="let step of steps(); let i = index"
          class="flex flex-1 items-center gap-2">
        <span class="flex h-7 w-7 flex-shrink-0 items-center justify-center
                     rounded-full text-xs font-semibold"
              [class.bg-coral-500]="i + 1 === active()"
              [class.text-white]="i + 1 === active()"
              [class.bg-graphite-900]="i + 1 < active()"
              [class.bg-graphite-200]="i + 1 > active()"
              [class.text-graphite-500]="i + 1 > active()">
          <ng-container *ngIf="i + 1 < active(); else numeric">✓</ng-container>
          <ng-template #numeric>{{ i + 1 }}</ng-template>
        </span>
        <span class="hidden text-xs sm:inline"
              [class.text-graphite-900]="i + 1 === active()"
              [class.font-medium]="i + 1 === active()"
              [class.text-graphite-500]="i + 1 !== active()">
          {{ step.label }}
        </span>
        <span *ngIf="i + 1 < steps().length"
              class="flex-1 h-px"
              [class.bg-graphite-900]="i + 1 < active()"
              [class.bg-graphite-200]="i + 1 >= active()"></span>
      </li>
    </ol>
  `,
})
export class StepperComponent {
  steps = input.required<Step[]>();
  active = input.required<number>();
}
```

- [ ] **Step 2: Typecheck**
```bash
npx ng build --configuration development
```
Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**
```bash
cd /home/ali/projects/pet-hub && git add frontend/storefront/src/app/shared/components/stepper
git commit -m "feat(storefront): add StepperComponent for checkout progress"
```

---

## Task 8: Shared components — CartItemRow + CouponInput

**Files:**
- Create: `src/app/shared/components/cart-item-row/cart-item-row.component.ts`
- Create: `src/app/shared/components/coupon-input/coupon-input.component.ts`

- [ ] **Step 1: Create CartItemRowComponent**

Path: `src/app/shared/components/cart-item-row/cart-item-row.component.ts`

```ts
import { Component, computed, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

import { CartItem } from '../../../core/models/cart';

/**
 * Linha do carrinho. Stepper interno de qty desabilita botões durante a
 * requisição (o parent dispara CartService.updateQty e re-habilita ao terminar).
 */
@Component({
  selector: 'app-cart-item-row',
  standalone: true,
  imports: [CommonModule],
  template: `
    <article class="flex gap-4 border-b border-graphite-200 py-4">
      <div class="h-24 w-24 flex-shrink-0 overflow-hidden rounded bg-graphite-100">
        <img *ngIf="item().imagemUrl; else ph"
             [src]="item().imagemUrl!"
             [alt]="item().nome"
             class="h-full w-full object-cover" />
        <ng-template #ph>
          <div class="flex h-full w-full items-center justify-center text-xs text-graphite-400">sem imagem</div>
        </ng-template>
      </div>
      <div class="flex flex-1 flex-col justify-between">
        <div class="flex justify-between gap-4">
          <div>
            <p class="text-xs text-graphite-500">SKU {{ item().sku }}</p>
            <h3 class="text-base text-graphite-900">{{ item().nome }}</h3>
            <p class="mt-1 text-sm text-graphite-500">
              {{ item().precoUnitario | currency:'BRL':'symbol':'1.2-2':'pt-BR' }} cada
            </p>
          </div>
          <p class="text-base font-semibold text-graphite-900 whitespace-nowrap">
            {{ item().subtotal | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}
          </p>
        </div>
        <div class="mt-3 flex items-center justify-between">
          <div class="flex items-center gap-2">
            <button type="button" class="btn-secondary px-2 py-1 text-sm"
                    [disabled]="busy()"
                    (click)="onDec()"
                    aria-label="Diminuir quantidade">−</button>
            <span class="min-w-[2ch] text-center text-sm tabular-nums">{{ item().qty }}</span>
            <button type="button" class="btn-secondary px-2 py-1 text-sm"
                    [disabled]="busy()"
                    (click)="onInc()"
                    aria-label="Aumentar quantidade">+</button>
          </div>
          <button type="button" class="text-sm text-graphite-500 hover:text-coral-500"
                  [disabled]="busy()"
                  (click)="remove.emit(item().sku)">
            Remover
          </button>
        </div>
      </div>
    </article>
  `,
})
export class CartItemRowComponent {
  item = input.required<CartItem>();
  busy = input(false);
  updateQty = output<{ sku: string; qty: number }>();
  remove = output<string>();

  onInc(): void {
    this.updateQty.emit({ sku: this.item().sku, qty: this.item().qty + 1 });
  }
  onDec(): void {
    const next = this.item().qty - 1;
    if (next < 1) {
      this.remove.emit(this.item().sku);
    } else {
      this.updateQty.emit({ sku: this.item().sku, qty: next });
    }
  }
}
```

- [ ] **Step 2: Create CouponInputComponent**

Path: `src/app/shared/components/coupon-input/coupon-input.component.ts`

```ts
import { Component, inject, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { CartCoupon } from '../../../core/models/cart';

/**
 * Input de cupom. Quando há cupom aplicado, mostra chip com `×`.
 * Cupom é só código (\w{1,30}); backend valida no preview do checkout.
 */
@Component({
  selector: 'app-coupon-input',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <ng-container *ngIf="!cupom(); else aplicado">
      <form [formGroup]="form" (ngSubmit)="onApply()" class="space-y-2">
        <label class="label" for="cupom">Cupom de desconto</label>
        <div class="flex gap-2">
          <input id="cupom" type="text"
                 class="input flex-1 uppercase"
                 formControlName="codigo"
                 placeholder="10OFF" />
          <button type="submit"
                  class="btn-secondary"
                  [disabled]="form.invalid || busy()">
            <span *ngIf="!busy()">Aplicar</span>
            <span *ngIf="busy()">…</span>
          </button>
        </div>
        <p *ngIf="error()" class="form-error">{{ error() }}</p>
      </form>
    </ng-container>
    <ng-template #aplicado>
      <div class="flex items-center justify-between rounded border border-graphite-200 px-3 py-2">
        <div>
          <p class="text-sm font-medium text-graphite-900">{{ cupom()!.codigo }}</p>
          <p class="text-xs text-graphite-500">Validade verificada no checkout</p>
        </div>
        <button type="button"
                class="text-graphite-500 hover:text-coral-500"
                [disabled]="busy()"
                (click)="remove.emit()"
                aria-label="Remover cupom">×</button>
      </div>
    </ng-template>
  `,
})
export class CouponInputComponent {
  cupom = input<CartCoupon | null>(null);
  busy = input(false);
  error = input<string | null>(null);
  apply = output<string>();
  remove = output<void>();

  private readonly fb = inject(FormBuilder);
  readonly form = this.fb.nonNullable.group({
    codigo: ['', [Validators.required, Validators.pattern(/^[\w-]{1,30}$/)]],
  });

  onApply(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.apply.emit(this.form.controls.codigo.value.trim().toUpperCase());
  }
}
```

- [ ] **Step 3: Typecheck**
```bash
npx ng build --configuration development
```
Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**
```bash
cd /home/ali/projects/pet-hub && git add frontend/storefront/src/app/shared/components/cart-item-row frontend/storefront/src/app/shared/components/coupon-input
git commit -m "feat(storefront): add CartItemRow and CouponInput shared components"
```

---

## Task 9: CartPage + integrate "Adicionar ao carrinho" + cart badge + route

**Files:**
- Create: `src/app/features/cart/cart.page.ts`
- Modify: `src/app/app.routes.ts`
- Modify: `src/app/shared/layout/main-layout.component.ts`
- Modify: `src/app/features/catalog/product-detail.page.ts`

- [ ] **Step 1: Create CartPage**

Path: `src/app/features/cart/cart.page.ts`

```ts
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

import { AuthService } from '../../core/services/auth.service';
import { CartService } from '../../core/services/cart.service';
import { CartItemRowComponent } from '../../shared/components/cart-item-row/cart-item-row.component';
import { CouponInputComponent } from '../../shared/components/coupon-input/coupon-input.component';
import { ShippingCalculatorComponent } from '../../shared/components/shipping-calculator/shipping-calculator.component';

@Component({
  selector: 'app-cart-page',
  standalone: true,
  imports: [CommonModule, RouterLink, CartItemRowComponent, CouponInputComponent, ShippingCalculatorComponent],
  template: `
    <section class="mx-auto max-w-page px-6 py-12">
      <h1 class="text-3xl text-graphite-900">Carrinho</h1>

      <ng-container *ngIf="auth.isAuthenticated(); else needLogin">
        <ng-container *ngIf="cart.cart() as c">
          <div *ngIf="c.items.length; else empty"
               class="mt-8 grid gap-12 lg:grid-cols-[1fr_360px]">
            <div>
              <app-cart-item-row *ngFor="let item of c.items"
                                 [item]="item"
                                 [busy]="busyItem() === item.sku"
                                 (updateQty)="onUpdateQty($event.sku, $event.qty)"
                                 (remove)="onRemove($event)" />
              <p *ngIf="topError()" class="form-error mt-3">{{ topError() }}</p>
            </div>

            <aside class="space-y-6">
              <div class="surface space-y-4">
                <h2 class="text-lg text-graphite-900">Resumo</h2>
                <div class="flex justify-between text-sm">
                  <span class="text-graphite-500">Itens ({{ c.totalItens }})</span>
                  <span>{{ c.subtotal | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</span>
                </div>
                <p class="text-xs text-graphite-500">Frete calculado no checkout.</p>
                <button class="btn-primary w-full"
                        type="button"
                        [disabled]="busyItem() !== null"
                        (click)="prosseguirCheckout()">
                  Finalizar compra
                </button>
              </div>

              <div class="surface">
                <app-coupon-input [cupom]="c.cupom"
                                  [busy]="couponBusy()"
                                  [error]="couponError()"
                                  (apply)="onApplyCoupon($event)"
                                  (remove)="onRemoveCoupon()" />
              </div>

              <div class="surface">
                <p class="label mb-2">Estimativa de frete</p>
                <app-shipping-calculator [sku]="c.items[0].sku" [qty]="c.items[0].qty" />
              </div>
            </aside>
          </div>
        </ng-container>

        <ng-template #empty>
          <div class="mt-12 rounded-lg border border-graphite-200 bg-graphite-0 py-16 text-center">
            <p class="text-graphite-600">Seu carrinho está vazio.</p>
            <a routerLink="/produtos" class="btn-primary mt-4 inline-flex">Explorar produtos</a>
          </div>
        </ng-template>
      </ng-container>

      <ng-template #needLogin>
        <div class="mt-12 rounded-lg border border-graphite-200 bg-graphite-0 py-16 text-center">
          <p class="text-graphite-600">Faça login para ver seu carrinho.</p>
          <a routerLink="/login" class="btn-primary mt-4 inline-flex">Entrar</a>
        </div>
      </ng-template>
    </section>
  `,
})
export class CartPage implements OnInit {
  readonly auth = inject(AuthService);
  readonly cart = inject(CartService);
  private readonly router = inject(Router);

  readonly busyItem = signal<string | null>(null);
  readonly couponBusy = signal(false);
  readonly couponError = signal<string | null>(null);
  readonly topError = signal<string | null>(null);

  ngOnInit(): void {
    if (!this.auth.isAuthenticated()) return;
    this.cart.load().subscribe({ error: () => this.topError.set('Não foi possível carregar o carrinho.') });
  }

  onUpdateQty(sku: string, qty: number): void {
    this.busyItem.set(sku);
    this.topError.set(null);
    this.cart.updateQty(sku, qty).subscribe({
      next: () => this.busyItem.set(null),
      error: (e: HttpErrorResponse) => {
        this.busyItem.set(null);
        this.topError.set(e.error?.detail ?? 'Não foi possível atualizar a quantidade.');
      },
    });
  }

  onRemove(sku: string): void {
    this.busyItem.set(sku);
    this.cart.remove(sku).subscribe({
      next: () => this.busyItem.set(null),
      error: (e: HttpErrorResponse) => {
        this.busyItem.set(null);
        this.topError.set(e.error?.detail ?? 'Não foi possível remover o item.');
      },
    });
  }

  onApplyCoupon(codigo: string): void {
    this.couponBusy.set(true);
    this.couponError.set(null);
    this.cart.applyCoupon(codigo).subscribe({
      next: () => this.couponBusy.set(false),
      error: (e: HttpErrorResponse) => {
        this.couponBusy.set(false);
        this.couponError.set(e.error?.detail ?? 'Cupom inválido.');
      },
    });
  }

  onRemoveCoupon(): void {
    this.couponBusy.set(true);
    this.cart.removeCoupon().subscribe({
      next: () => this.couponBusy.set(false),
      error: () => this.couponBusy.set(false),
    });
  }

  prosseguirCheckout(): void {
    this.router.navigate(['/checkout/endereco']);
  }
}
```

- [ ] **Step 2: Add `/carrinho` route**

Modify `src/app/app.routes.ts` — insert the cart route between `produtos/:sku` and `minha-conta`:

```ts
{
  path: 'carrinho',
  loadComponent: () => import('./features/cart/cart.page').then((m) => m.CartPage),
},
```

The full file after this step:
```ts
import { Routes } from '@angular/router';

import { MainLayoutComponent } from './shared/layout/main-layout.component';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    component: MainLayoutComponent,
    children: [
      {
        path: '',
        loadComponent: () => import('./features/home/home.page').then((m) => m.HomePage),
      },
      {
        path: 'login',
        loadComponent: () => import('./features/auth/login.page').then((m) => m.LoginPage),
      },
      {
        path: 'cadastro',
        loadComponent: () => import('./features/auth/register.page').then((m) => m.RegisterPage),
      },
      {
        path: 'produtos',
        loadComponent: () =>
          import('./features/catalog/product-list.page').then((m) => m.ProductListPage),
      },
      {
        path: 'produtos/:sku',
        loadComponent: () =>
          import('./features/catalog/product-detail.page').then((m) => m.ProductDetailPage),
      },
      {
        path: 'carrinho',
        loadComponent: () => import('./features/cart/cart.page').then((m) => m.CartPage),
      },
      {
        path: 'minha-conta',
        canActivate: [authGuard],
        loadComponent: () => import('./features/home/home.page').then((m) => m.HomePage),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
```

- [ ] **Step 3: Add cart badge in MainLayoutComponent**

Modify `src/app/shared/layout/main-layout.component.ts`. Replace the file content with:

```ts
import { Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../core/services/auth.service';
import { CartService } from '../../core/services/cart.service';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="min-h-screen flex flex-col">
      <header class="border-b border-graphite-200 bg-graphite-0">
        <div class="mx-auto flex max-w-page items-center justify-between px-6 py-4">
          <a routerLink="/" class="text-xl font-display font-semibold text-graphite-900">
            Pet Hub
          </a>
          <nav class="flex items-center gap-6 text-sm">
            <a routerLink="/produtos" routerLinkActive="text-coral-500"
               class="text-graphite-700 hover:text-graphite-900">Catálogo</a>
            <a *ngIf="isLogged()" routerLink="/carrinho" routerLinkActive="text-coral-500"
               class="relative text-graphite-700 hover:text-graphite-900">
              Carrinho
              <span *ngIf="cartCount() > 0"
                    class="absolute -right-3 -top-2 inline-flex h-4 min-w-[1rem] items-center
                           justify-center rounded-full bg-coral-500 px-1 text-[10px] font-medium text-white">
                {{ cartCount() }}
              </span>
            </a>
            <ng-container *ngIf="isLogged(); else loggedOut">
              <a routerLink="/minha-conta" routerLinkActive="text-coral-500"
                 class="text-graphite-700 hover:text-graphite-900">
                {{ userName() }}
              </a>
              <button class="btn-ghost text-sm py-1.5 px-3" (click)="onLogout()">Sair</button>
            </ng-container>
            <ng-template #loggedOut>
              <a routerLink="/login" class="text-graphite-700 hover:text-graphite-900">Entrar</a>
              <a routerLink="/cadastro" class="btn-primary text-sm py-2 px-4">Criar conta</a>
            </ng-template>
          </nav>
        </div>
      </header>
      <main class="flex-1">
        <router-outlet />
      </main>
      <footer class="border-t border-graphite-200 bg-graphite-0">
        <div class="mx-auto max-w-page px-6 py-8 text-xs text-graphite-500">
          © 2026 Pet Hub · Pet tech ecommerce
        </div>
      </footer>
    </div>
  `,
})
export class MainLayoutComponent {
  private readonly auth = inject(AuthService);
  private readonly cart = inject(CartService);

  readonly isLogged = this.auth.isAuthenticated;
  readonly userName = computed(() => this.auth.currentUser()?.nome ?? '');
  readonly cartCount = this.cart.totalItens;
}
```

- [ ] **Step 4: Wire ProductDetailPage to real CartService**

Modify `src/app/features/catalog/product-detail.page.ts`:

a) Add CartService + Router imports + inject:
```ts
import { CartService } from '../../core/services/cart.service';
// (Router já existe)
```
In the class, add: `private readonly cartService = inject(CartService);`

b) Replace the existing `adicionarAoCarrinho()` method (lines ~179-187) with:
```ts
adicionarAoCarrinho(): void {
  const p = this.produto();
  if (!p || this.addingToCart()) return;
  if (!this.auth.isAuthenticated()) {
    this.router.navigate(['/login']);
    return;
  }
  this.addingToCart.set(true);
  this.addToCartHint.set(null);
  this.cartService.add(p.sku, this.qty()).subscribe({
    next: () => {
      this.addingToCart.set(false);
      this.addToCartHint.set('Adicionado ao carrinho.');
    },
    error: (e: HttpErrorResponse) => {
      this.addingToCart.set(false);
      this.addToCartHint.set(e.error?.detail ?? 'Não foi possível adicionar.');
    },
  });
}
```

c) Add `AuthService` to imports and inject in the class:
```ts
import { AuthService } from '../../core/services/auth.service';
// dentro da classe:
private readonly auth = inject(AuthService);
```

- [ ] **Step 5: Typecheck**
```bash
npx ng build --configuration development
```
Expected: BUILD SUCCESS.

- [ ] **Step 6: Smoke validation (manual)**

Storefront precisa estar rodando (`ng serve --host 127.0.0.1 --port 4242 --proxy-config proxy.conf.json`) com backend up.

Em http://127.0.0.1:4242:
1. Login `maria.fase2@pethub.com / Senha@123`.
2. Catálogo → COLLAR-PRO-001 → "Adicionar ao carrinho" — texto "Adicionado ao carrinho." aparece.
3. Header → badge no Carrinho mostra `1`.
4. Click no Carrinho → vê o item, foto, preço, subtotal correto.
5. `+` → qty=2, subtotal dobra, badge=2. `−` no qty=1 → item some, badge some.
6. Adicionar de novo → cupom `10OFF` → chip aparece, "Validade verificada no checkout".
7. `×` no chip → some.
8. Logout e abrir `/carrinho` → painel "Faça login".

Expected: tudo passa sem erro de console.

- [ ] **Step 7: Commit**
```bash
cd /home/ali/projects/pet-hub && git add frontend/storefront/src/app/features/cart frontend/storefront/src/app/app.routes.ts frontend/storefront/src/app/shared/layout/main-layout.component.ts frontend/storefront/src/app/features/catalog/product-detail.page.ts
git commit -m "feat(storefront): add /carrinho page, cart badge in header, wire PDP add-to-cart"
```

---

## Task 10: Wizard step 1 — `/checkout/endereco`

**Files:**
- Create: `src/app/features/checkout/address.page.ts`
- Modify: `src/app/app.routes.ts`

- [ ] **Step 1: Create CheckoutAddressPage**

Path: `src/app/features/checkout/address.page.ts`

```ts
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { AddressService } from '../../core/services/address.service';
import { CheckoutStateService } from '../../core/services/checkout-state.service';
import { EnderecoResponse, TipoEndereco, UFS, UnidadeFederativa } from '../../core/models/address';
import { StepperComponent } from '../../shared/components/stepper/stepper.component';

@Component({
  selector: 'app-checkout-address-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, StepperComponent],
  template: `
    <section class="mx-auto max-w-page px-6 py-12">
      <app-stepper [steps]="WIZARD_STEPS" [active]="1" />

      <h1 class="mt-8 text-2xl text-graphite-900">Endereço de entrega</h1>

      <div *ngIf="enderecos().length" class="mt-6 space-y-3">
        <label *ngFor="let e of enderecos()" class="block cursor-pointer">
          <input type="radio" name="endereco" class="peer sr-only"
                 [value]="e.id"
                 [checked]="enderecoEntregaId() === e.id"
                 (change)="selectEntrega(e.id)" />
          <div class="surface peer-checked:border-coral-500 peer-checked:ring-1 peer-checked:ring-coral-500">
            <div class="flex justify-between">
              <p class="font-medium text-graphite-900">{{ e.apelido }}
                <span *ngIf="e.padraoEntrega" class="ml-2 text-xs text-coral-500">padrão</span>
              </p>
              <p class="text-xs text-graphite-500">{{ e.tipo }}</p>
            </div>
            <p class="mt-1 text-sm text-graphite-700">
              {{ e.logradouro }}<ng-container *ngIf="e.numero">, {{ e.numero }}</ng-container>
              <ng-container *ngIf="e.complemento"> — {{ e.complemento }}</ng-container>
            </p>
            <p class="text-sm text-graphite-500">{{ e.bairro }}, {{ e.cidade }}/{{ e.uf }} · {{ formatCep(e.cep) }}</p>
          </div>
        </label>
      </div>

      <button *ngIf="enderecos().length && !showForm()"
              type="button"
              class="btn-ghost mt-4"
              (click)="showForm.set(true)">
        + Adicionar novo endereço
      </button>

      <form *ngIf="showForm() || enderecos().length === 0"
            [formGroup]="form" (ngSubmit)="onCriar()" class="mt-6 surface space-y-4">
        <h2 class="text-base text-graphite-900">Novo endereço</h2>
        <div class="grid grid-cols-1 gap-4 sm:grid-cols-3">
          <div class="sm:col-span-1">
            <label class="label" for="cep">CEP</label>
            <input id="cep" class="input" formControlName="cep" inputmode="numeric"
                   maxlength="9" placeholder="00000-000"
                   (blur)="onLookupCep()" />
            <p *ngIf="cepError()" class="form-error">{{ cepError() }}</p>
          </div>
          <div class="sm:col-span-2">
            <label class="label" for="apelido">Apelido</label>
            <input id="apelido" class="input" formControlName="apelido" placeholder="Casa, Trabalho..." />
          </div>
          <div class="sm:col-span-2">
            <label class="label" for="logradouro">Logradouro</label>
            <input id="logradouro" class="input" formControlName="logradouro" />
          </div>
          <div class="sm:col-span-1">
            <label class="label" for="numero">Número</label>
            <input id="numero" class="input" formControlName="numero" />
          </div>
          <div class="sm:col-span-3">
            <label class="label" for="complemento">Complemento</label>
            <input id="complemento" class="input" formControlName="complemento" placeholder="Apto 12, Bloco B..." />
          </div>
          <div class="sm:col-span-1">
            <label class="label" for="bairro">Bairro</label>
            <input id="bairro" class="input" formControlName="bairro" />
          </div>
          <div class="sm:col-span-1">
            <label class="label" for="cidade">Cidade</label>
            <input id="cidade" class="input" formControlName="cidade" />
          </div>
          <div class="sm:col-span-1">
            <label class="label" for="uf">UF</label>
            <select id="uf" class="input" formControlName="uf">
              <option *ngFor="let uf of UFS" [value]="uf">{{ uf }}</option>
            </select>
          </div>
          <div class="sm:col-span-1">
            <label class="label" for="tipo">Tipo</label>
            <select id="tipo" class="input" formControlName="tipo">
              <option value="RESIDENCIAL">Residencial</option>
              <option value="COMERCIAL">Comercial</option>
            </select>
          </div>
        </div>
        <p *ngIf="formError()" class="form-error">{{ formError() }}</p>
        <div class="flex gap-2">
          <button type="submit" class="btn-primary" [disabled]="form.invalid || saving()">
            <span *ngIf="!saving()">Salvar endereço</span>
            <span *ngIf="saving()">Salvando…</span>
          </button>
          <button *ngIf="enderecos().length"
                  type="button"
                  class="btn-ghost"
                  (click)="showForm.set(false)">Cancelar</button>
        </div>
      </form>

      <div class="mt-8 surface">
        <label class="flex items-center gap-2">
          <input type="checkbox" [checked]="mesmoCobranca()" (change)="toggleMesmoCobranca()" />
          <span class="text-sm text-graphite-700">Usar mesmo endereço para cobrança</span>
        </label>
        <div *ngIf="!mesmoCobranca()" class="mt-3 space-y-2">
          <p class="label">Endereço de cobrança</p>
          <label *ngFor="let e of enderecos()" class="block cursor-pointer">
            <input type="radio" name="cobranca" class="peer sr-only"
                   [checked]="enderecoCobrancaId() === e.id"
                   (change)="selectCobranca(e.id)" />
            <div class="rounded border border-graphite-200 px-3 py-2 peer-checked:border-coral-500">
              <p class="text-sm">{{ e.apelido }} — {{ e.cidade }}/{{ e.uf }}</p>
            </div>
          </label>
        </div>
      </div>

      <div class="mt-8 flex justify-end">
        <button class="btn-primary"
                type="button"
                [disabled]="!enderecoEntregaId() || !enderecoCobrancaId()"
                (click)="onProsseguir()">
          Continuar para frete
        </button>
      </div>
    </section>
  `,
})
export class CheckoutAddressPage implements OnInit {
  private readonly addressService = inject(AddressService);
  private readonly state = inject(CheckoutStateService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  readonly UFS = UFS;
  readonly WIZARD_STEPS = [
    { label: 'Endereço' }, { label: 'Frete' }, { label: 'Pagamento' }, { label: 'Revisão' },
  ];

  readonly enderecos = signal<EnderecoResponse[]>([]);
  readonly enderecoEntregaId = signal<number | null>(null);
  readonly enderecoCobrancaId = signal<number | null>(null);
  readonly mesmoCobranca = signal(true);
  readonly showForm = signal(false);
  readonly saving = signal(false);
  readonly cepError = signal<string | null>(null);
  readonly formError = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    apelido: ['', [Validators.required, Validators.maxLength(50)]],
    cep: ['', [Validators.required, Validators.pattern(/^\d{5}-?\d{3}$/)]],
    logradouro: ['', [Validators.required, Validators.maxLength(200)]],
    numero: [''],
    complemento: [''],
    bairro: ['', [Validators.required, Validators.maxLength(100)]],
    cidade: ['', [Validators.required, Validators.maxLength(100)]],
    uf: this.fb.nonNullable.control<UnidadeFederativa>('SP', Validators.required),
    tipo: this.fb.nonNullable.control<TipoEndereco>('RESIDENCIAL', Validators.required),
  });

  ngOnInit(): void {
    const initial = this.state.state();
    if (initial.enderecoEntregaId) this.enderecoEntregaId.set(initial.enderecoEntregaId);
    if (initial.enderecoCobrancaId) this.enderecoCobrancaId.set(initial.enderecoCobrancaId);
    if (initial.enderecoEntregaId && initial.enderecoCobrancaId
        && initial.enderecoEntregaId !== initial.enderecoCobrancaId) {
      this.mesmoCobranca.set(false);
    }

    this.addressService.list().subscribe({
      next: (list) => {
        this.enderecos.set(list);
        if (!this.enderecoEntregaId()) {
          const def = list.find((e) => e.padraoEntrega) ?? list[0];
          if (def) {
            this.enderecoEntregaId.set(def.id);
            this.enderecoCobrancaId.set(def.id);
          } else {
            this.showForm.set(true);
          }
        }
      },
      error: () => this.showForm.set(true),
    });
  }

  selectEntrega(id: number): void {
    this.enderecoEntregaId.set(id);
    if (this.mesmoCobranca()) this.enderecoCobrancaId.set(id);
  }

  selectCobranca(id: number): void {
    this.enderecoCobrancaId.set(id);
  }

  toggleMesmoCobranca(): void {
    const next = !this.mesmoCobranca();
    this.mesmoCobranca.set(next);
    if (next) this.enderecoCobrancaId.set(this.enderecoEntregaId());
  }

  onLookupCep(): void {
    const cep = this.form.controls.cep.value.replace(/\D/g, '');
    if (cep.length !== 8) return;
    this.cepError.set(null);
    this.addressService.lookupCep(cep).subscribe({
      next: (r) => {
        if (r.erro) {
          this.cepError.set('CEP não encontrado. Preencha manualmente.');
          return;
        }
        this.form.patchValue({
          logradouro: r.logradouro || this.form.controls.logradouro.value,
          bairro: r.bairro || this.form.controls.bairro.value,
          cidade: r.cidade || this.form.controls.cidade.value,
          uf: (r.uf as UnidadeFederativa) || this.form.controls.uf.value,
        });
      },
      error: () => this.cepError.set('CEP não encontrado. Preencha manualmente.'),
    });
  }

  onCriar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.formError.set(null);
    const v = this.form.getRawValue();
    this.addressService.create({
      ...v,
      cep: v.cep.replace(/\D/g, ''),
      padraoEntrega: this.enderecos().length === 0,
      padraoCobranca: this.enderecos().length === 0,
    }).subscribe({
      next: (e) => {
        this.saving.set(false);
        this.enderecos.update((list) => [...list, e]);
        this.enderecoEntregaId.set(e.id);
        if (this.mesmoCobranca()) this.enderecoCobrancaId.set(e.id);
        this.showForm.set(false);
        this.form.reset({ uf: 'SP', tipo: 'RESIDENCIAL', apelido: '', cep: '', logradouro: '', numero: '', complemento: '', bairro: '', cidade: '' });
      },
      error: (err: HttpErrorResponse) => {
        this.saving.set(false);
        this.formError.set(err.error?.detail ?? 'Não foi possível salvar o endereço.');
      },
    });
  }

  onProsseguir(): void {
    this.state.patch({
      enderecoEntregaId: this.enderecoEntregaId(),
      enderecoCobrancaId: this.enderecoCobrancaId(),
    });
    this.router.navigate(['/checkout/frete']);
  }

  formatCep(cep: string): string {
    const c = cep.replace(/\D/g, '');
    return c.length === 8 ? `${c.slice(0, 5)}-${c.slice(5)}` : cep;
  }
}
```

- [ ] **Step 2: Register route**

Modify `src/app/app.routes.ts` — add the checkout-address route after `carrinho` and before `minha-conta`. Import the guard:

```ts
import { checkoutStepGuard } from './core/guards/checkout-step.guard';
```

Add route:
```ts
{
  path: 'checkout/endereco',
  canActivate: [authGuard, checkoutStepGuard(['cart-not-empty'])],
  loadComponent: () => import('./features/checkout/address.page').then((m) => m.CheckoutAddressPage),
},
```

- [ ] **Step 3: Typecheck**
```bash
npx ng build --configuration development
```
Expected: BUILD SUCCESS.

- [ ] **Step 4: Smoke validation**

1. Cart com 1 item → "Finalizar compra" → `/checkout/endereco`.
2. Maria já tem endereço cadastrado da Fase 2? Se sim → aparece na lista e é auto-selecionado. Se não → form aberto.
3. CEP `01310-100` no form → blur → auto-fill (Av. Paulista, Bela Vista, São Paulo, SP).
4. Salvar → endereço aparece na lista, selecionado, badge "padrão" se primeiro.
5. Acessar `/checkout/frete` direto na URL com cart vazio → redirect para `/carrinho`.
6. CTA "Continuar para frete" só habilita após selecionar entrega + cobrança.

- [ ] **Step 5: Commit**
```bash
cd /home/ali/projects/pet-hub && git add frontend/storefront/src/app/features/checkout/address.page.ts frontend/storefront/src/app/app.routes.ts
git commit -m "feat(storefront): checkout step 1 — address selection with inline create + ViaCEP autofill"
```

---

## Task 11: Wizard step 2 — `/checkout/frete`

**Files:**
- Create: `src/app/features/checkout/shipping.page.ts`
- Modify: `src/app/app.routes.ts`

- [ ] **Step 1: Create CheckoutShippingPage**

Path: `src/app/features/checkout/shipping.page.ts`

```ts
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

import { AddressService } from '../../core/services/address.service';
import { CartService } from '../../core/services/cart.service';
import { CheckoutStateService } from '../../core/services/checkout-state.service';
import { ShippingService } from '../../core/services/shipping.service';
import { EnderecoResponse } from '../../core/models/address';
import { OpcaoFrete } from '../../core/models/shipping';
import { StepperComponent } from '../../shared/components/stepper/stepper.component';

@Component({
  selector: 'app-checkout-shipping-page',
  standalone: true,
  imports: [CommonModule, StepperComponent],
  template: `
    <section class="mx-auto max-w-page px-6 py-12">
      <app-stepper [steps]="WIZARD_STEPS" [active]="2" />

      <h1 class="mt-8 text-2xl text-graphite-900">Frete</h1>
      <p *ngIf="endereco() as e" class="mt-1 text-sm text-graphite-500">
        Entregando em {{ e.cidade }}/{{ e.uf }} · {{ formatCep(e.cep) }}
      </p>

      <div *ngIf="loading()" class="mt-8 space-y-3">
        <div class="h-16 animate-pulse rounded bg-graphite-100"></div>
        <div class="h-16 animate-pulse rounded bg-graphite-100"></div>
        <div class="h-16 animate-pulse rounded bg-graphite-100"></div>
      </div>

      <p *ngIf="error()" class="form-error mt-6">{{ error() }}</p>

      <div *ngIf="!loading() && !error()" class="mt-6 space-y-3">
        <label *ngFor="let opt of opcoes()" class="block cursor-pointer">
          <input type="radio" name="frete" class="peer sr-only"
                 [value]="opt.codigo"
                 [checked]="opcaoCodigo() === opt.codigo"
                 (change)="select(opt)" />
          <div class="surface peer-checked:border-coral-500 peer-checked:ring-1 peer-checked:ring-coral-500">
            <div class="flex justify-between">
              <div>
                <p class="font-medium text-graphite-900">{{ opt.servico }}</p>
                <p class="text-xs text-graphite-500">
                  {{ opt.transportadora }} · entrega em até {{ opt.prazoDias }} dia(s) úteis
                </p>
              </div>
              <p class="font-semibold text-graphite-900">
                {{ opt.valor | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}
              </p>
            </div>
          </div>
        </label>
      </div>

      <div class="mt-8 flex justify-between">
        <button class="btn-ghost" type="button" (click)="voltar()">← Voltar para endereço</button>
        <button class="btn-primary"
                type="button"
                [disabled]="!opcaoCodigo()"
                (click)="onProsseguir()">
          Continuar para pagamento
        </button>
      </div>
    </section>
  `,
})
export class CheckoutShippingPage implements OnInit {
  private readonly addressService = inject(AddressService);
  private readonly cart = inject(CartService);
  private readonly shipping = inject(ShippingService);
  private readonly state = inject(CheckoutStateService);
  private readonly router = inject(Router);

  readonly WIZARD_STEPS = [
    { label: 'Endereço' }, { label: 'Frete' }, { label: 'Pagamento' }, { label: 'Revisão' },
  ];

  readonly endereco = signal<EnderecoResponse | null>(null);
  readonly opcoes = signal<OpcaoFrete[]>([]);
  readonly opcaoCodigo = signal<string | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    const s = this.state.state();
    if (s.opcaoFreteCodigo) this.opcaoCodigo.set(s.opcaoFreteCodigo);

    this.addressService.list().subscribe({
      next: (list) => {
        const e = list.find((x) => x.id === s.enderecoEntregaId);
        if (!e) {
          this.router.navigate(['/checkout/endereco']);
          return;
        }
        this.endereco.set(e);
        this.fetchOpcoes(e);
      },
      error: () => this.router.navigate(['/checkout/endereco']),
    });
  }

  select(opt: OpcaoFrete): void {
    this.opcaoCodigo.set(opt.codigo);
  }

  onProsseguir(): void {
    const opt = this.opcoes().find((o) => o.codigo === this.opcaoCodigo());
    if (!opt) return;
    this.state.patch({
      opcaoFreteCodigo: opt.codigo,
      opcaoFreteSnapshot: opt,
    });
    this.router.navigate(['/checkout/pagamento']);
  }

  voltar(): void {
    this.router.navigate(['/checkout/endereco']);
  }

  formatCep(cep: string): string {
    const c = cep.replace(/\D/g, '');
    return c.length === 8 ? `${c.slice(0, 5)}-${c.slice(5)}` : cep;
  }

  private fetchOpcoes(e: EnderecoResponse): void {
    const itens = (this.cart.cart()?.items ?? []).map((i) => ({ sku: i.sku, qty: i.qty }));
    if (!itens.length) {
      this.router.navigate(['/carrinho']);
      return;
    }
    this.shipping.calculate({ cepDestino: e.cep, itens }).subscribe({
      next: (list) => {
        this.loading.set(false);
        this.opcoes.set(list);
        if (!this.opcaoCodigo() && list.length) {
          // auto-selecionar o mais barato
          const cheapest = list.reduce((a, b) => (a.valor <= b.valor ? a : b));
          this.opcaoCodigo.set(cheapest.codigo);
        }
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        this.error.set(err.error?.detail ?? 'Não foi possível calcular o frete.');
      },
    });
  }
}
```

- [ ] **Step 2: Register route**

Modify `src/app/app.routes.ts` — add after the `endereco` route:

```ts
{
  path: 'checkout/frete',
  canActivate: [authGuard, checkoutStepGuard(['enderecoEntregaId', 'enderecoCobrancaId'])],
  loadComponent: () => import('./features/checkout/shipping.page').then((m) => m.CheckoutShippingPage),
},
```

- [ ] **Step 3: Typecheck**
```bash
npx ng build --configuration development
```
Expected: BUILD SUCCESS.

- [ ] **Step 4: Smoke validation**

1. Cart com item → `/checkout/endereco` → escolher endereço SP → "Continuar" → `/checkout/frete`.
2. 3 cards aparecem (SEDEX, TABELADO, MOTOBOY); mais barato auto-selecionado.
3. Trocar endereço para um fora de SP (manualmente seed via Postman, ou usar CEP `90000-000`) → motoboy não aparece.
4. Acesso direto `/checkout/frete` sem passar pelo passo 1 → redirect `/checkout/endereco`.
5. CTA "Continuar para pagamento" habilita.

- [ ] **Step 5: Commit**
```bash
cd /home/ali/projects/pet-hub && git add frontend/storefront/src/app/features/checkout/shipping.page.ts frontend/storefront/src/app/app.routes.ts
git commit -m "feat(storefront): checkout step 2 — shipping options with auto-cheapest selection"
```

---

## Task 12: Wizard step 3 — `/checkout/pagamento`

**Files:**
- Create: `src/app/features/checkout/payment.page.ts`
- Modify: `src/app/app.routes.ts`

- [ ] **Step 1: Create CheckoutPaymentPage**

Path: `src/app/features/checkout/payment.page.ts`

```ts
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { CheckoutStateService } from '../../core/services/checkout-state.service';
import { PaymentMethodService } from '../../core/services/payment-method.service';
import {
  Bandeira,
  CreateFormaPagamentoRequest,
  FormaPagamentoResponse,
  TipoPagamento,
} from '../../core/models/payment-method';
import { StepperComponent } from '../../shared/components/stepper/stepper.component';

type Tab = 'CARTAO' | 'PIX' | 'BOLETO';

@Component({
  selector: 'app-checkout-payment-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, StepperComponent],
  template: `
    <section class="mx-auto max-w-page px-6 py-12">
      <app-stepper [steps]="WIZARD_STEPS" [active]="3" />
      <h1 class="mt-8 text-2xl text-graphite-900">Pagamento</h1>

      <div class="mt-6 flex gap-2 border-b border-graphite-200">
        <button *ngFor="let t of TABS"
                type="button"
                class="px-4 py-2 text-sm"
                [class.text-coral-500]="tab() === t.value"
                [class.font-medium]="tab() === t.value"
                [class.border-b-2]="tab() === t.value"
                [class.border-coral-500]="tab() === t.value"
                [class.text-graphite-500]="tab() !== t.value"
                (click)="switchTab(t.value)">
          {{ t.label }}
        </button>
      </div>

      <!-- Tab CARTÃO -->
      <div *ngIf="tab() === 'CARTAO'" class="mt-6 space-y-6">
        <div *ngIf="cartoes().length" class="space-y-3">
          <label *ngFor="let c of cartoes()" class="block cursor-pointer">
            <input type="radio" name="cartao" class="peer sr-only"
                   [value]="c.id"
                   [checked]="cartaoSelecionadoId() === c.id"
                   (change)="selectCartao(c.id)" />
            <div class="surface peer-checked:border-coral-500 peer-checked:ring-1 peer-checked:ring-coral-500">
              <div class="flex justify-between">
                <p class="font-medium text-graphite-900">
                  {{ c.bandeira }} ···· {{ c.ultimosQuatroDigitos }}
                  <span *ngIf="c.padrao" class="ml-2 text-xs text-coral-500">padrão</span>
                </p>
                <p class="text-xs text-graphite-500">
                  Válido até {{ c.validadeMes }}/{{ c.validadeAno }}
                </p>
              </div>
              <p class="mt-1 text-sm text-graphite-700">{{ c.nomeImpresso }}</p>
            </div>
          </label>
        </div>

        <div *ngIf="cartaoSelecionadoId()" class="surface space-y-3">
          <label class="label" for="parcelas">Parcelas</label>
          <select id="parcelas" class="input" [value]="parcelas()" (change)="setParcelas($any($event.target).value)">
            <option *ngFor="let p of [1,2,3,4,5,6,7,8,9,10,11,12]" [value]="p">{{ p }}x sem juros</option>
          </select>
        </div>

        <button *ngIf="cartoes().length && !showCardForm()"
                type="button"
                class="btn-ghost"
                (click)="showCardForm.set(true)">
          + Adicionar novo cartão
        </button>

        <form *ngIf="showCardForm() || cartoes().length === 0"
              [formGroup]="cardForm" (ngSubmit)="onTokenizar()"
              class="surface space-y-4" autocomplete="on">
          <h2 class="text-base text-graphite-900">Novo cartão</h2>
          <div class="grid grid-cols-1 gap-4 sm:grid-cols-3">
            <div class="sm:col-span-3">
              <label class="label" for="numero">Número</label>
              <input id="numero" class="input"
                     formControlName="numero"
                     inputmode="numeric"
                     autocomplete="cc-number"
                     maxlength="19"
                     placeholder="9999 9999 9999 9999" />
              <p *ngIf="cardForm.controls.numero.touched && cardForm.controls.numero.invalid"
                 class="form-error">Número deve ter 13 a 19 dígitos.</p>
            </div>
            <div class="sm:col-span-3">
              <label class="label" for="nomeImpresso">Nome impresso</label>
              <input id="nomeImpresso" class="input"
                     formControlName="nomeImpresso"
                     autocomplete="cc-name" />
            </div>
            <div>
              <label class="label" for="validadeMes">Mês (MM)</label>
              <input id="validadeMes" class="input"
                     formControlName="validadeMes"
                     inputmode="numeric" maxlength="2"
                     autocomplete="cc-exp-month" />
            </div>
            <div>
              <label class="label" for="validadeAno">Ano (AAAA)</label>
              <input id="validadeAno" class="input"
                     formControlName="validadeAno"
                     inputmode="numeric" maxlength="4"
                     autocomplete="cc-exp-year" />
            </div>
            <div>
              <label class="label" for="cvv">CVV</label>
              <input id="cvv" class="input"
                     type="password"
                     formControlName="cvv"
                     inputmode="numeric" maxlength="4"
                     autocomplete="cc-csc" />
            </div>
          </div>
          <p *ngIf="cardError()" class="form-error">{{ cardError() }}</p>
          <div class="flex gap-2">
            <button type="submit" class="btn-primary" [disabled]="cardForm.invalid || tokenizing()">
              <span *ngIf="!tokenizing()">Salvar cartão</span>
              <span *ngIf="tokenizing()">Validando…</span>
            </button>
            <button *ngIf="cartoes().length"
                    type="button"
                    class="btn-ghost"
                    (click)="showCardForm.set(false)">Cancelar</button>
          </div>
        </form>
      </div>

      <!-- Tab PIX -->
      <div *ngIf="tab() === 'PIX'" class="mt-6 surface">
        <p class="text-graphite-700">Você verá o QR Code após confirmar o pedido.</p>
        <p class="mt-1 text-sm text-graphite-500">Aprovação imediata; o pedido entra em separação ao receber a confirmação.</p>
      </div>

      <!-- Tab BOLETO -->
      <div *ngIf="tab() === 'BOLETO'" class="mt-6 surface">
        <p class="text-graphite-700">O boleto será gerado e enviado por e-mail após confirmar o pedido.</p>
        <p class="mt-1 text-sm text-graphite-500">Vencimento em 3 dias úteis.</p>
      </div>

      <div class="mt-8 flex justify-between">
        <button class="btn-ghost" type="button" (click)="voltar()">← Voltar para frete</button>
        <button class="btn-primary"
                type="button"
                [disabled]="!podeProsseguir()"
                (click)="onProsseguir()">
          Continuar para revisão
        </button>
      </div>
    </section>
  `,
})
export class CheckoutPaymentPage implements OnInit {
  private readonly paymentMethod = inject(PaymentMethodService);
  private readonly state = inject(CheckoutStateService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  readonly WIZARD_STEPS = [
    { label: 'Endereço' }, { label: 'Frete' }, { label: 'Pagamento' }, { label: 'Revisão' },
  ];
  readonly TABS: { value: Tab; label: string }[] = [
    { value: 'CARTAO', label: 'Cartão' },
    { value: 'PIX', label: 'PIX' },
    { value: 'BOLETO', label: 'Boleto' },
  ];

  readonly tab = signal<Tab>('CARTAO');
  readonly metodos = signal<FormaPagamentoResponse[]>([]);
  readonly cartoes = computed(() => this.metodos().filter((m) => m.tipo === 'CARTAO_CREDITO'));
  readonly cartaoSelecionadoId = signal<number | null>(null);
  readonly parcelas = signal(1);
  readonly showCardForm = signal(false);
  readonly tokenizing = signal(false);
  readonly cardError = signal<string | null>(null);

  readonly cardForm = this.fb.nonNullable.group({
    numero: ['', [Validators.required, Validators.pattern(/^\d{13,19}$/)]],
    nomeImpresso: ['', [Validators.required, Validators.maxLength(100)]],
    validadeMes: ['', [Validators.required, Validators.pattern(/^(0?[1-9]|1[0-2])$/)]],
    validadeAno: ['', [Validators.required, Validators.pattern(/^20\d{2}$/)]],
    cvv: ['', [Validators.required, Validators.pattern(/^\d{3,4}$/)]],
  });

  readonly podeProsseguir = computed(() => {
    if (this.tab() === 'CARTAO') return this.cartaoSelecionadoId() !== null;
    return true;
  });

  ngOnInit(): void {
    const s = this.state.state();
    this.parcelas.set(s.parcelas || 1);
    this.refreshMethods(s.formaPagamentoId);
  }

  switchTab(t: Tab): void {
    this.tab.set(t);
  }

  selectCartao(id: number): void {
    this.cartaoSelecionadoId.set(id);
  }

  setParcelas(raw: string): void {
    const n = Number(raw);
    if (Number.isInteger(n) && n >= 1 && n <= 12) this.parcelas.set(n);
  }

  onTokenizar(): void {
    if (this.cardForm.invalid) {
      this.cardForm.markAllAsTouched();
      return;
    }
    const v = this.cardForm.getRawValue();
    this.tokenizing.set(true);
    this.cardError.set(null);
    this.paymentMethod.tokenize({
      numero: v.numero.replace(/\D/g, ''),
      cvv: v.cvv,
      nomeImpresso: v.nomeImpresso,
      validadeMes: Number(v.validadeMes),
      validadeAno: Number(v.validadeAno),
    }).subscribe({
      next: (tok) => {
        // Limpa imediatamente os campos sensíveis após tokenizar
        this.cardForm.patchValue({ numero: '', cvv: '' });
        const req: CreateFormaPagamentoRequest = {
          tipo: 'CARTAO_CREDITO',
          gatewayToken: tok.token,
          bandeira: tok.bandeira,
          ultimosQuatroDigitos: tok.ultimosQuatroDigitos,
          nomeImpresso: v.nomeImpresso,
          validadeMes: Number(v.validadeMes),
          validadeAno: Number(v.validadeAno),
          padrao: this.cartoes().length === 0,
        };
        this.paymentMethod.create(req).subscribe({
          next: (m) => {
            this.tokenizing.set(false);
            this.metodos.update((list) => [...list, m]);
            this.cartaoSelecionadoId.set(m.id);
            this.showCardForm.set(false);
            this.cardForm.reset();
          },
          error: (err: HttpErrorResponse) => {
            this.tokenizing.set(false);
            this.cardError.set(err.error?.detail ?? 'Não foi possível salvar o cartão.');
          },
        });
      },
      error: (err: HttpErrorResponse) => {
        this.tokenizing.set(false);
        this.cardError.set(err.error?.detail ?? 'Número do cartão inválido.');
      },
    });
  }

  onProsseguir(): void {
    if (this.tab() === 'CARTAO') {
      this.state.patch({
        formaPagamentoId: this.cartaoSelecionadoId(),
        parcelas: this.parcelas(),
      });
      this.router.navigate(['/checkout/revisao']);
      return;
    }
    // PIX ou BOLETO: cria/encontra o método on-the-fly e segue
    const kind: TipoPagamento = this.tab() === 'PIX' ? 'PIX' : 'BOLETO';
    const existente = this.metodos().find((m) => m.tipo === kind);
    if (existente) {
      this.state.patch({ formaPagamentoId: existente.id, parcelas: 1 });
      this.router.navigate(['/checkout/revisao']);
      return;
    }
    this.paymentMethod.create({ tipo: kind }).subscribe({
      next: (m) => {
        this.metodos.update((list) => [...list, m]);
        this.state.patch({ formaPagamentoId: m.id, parcelas: 1 });
        this.router.navigate(['/checkout/revisao']);
      },
      error: (err: HttpErrorResponse) => {
        this.cardError.set(err.error?.detail ?? 'Não foi possível registrar o método.');
      },
    });
  }

  voltar(): void {
    this.router.navigate(['/checkout/frete']);
  }

  private refreshMethods(preselectId: number | null): void {
    this.paymentMethod.list().subscribe({
      next: (list) => {
        this.metodos.set(list);
        if (preselectId) {
          const m = list.find((x) => x.id === preselectId);
          if (m) {
            if (m.tipo === 'CARTAO_CREDITO') {
              this.tab.set('CARTAO');
              this.cartaoSelecionadoId.set(m.id);
            } else if (m.tipo === 'PIX') {
              this.tab.set('PIX');
            } else if (m.tipo === 'BOLETO') {
              this.tab.set('BOLETO');
            }
            return;
          }
        }
        const cartao = list.find((m) => m.tipo === 'CARTAO_CREDITO');
        if (cartao) this.cartaoSelecionadoId.set(cartao.id);
        else this.showCardForm.set(true);
      },
      error: () => this.showCardForm.set(true),
    });
  }
}
```

- [ ] **Step 2: Register route**

Modify `src/app/app.routes.ts` — add after the `frete` route:

```ts
{
  path: 'checkout/pagamento',
  canActivate: [authGuard, checkoutStepGuard(['opcaoFreteCodigo'])],
  loadComponent: () => import('./features/checkout/payment.page').then((m) => m.CheckoutPaymentPage),
},
```

- [ ] **Step 3: Typecheck**
```bash
npx ng build --configuration development
```
Expected: BUILD SUCCESS.

- [ ] **Step 4: Smoke validation (AppSec-critical step!)**

Backend rodando, cart com item, passo 1 + 2 concluídos.

1. `/checkout/pagamento` → tab Cartão default, sem cartão → form aberto.
2. Cartão `4111 1111 1111 1111`, nome `Maria Teste`, validade `12/2030`, CVV `123` → "Salvar cartão" → método aparece selecionado.
3. **DevTools Network**: ver request `POST /api/v1/customers/payment-methods/tokenize` — body contém o PAN. Conferir nenhum outro request subsequente (`POST /me/payment-methods` ou `place-order` mais tarde) carrega o PAN.
4. **DevTools Application → SessionStorage**: `pethub:checkout:v1` existe, **não contém** PAN ou CVV.
5. Cartão `4111 1111 1111 1112` (Luhn falha) → tokenize 422, mensagem inline.
6. Tab PIX → card informativo aparece; CTA prosseguir habilita.
7. Tab Boleto → idem.

- [ ] **Step 5: Commit**
```bash
cd /home/ali/projects/pet-hub && git add frontend/storefront/src/app/features/checkout/payment.page.ts frontend/storefront/src/app/app.routes.ts
git commit -m "feat(storefront): checkout step 3 — payment method tabs + inline card tokenization"
```

---

## Task 13: Wizard step 4 — `/checkout/revisao` (preview + place-order + idempotency)

**Files:**
- Create: `src/app/features/checkout/review.page.ts`
- Modify: `src/app/app.routes.ts`

- [ ] **Step 1: Create CheckoutReviewPage**

Path: `src/app/features/checkout/review.page.ts`

```ts
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

import { AddressService } from '../../core/services/address.service';
import { CartService } from '../../core/services/cart.service';
import { CheckoutService } from '../../core/services/checkout.service';
import { CheckoutStateService } from '../../core/services/checkout-state.service';
import { PaymentMethodService } from '../../core/services/payment-method.service';
import { CheckoutPreviewResponse } from '../../core/models/checkout';
import { EnderecoResponse } from '../../core/models/address';
import { FormaPagamentoResponse } from '../../core/models/payment-method';
import { StepperComponent } from '../../shared/components/stepper/stepper.component';

@Component({
  selector: 'app-checkout-review-page',
  standalone: true,
  imports: [CommonModule, RouterLink, StepperComponent],
  template: `
    <section class="mx-auto max-w-page px-6 py-12">
      <app-stepper [steps]="WIZARD_STEPS" [active]="4" />
      <h1 class="mt-8 text-2xl text-graphite-900">Revisão do pedido</h1>

      <div *ngIf="loading()" class="mt-8 space-y-3">
        <div class="h-24 animate-pulse rounded bg-graphite-100"></div>
        <div class="h-24 animate-pulse rounded bg-graphite-100"></div>
        <div class="h-24 animate-pulse rounded bg-graphite-100"></div>
      </div>

      <p *ngIf="loadError()" class="form-error mt-6">{{ loadError() }}</p>

      <ng-container *ngIf="preview() as p">
        <div class="mt-8 grid gap-8 lg:grid-cols-[1fr_360px]">
          <div class="space-y-6">
            <div class="surface">
              <div class="flex justify-between">
                <h2 class="text-base text-graphite-900">Endereço de entrega</h2>
                <a routerLink="/checkout/endereco" class="text-sm text-coral-500">Editar</a>
              </div>
              <p *ngIf="endereco() as e" class="mt-2 text-sm text-graphite-700">
                {{ e.apelido }} — {{ e.logradouro }}<ng-container *ngIf="e.numero">, {{ e.numero }}</ng-container><br>
                {{ e.bairro }}, {{ e.cidade }}/{{ e.uf }} · {{ formatCep(e.cep) }}
              </p>
            </div>
            <div class="surface">
              <div class="flex justify-between">
                <h2 class="text-base text-graphite-900">Frete</h2>
                <a routerLink="/checkout/frete" class="text-sm text-coral-500">Editar</a>
              </div>
              <p class="mt-2 text-sm text-graphite-700">
                {{ p.freteEscolhido.servico }} · {{ p.freteEscolhido.transportadora }} ·
                entrega em até {{ p.freteEscolhido.prazoDias }} dia(s) úteis
              </p>
            </div>
            <div class="surface">
              <div class="flex justify-between">
                <h2 class="text-base text-graphite-900">Pagamento</h2>
                <a routerLink="/checkout/pagamento" class="text-sm text-coral-500">Editar</a>
              </div>
              <p *ngIf="pagamento() as m" class="mt-2 text-sm text-graphite-700">
                <ng-container *ngIf="m.tipo === 'CARTAO_CREDITO'">
                  {{ m.bandeira }} ···· {{ m.ultimosQuatroDigitos }} em {{ state.state().parcelas }}x
                </ng-container>
                <ng-container *ngIf="m.tipo === 'PIX'">PIX (QR Code após confirmação)</ng-container>
                <ng-container *ngIf="m.tipo === 'BOLETO'">Boleto (3 dias úteis)</ng-container>
              </p>
            </div>
            <div class="surface">
              <h2 class="text-base text-graphite-900">Itens ({{ p.itens.length }})</h2>
              <ul class="mt-2 divide-y divide-graphite-200">
                <li *ngFor="let it of p.itens" class="flex justify-between py-2 text-sm">
                  <span class="text-graphite-700">{{ it.qty }}× {{ it.nome }}</span>
                  <span>{{ it.subtotal | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</span>
                </li>
              </ul>
            </div>
          </div>

          <aside class="surface space-y-3">
            <h2 class="text-lg text-graphite-900">Total</h2>
            <div class="space-y-1.5 text-sm">
              <div class="flex justify-between">
                <span class="text-graphite-500">Subtotal</span>
                <span>{{ p.subtotal | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</span>
              </div>
              <div *ngIf="p.descontoPromocoes > 0" class="flex justify-between text-leaf-700">
                <span>Promoções</span>
                <span>− {{ p.descontoPromocoes | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</span>
              </div>
              <div *ngIf="p.descontoCupom > 0" class="flex justify-between text-leaf-700">
                <span>Cupom {{ p.cupomCodigo }}</span>
                <span>− {{ p.descontoCupom | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</span>
              </div>
              <div *ngIf="p.impostosCalculados.valorImpostos > 0" class="flex justify-between">
                <span class="text-graphite-500">Impostos</span>
                <span>{{ p.impostosCalculados.valorImpostos | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</span>
              </div>
              <div class="flex justify-between">
                <span class="text-graphite-500">Frete</span>
                <span>{{ p.freteEscolhido.valor | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</span>
              </div>
            </div>
            <hr class="border-graphite-200">
            <div class="flex justify-between font-semibold">
              <span>Total</span>
              <span class="text-lg">{{ p.valorTotal | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</span>
            </div>
            <p *ngIf="submitError()" class="form-error">{{ submitError() }}</p>
            <button class="btn-primary w-full"
                    type="button"
                    [disabled]="submitting()"
                    (click)="onFinalizar()">
              <span *ngIf="!submitting()">Finalizar compra · {{ p.valorTotal | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</span>
              <span *ngIf="submitting()">Processando…</span>
            </button>
          </aside>
        </div>
      </ng-container>

      <!-- Dialog de estoque alterado -->
      <div *ngIf="estoqueDialog()"
           class="fixed inset-0 z-50 flex items-center justify-center bg-graphite-900/60 px-4">
        <div class="w-full max-w-md rounded-lg bg-graphite-0 p-6">
          <h2 class="text-lg text-graphite-900">Estoque mudou</h2>
          <p class="mt-2 text-sm text-graphite-700">
            Algumas mudanças aconteceram no seu carrinho enquanto você revisava o pedido.
          </p>
          <p class="mt-2 rounded bg-graphite-100 px-3 py-2 text-sm font-mono text-graphite-700">
            {{ estoqueDialog()!.mensagem }}
          </p>
          <div class="mt-4 flex justify-end gap-2">
            <button class="btn-primary" type="button" (click)="ajustarCarrinho()">Ajustar carrinho</button>
          </div>
        </div>
      </div>
    </section>
  `,
})
export class CheckoutReviewPage implements OnInit {
  private readonly addressService = inject(AddressService);
  private readonly paymentMethod = inject(PaymentMethodService);
  private readonly checkout = inject(CheckoutService);
  private readonly cart = inject(CartService);
  readonly state = inject(CheckoutStateService);
  private readonly router = inject(Router);

  readonly WIZARD_STEPS = [
    { label: 'Endereço' }, { label: 'Frete' }, { label: 'Pagamento' }, { label: 'Revisão' },
  ];

  readonly preview = signal<CheckoutPreviewResponse | null>(null);
  readonly endereco = signal<EnderecoResponse | null>(null);
  readonly pagamento = signal<FormaPagamentoResponse | null>(null);
  readonly loading = signal(true);
  readonly loadError = signal<string | null>(null);
  readonly submitting = signal(false);
  readonly submitError = signal<string | null>(null);
  readonly estoqueDialog = signal<{ mensagem: string } | null>(null);

  ngOnInit(): void {
    const s = this.state.state();
    if (!s.enderecoEntregaId || !s.opcaoFreteCodigo || !s.formaPagamentoId) {
      this.router.navigate(['/checkout/endereco']);
      return;
    }

    this.addressService.list().subscribe((list) => {
      this.endereco.set(list.find((e) => e.id === s.enderecoEntregaId) ?? null);
    });
    this.paymentMethod.list().subscribe((list) => {
      this.pagamento.set(list.find((m) => m.id === s.formaPagamentoId) ?? null);
    });

    this.checkout.preview({
      enderecoEntregaId: s.enderecoEntregaId,
      opcaoFreteCodigo: s.opcaoFreteCodigo,
      cupom: this.cart.cart()?.cupom?.codigo,
    }).subscribe({
      next: (p) => {
        this.preview.set(p);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        if (this.isEstoqueError(err)) {
          this.estoqueDialog.set({ mensagem: err.error.detail });
        } else {
          this.loadError.set(err.error?.detail ?? 'Não foi possível calcular o pedido.');
        }
      },
    });
  }

  /**
   * Backend devolve 422 com ProblemDetail.detail = "Estoque insuficiente para: ..."
   * (CheckoutService.validarEstoqueDisponivel) ou "Estoque insuficiente para SKU X..."
   * (InventoryService.reservar). Não há campo estruturado — detectamos pelo prefixo.
   */
  private isEstoqueError(err: HttpErrorResponse): boolean {
    if (err.status !== 422) return false;
    const detail: string | undefined = err.error?.detail;
    return typeof detail === 'string' && detail.toLowerCase().includes('estoque insufic');
  }

  async onFinalizar(): Promise<void> {
    const s = this.state.state();
    if (!s.enderecoEntregaId || !s.enderecoCobrancaId || !s.opcaoFreteCodigo || !s.formaPagamentoId) {
      this.router.navigate(['/checkout/endereco']);
      return;
    }
    const idempotencyKey = this.state.ensureIdempotencyKey();
    this.submitting.set(true);
    this.submitError.set(null);
    try {
      const resp = await firstValueFrom(this.checkout.placeOrder({
        enderecoEntregaId: s.enderecoEntregaId,
        enderecoCobrancaId: s.enderecoCobrancaId,
        opcaoFreteCodigo: s.opcaoFreteCodigo,
        formaPagamentoId: s.formaPagamentoId,
        cupom: this.cart.cart()?.cupom?.codigo,
        parcelas: s.parcelas,
        idempotencyKey,
      }));
      if (resp.status === 'APPROVED') {
        this.cart.clearLocal();
        this.state.clear();
        this.router.navigate(['/checkout/sucesso', resp.referenciaPedido], {
          state: { snapshot: resp },
        });
      } else {
        // REJECTED — gera key nova na próxima tentativa
        this.state.patch({ idempotencyKey: null });
        this.submitting.set(false);
        this.submitError.set('Pagamento recusado. Troque a forma de pagamento ou tente novamente.');
      }
    } catch (err) {
      this.submitting.set(false);
      const httpErr = err as HttpErrorResponse;
      if (this.isEstoqueError(httpErr)) {
        this.estoqueDialog.set({ mensagem: httpErr.error.detail });
        return;
      }
      // Erro de rede / 5xx — preserva idempotencyKey, permite reenvio
      this.submitError.set(httpErr.error?.detail
        ?? 'Falha ao confirmar. Verifique sua conexão e tente novamente.');
    }
  }

  ajustarCarrinho(): void {
    this.estoqueDialog.set(null);
    this.router.navigate(['/carrinho']);
  }

  formatCep(cep: string): string {
    const c = cep.replace(/\D/g, '');
    return c.length === 8 ? `${c.slice(0, 5)}-${c.slice(5)}` : cep;
  }
}
```

- [ ] **Step 2: Register route**

Modify `src/app/app.routes.ts` — add after the `pagamento` route:

```ts
{
  path: 'checkout/revisao',
  canActivate: [authGuard, checkoutStepGuard(['formaPagamentoId'])],
  loadComponent: () => import('./features/checkout/review.page').then((m) => m.CheckoutReviewPage),
},
```

- [ ] **Step 3: Typecheck**
```bash
npx ng build --configuration development
```
Expected: BUILD SUCCESS.

- [ ] **Step 4: Smoke validation**

1. Fluxo até `/checkout/revisao` → breakdown completo aparece.
2. Editar endereço/frete/pagamento → volta passo → mudar → voltar passo 4 → preview recalcula.
3. Cupom `10OFF` no carrinho → linha "Cupom 10OFF − R$ X,XX" aparece.
4. Cartão `1111` → "Finalizar compra" → request `POST /place-order` 200 → redirect `/checkout/sucesso/PH-2026-NNNNNN`.
5. Cartão `4000` (last4=4000) → toast erro, permanece. DevTools sessionStorage: `idempotencyKey` voltou a `null`.
6. Estoque race: em outro terminal, simular estoque zerado para o SKU no carrinho:
   ```bash
   docker exec -i pethub-postgres psql -U pethub -d pethub <<'SQL'
   UPDATE estoque SET quantidade = 0
   WHERE produto_id = (SELECT id FROM produtos WHERE sku = 'COLLAR-PRO-001');
   SQL
   ```
   Recarregar `/checkout/revisao` (ou clicar "Finalizar") → dialog "Estoque mudou" abre com a mensagem do backend (ex.: `Estoque insuficiente para: SKU COLLAR-PRO-001 (solic=1, disp=0)`) → CTA "Ajustar carrinho" → `/carrinho`. Restaurar:
   ```bash
   docker exec -i pethub-postgres psql -U pethub -d pethub <<'SQL'
   UPDATE estoque SET quantidade = 50
   WHERE produto_id = (SELECT id FROM produtos WHERE sku = 'COLLAR-PRO-001');
   SQL
   ```

- [ ] **Step 5: Commit**
```bash
cd /home/ali/projects/pet-hub && git add frontend/storefront/src/app/features/checkout/review.page.ts frontend/storefront/src/app/app.routes.ts
git commit -m "feat(storefront): checkout step 4 — review with preview + idempotent place-order + stock-changed dialog"
```

---

## Task 14: Wizard terminal — `/checkout/sucesso/:numero` (confetti)

**Files:**
- Create: `src/app/features/checkout/success.page.ts`
- Modify: `src/app/app.routes.ts`

- [ ] **Step 1: Create CheckoutSuccessPage**

Path: `src/app/features/checkout/success.page.ts`

```ts
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import confetti from 'canvas-confetti';

import { PlaceOrderResponse } from '../../core/models/checkout';

@Component({
  selector: 'app-checkout-success-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <section class="mx-auto max-w-page px-6 py-16 text-center">
      <div class="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-leaf-100 text-3xl text-leaf-700">
        ✓
      </div>
      <h1 class="mt-6 text-3xl text-graphite-900">Pedido confirmado!</h1>
      <p class="mt-2 text-graphite-500">
        Número do pedido: <span class="font-mono text-graphite-900">{{ numero() }}</span>
      </p>

      <div *ngIf="snapshot() as s" class="mx-auto mt-8 max-w-md surface text-left space-y-2 text-sm">
        <div class="flex justify-between">
          <span class="text-graphite-500">Total pago</span>
          <span class="font-semibold">{{ s.valorTotal | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</span>
        </div>
        <div class="flex justify-between">
          <span class="text-graphite-500">Método</span>
          <span>{{ humanizeMetodo(s.metodo) }}</span>
        </div>
        <div *ngIf="s.qrCode" class="flex justify-between">
          <span class="text-graphite-500">PIX QR Code</span>
          <span class="font-mono text-xs break-all">{{ s.qrCode }}</span>
        </div>
        <div *ngIf="s.boletoUrl" class="flex justify-between">
          <span class="text-graphite-500">Boleto</span>
          <a [href]="s.boletoUrl" target="_blank" rel="noopener" class="text-coral-500">Abrir PDF</a>
        </div>
      </div>

      <div class="mt-8 flex flex-wrap justify-center gap-3">
        <button class="btn-secondary"
                type="button"
                disabled
                title="Disponível em breve">
          Acompanhar pedido
        </button>
        <a routerLink="/produtos" class="btn-primary">Voltar à loja</a>
      </div>
    </section>
  `,
})
export class CheckoutSuccessPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly numero = signal<string>('');
  readonly snapshot = signal<PlaceOrderResponse | null>(null);

  ngOnInit(): void {
    this.numero.set(this.route.snapshot.paramMap.get('numero') ?? '');
    const nav = this.router.getCurrentNavigation();
    const fromState = (nav?.extras.state as { snapshot?: PlaceOrderResponse } | undefined)?.snapshot
      ?? (history.state?.snapshot as PlaceOrderResponse | undefined);
    if (fromState) this.snapshot.set(fromState);
    this.fireConfetti();
  }

  humanizeMetodo(m: string): string {
    switch (m) {
      case 'CARTAO_CREDITO': return 'Cartão de crédito';
      case 'CARTAO_DEBITO': return 'Cartão de débito';
      case 'PIX': return 'PIX';
      case 'BOLETO': return 'Boleto';
      default: return m;
    }
  }

  private fireConfetti(): void {
    if (typeof window === 'undefined') return;
    confetti({
      particleCount: 200,
      spread: 90,
      origin: { y: 0.6 },
      ticks: 200,                 // ≈2.5s a 80fps
    });
  }
}
```

- [ ] **Step 2: Register route**

Modify `src/app/app.routes.ts` — add after the `revisao` route:

```ts
{
  path: 'checkout/sucesso/:numero',
  canActivate: [authGuard],
  loadComponent: () => import('./features/checkout/success.page').then((m) => m.CheckoutSuccessPage),
},
```

- [ ] **Step 3: Typecheck**
```bash
npx ng build --configuration development
```
Expected: BUILD SUCCESS.

- [ ] **Step 4: Smoke validation**

1. Fluxo completo até "Finalizar compra" com `1111` → redirect para `/checkout/sucesso/PH-2026-NNNNNN`.
2. Confete dispara uma vez (não em loop). Recarregar a página: confete dispara de novo (esperado — `ngOnInit` roda outra vez).
3. Resumo mostra total + método.
4. "Acompanhar pedido" desabilitado com cursor `not-allowed`.
5. "Voltar à loja" → `/produtos`.

- [ ] **Step 5: Commit**
```bash
cd /home/ali/projects/pet-hub && git add frontend/storefront/src/app/features/checkout/success.page.ts frontend/storefront/src/app/app.routes.ts
git commit -m "feat(storefront): checkout success page with confetti + order number + summary"
```

---

## Task 15: End-to-end smoke + memory update + slice closing commit

**Files:**
- Modify: `/home/ali/.claude/projects/-home-ali-projects-pet-hub/memory/project_fase5_em_andamento.md`

- [ ] **Step 1: Full smoke E2E** (executa o checklist completo da §10 da spec)

Pre-cond: infra + backend + storefront rodando.

Login Maria → executar **em sequência sem fechar a aba**:

a) **Carrinho**
- [ ] Adicionar `COLLAR-PRO-001` qty=1 do PDP.
- [ ] Header mostra badge `1`.
- [ ] `/carrinho`: foto, preço, subtotal corretos.
- [ ] `+` → qty=2, subtotal dobra; `-` em qty=1 remove item.
- [ ] Adicionar novamente; cupom `10OFF` → chip aparece.
- [ ] Cupom `EXPIRED` → backend aceita anexar.
- [ ] Remover cupom → chip some.

b) **Endereço**
- [ ] `/checkout/endereco`: endereço da Maria pré-selecionado (ou criar com CEP `01310-100`).
- [ ] Desmarcar "mesmo para cobrança" → segunda lista aparece.

c) **Frete**
- [ ] 3 opções; mais barato selecionado automaticamente.

d) **Pagamento**
- [ ] Tab Cartão sem cartão → form aberto.
- [ ] `4111 1111 1111 1111`, `Maria Teste`, `12/2030`, CVV `123` → "Salvar" → tokenize 200 → cartão aparece selecionado.
- [ ] DevTools Network: PAN só em `/tokenize`, não em outros requests.
- [ ] DevTools Application → SessionStorage `pethub:checkout:v1`: sem PAN/CVV/token.
- [ ] Tab PIX → card info; Tab Boleto → card info.

e) **Revisão**
- [ ] `/checkout/revisao`: breakdown completo, com linha de cupom `10OFF`.
- [ ] "Editar endereço" → passo 1 → voltar → preview recalcula.

f) **Idempotência (network)**
- [ ] DevTools Network → throttle Slow 3G + "Finalizar" → loading bloqueia 2º clique.
- [ ] DevTools Application → SessionStorage: `idempotencyKey` presente antes do submit.

g) **Sucesso**
- [ ] Redireciona `/checkout/sucesso/PH-2026-NNNNNN`.
- [ ] Confete dispara 1×.
- [ ] Resumo: total + método.
- [ ] DevTools Application → SessionStorage: `pethub:checkout:v1` REMOVIDO após APPROVED.
- [ ] `/carrinho` agora vazio.

h) **Recusa**
- [ ] Refazer fluxo com cartão novo `4000 0000 0000 4000` (CVV `123`, valid `12/2030`).
- [ ] "Finalizar" → toast erro vermelho, permanece em `/checkout/revisao`.
- [ ] SessionStorage: `idempotencyKey` voltou a `null`.

i) **AppSec spot-check**
- [ ] DevTools Application → Cookies: `pethub_refresh` HttpOnly + SameSite=Lax.
- [ ] LocalStorage: vazio (ou só prefs, sem tokens).
- [ ] Console: zero erros não-handled durante o fluxo todo.

Se algum item falhar, fix-it inline (não criar nova task), reverificar, e só então prosseguir.

- [ ] **Step 2: Atualizar memory para marcar slice 3 entregue**

Write file `/home/ali/.claude/projects/-home-ali-projects-pet-hub/memory/project_fase5_em_andamento.md` with the following content (substitui o existente):

```markdown
---
name: project-fase5-em-andamento
description: Pet Hub Fase 5 (Storefront Angular) — slices 1, 2 e 3 entregues em origin/main; slice 4 ainda pendente
metadata:
  node_type: memory
  type: project
---

Fase 5 do Pet Hub: 3 dos 4 slices em `origin/main`. **Slice 4 (minha conta + timeline visual) é o último, pendente.**

## ✅ Slices entregues

- `7c37102 feat(identity)` — refresh em cookie HttpOnly.
- `2778cf8 feat(storefront) slice 1` — scaffold + auth (login/cadastro).
- `167a887 feat(storefront) slice 2` — catálogo (home/lista/detalhe + calc frete).
- `feat(storefront) slice 3` (2026-05-16) — carrinho + checkout 4 passos + sucesso com confete.
  Decisões congeladas: sessionStorage `pethub:checkout:v1`, cadastro inline de endereço/cartão no wizard, cupom só em `/carrinho`, tokenize via `POST /customers/payment-methods/tokenize` (PAN só nessa request, nunca persistido client-side). Idempotency-key gerada com `crypto.randomUUID()`, reusada em retry de transporte e regenerada após REJECTED semântico. Dialog "Estoque mudou" trata 422 do place-order.

## ⏳ Slice 4 — Minha conta + Timeline visual (o WOW)
- `/minha-conta` overview (cards: perfil, endereços, cartões, pets, pedidos).
- CRUDs: `/minha-conta/perfil`, `/enderecos`, `/cartoes`, `/pets`.
- `/minha-conta/pedidos` listagem.
- `/minha-conta/pedidos/:numero` com **OrderTimelineComponent** (linha horizontal desktop / vertical mobile, ícones por etapa, estado concluida/atual/pendente, tooltip data-hora, comparativo de tempo médio).
- Habilitar o CTA "Acompanhar pedido" da tela de sucesso do slice 3 (hoje está disabled).
- DOMPurify pipe `safe-html.pipe.ts` para HTML rico (substitui o escape manual no detail do produto).

## 🚀 Como retomar

```bash
cd /home/ali/projects/pet-hub
git log --oneline -8
git status

# Infra
docker compose --env-file .env.local -f infrastructure/docker/docker-compose.dev.yml up -d
# Backend
cd backend && docker run --rm -d --name pethub-app-dev \
  --env-file /home/ali/projects/pet-hub/.env.local \
  -v "$PWD":/workspace -v /home/ali/.m2:/root/.m2 \
  --network host -w /workspace \
  maven:3.9-eclipse-temurin-21 \
  mvn -B -ntp -pl application spring-boot:run -DskipTests
# Frontend
cd /home/ali/projects/pet-hub/frontend/storefront
npx ng serve --host 127.0.0.1 --port 4242 --proxy-config proxy.conf.json
```

Storefront em http://127.0.0.1:4242 — login `maria.fase2@pethub.com / Senha@123`.

## ⚠️ Dívidas / decisões a confirmar

- Testes Karma/Jest formais continuam dívida (mesmo bloqueio das Fases 1-4). Spec slice 3 §11 lista os casos. Endereçar junto com setup de CI (Fase 9).
- `appsec-pendencias.md` continua aberto: JWT-1, CORS-1, VAL-1 viram bloqueantes só no caminho para staging.
- `fase-5-pendencias.md` será criado depois do slice 4 fechar.

Linked: [[project-fase4-concluida]], [[feedback-coding-patterns]].
```

- [ ] **Step 3: Final closing commit**

```bash
cd /home/ali/projects/pet-hub && git log --oneline -20
```

Confirma que os ~13 commits do slice 3 estão na história. Se algum smoke falhou e foi corrigido inline, garante que o último commit cobre o fix.

Atualizar `ROADMAP.md` para marcar a Fase 5 com progresso `🚧 (slice 3 done, slice 4 pending)` — abrir o arquivo, achar a linha da Fase 5 e ajustar. Conventional commit:

```bash
cd /home/ali/projects/pet-hub && git add ROADMAP.md
git commit -m "docs(roadmap): slice 3 da Fase 5 entregue (carrinho + checkout + sucesso)"
```

(Skip if `git status` mostra ROADMAP.md sem alterações nesta seção — o file pode já estar genérico.)

---

## Done

Final state:
- 6 rotas novas (`/carrinho` + 5 checkout).
- 5 services novos (Cart, Checkout, CheckoutState, Address, PaymentMethod).
- 1 guard novo (checkoutStepGuard).
- 8 components novos (cart-item-row, coupon-input, stepper + 5 páginas).
- Auth interceptor inalterado (já cobria `/cart`, `/checkout`, `/customers`).
- `canvas-confetti` adicionado como dep.
- Tokenize trafega PAN/CVV só na request dedicada; sessionStorage nunca recebe esses campos.
- Idempotency-key correta: reusada em retry de transporte, regenerada após REJECTED.
- Smoke E2E completo passou.

Next session: slice 4 (memory já indica como retomar).
