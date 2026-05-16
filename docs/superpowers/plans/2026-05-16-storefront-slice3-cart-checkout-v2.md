# Storefront Slice 3 — Cart + Checkout 4-step Wizard — Plano v2 (Tasks 9-15)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Por que v2:** O plano v1 (`2026-05-16-storefront-slice3-cart-checkout.md`) foi escrito com a estrutura `features/*` flat. O refactor estrutural em `d80e519` migrou o storefront para padrão `modules/` (igual `guep-crm`), separou templates em arquivos `.html` + `.scss`, introduziu path aliases (`@core`, `@shared`, `@modules`, `@env`) e padronizou nomes descritivos sem variáveis de 1 letra. Tasks 1-8 já foram executadas e ficam no v1 como histórico. Este v2 reescreve as Tasks 9-15 alinhadas à estrutura atual.

**Spec:** [`docs/superpowers/specs/2026-05-16-storefront-slice3-cart-checkout-design.md`](../specs/2026-05-16-storefront-slice3-cart-checkout-design.md) (inalterada — apenas a tradução em arquivos mudou).

**Working directory:** `/home/ali/projects/pet-hub/frontend/storefront/`

---

## Convenções vigentes (todas as tasks abaixo)

1. **Standalone components, sempre.** `imports: [...]` no decorator, sem NgModules.
2. **Templates e estilos em arquivos separados** — cada `.ts` aponta para um `.html` (`templateUrl: './x.component.html'`) e `.scss` próprio na mesma pasta (`styleUrls: ['./x.component.scss']`). O `.scss` pode iniciar vazio se todo styling estiver coberto por Tailwind utility + classes globais em `src/styles.css` (`.btn-primary`, `.surface`, `.input`, `.label`, `.form-error`, `.radio-card`, `.stepper*`).
3. **Cada componente em pasta própria**: `modules/<nome-modulo>/{components,pages}/<nome-componente-ou-page>/{ts,html,scss}`. Mesmo um `app-root` simples segue o padrão (`app.component.{ts,html,scss}`).
4. **Path aliases nos imports cross-módulo.** Configurados em `tsconfig.json`:
   - `@core/*` → `src/app/core/*`
   - `@shared/*` → `src/app/shared/*`
   - `@modules/*` → `src/app/modules/*`
   - `@validators/*` → `src/app/validators/*`
   - `@env/*` → `src/environments/*`
   Imports intra-módulo (`../models/x`, `../services/y`) permanecem relativos.
5. **Nada de variáveis de 1 letra.** Nunca use `e`, `c`, `p`, `s`, `r`, `i`, `v`, `n`, `t`, `m`, `o`, `q`, etc. para argumentos, callbacks, lambdas, loops, destructuring. Use nomes descritivos: `httpError`, `cartResponse`, `produtoExibido`, `stateSnapshot`, `pagaResposta`, `stepIndex`, `valorAtual`, `cepSemMascara`, etc. Mesma regra para identifiers de 2 letras puramente abreviativos (`fb` → `formBuilder`).
6. **Métodos descrevem o que fazem.** Em vez de `onSubmit()` use `entrar()` / `cadastrar()` / `finalizarCompra()`. Em vez de `onInc()` use `incrementarQty()`. Em vez de `fetch()` use `buscarProdutos()` / `carregarPedidos()`.
7. **`withCredentials`** continua coberto pelo interceptor para os paths em `environment.credentialedPaths`. NUNCA passar manualmente nas chamadas `http.post(..., { withCredentials: true })` em código novo deste slice.
8. **Backend RFC 7807 Problem Details** — error body tem `{ status, detail, errors? }`. UI consome `error.detail` primeiro, fallback para mensagens por código HTTP. Stack trace nunca é exibido.
9. **Testes Karma/Jest formais permanecem dívida acumulada** (spec §11). Cada task usa `ng build --configuration development` como typecheck. Smoke E2E manual completo na Task 15.

**Estado atual da árvore (pós-`d80e519`):**

```
src/app/
├── app.component.{ts,html,scss}
├── app.config.ts
├── app.routes.ts
├── core/
│   ├── guards/auth.guard.ts
│   ├── interceptors/auth.interceptor.ts
│   ├── layout/main-layout/main-layout.component.{ts,html,scss}     # cart badge entra na Task 9
│   └── services/auth.service.ts
├── shared/
│   ├── components/
│   │   ├── price-display/price-display.component.{ts,html,scss}
│   │   ├── product-card/product-card.component.{ts,html,scss}
│   │   └── shipping-calculator/shipping-calculator.component.{ts,html,scss}
│   ├── models/{page,shipping,user}.ts
│   └── services/shipping.service.ts
└── modules/
    ├── auth/pages/{login,register}/{ts,html,scss}
    ├── home/pages/home/{ts,html,scss}
    ├── catalog/
    │   ├── models/catalog.ts
    │   ├── pages/{product-list,product-detail}/{ts,html,scss}      # product-detail.ts já chama CartService (foi feito no refactor)
    │   └── services/catalog.service.ts
    ├── customer/
    │   ├── models/{address,payment-method}.ts
    │   └── services/{address,payment-method}.service.ts
    ├── cart/
    │   ├── components/
    │   │   ├── cart-item-row/cart-item-row.component.{ts,html,scss}
    │   │   └── coupon-input/coupon-input.component.{ts,html,scss}
    │   ├── models/cart.ts
    │   ├── pages/      # cart.page virá aqui na Task 9
    │   └── services/cart.service.ts
    └── checkout/
        ├── components/stepper/stepper.component.{ts,html,scss}
        ├── guards/checkout-step.guard.ts
        ├── models/checkout.ts
        ├── pages/      # address/shipping/payment/review/success virão aqui (Tasks 10-14)
        └── services/{checkout,checkout-state}.service.ts
```

---

## Task 9: CartPage + cart badge no header

**Files:**
- Create: `src/app/modules/cart/pages/cart/cart.page.{ts,html,scss}`
- Modify: `src/app/app.routes.ts`
- Modify: `src/app/core/layout/main-layout/main-layout.component.{ts,html}` — adicionar cart badge

> NOTA: O `ProductDetailPage` **já foi atualizado** durante o refactor `d80e519` para chamar `CartService.add()` real (não mais o setTimeout mock). Não precisa tocar nele aqui.

- [ ] **Step 1: Criar CartPage `.html`**

Path: `src/app/modules/cart/pages/cart/cart.page.html`

```html
<section class="mx-auto max-w-page px-6 py-12">
  <h1 class="text-3xl text-graphite-900">Carrinho</h1>

  <ng-container *ngIf="authService.isAuthenticated(); else painelDeslogado">
    <ng-container *ngIf="cartService.cart() as carrinhoAtual">
      <div *ngIf="carrinhoAtual.items.length; else carrinhoVazio"
           class="mt-8 grid gap-12 lg:grid-cols-[1fr_360px]">
        <div>
          <app-cart-item-row *ngFor="let itemCarrinho of carrinhoAtual.items"
                             [item]="itemCarrinho"
                             [busy]="skuOcupado() === itemCarrinho.sku"
                             (updateQty)="atualizarQty($event.sku, $event.qty)"
                             (remove)="removerItem($event)" />
          <p *ngIf="erroLista()" class="form-error mt-3">{{ erroLista() }}</p>
        </div>

        <aside class="space-y-6">
          <div class="surface space-y-4">
            <h2 class="text-lg text-graphite-900">Resumo</h2>
            <div class="flex justify-between text-sm">
              <span class="text-graphite-500">Itens ({{ carrinhoAtual.totalItens }})</span>
              <span>{{ carrinhoAtual.subtotal | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</span>
            </div>
            <p class="text-xs text-graphite-500">Frete calculado no checkout.</p>
            <button class="btn-primary w-full"
                    type="button"
                    [disabled]="skuOcupado() !== null"
                    (click)="irParaCheckout()">
              Finalizar compra
            </button>
          </div>

          <div class="surface">
            <app-coupon-input [cupom]="carrinhoAtual.cupom"
                              [busy]="cupomEmProcessamento()"
                              [error]="erroCupom()"
                              (apply)="aplicarCupom($event)"
                              (remove)="removerCupom()" />
          </div>

          <div class="surface">
            <p class="label mb-2">Estimativa de frete</p>
            <app-shipping-calculator [sku]="carrinhoAtual.items[0].sku"
                                     [qty]="carrinhoAtual.items[0].qty" />
          </div>
        </aside>
      </div>
    </ng-container>

    <ng-template #carrinhoVazio>
      <div class="mt-12 rounded-lg border border-graphite-200 bg-graphite-0 py-16 text-center">
        <p class="text-graphite-600">Seu carrinho está vazio.</p>
        <a routerLink="/produtos" class="btn-primary mt-4 inline-flex">Explorar produtos</a>
      </div>
    </ng-template>
  </ng-container>

  <ng-template #painelDeslogado>
    <div class="mt-12 rounded-lg border border-graphite-200 bg-graphite-0 py-16 text-center">
      <p class="text-graphite-600">Faça login para ver seu carrinho.</p>
      <a routerLink="/login" class="btn-primary mt-4 inline-flex">Entrar</a>
    </div>
  </ng-template>
</section>
```

- [ ] **Step 2: Criar CartPage `.scss`**

Path: `src/app/modules/cart/pages/cart/cart.page.scss` — arquivo vazio (todo o styling está em Tailwind utility + classes globais).

```scss
```

- [ ] **Step 3: Criar CartPage `.ts`**

Path: `src/app/modules/cart/pages/cart/cart.page.ts`

```ts
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

import { AuthService } from '@core/services/auth.service';
import { CartService } from '@modules/cart/services/cart.service';
import { CartItemRowComponent } from '@modules/cart/components/cart-item-row/cart-item-row.component';
import { CouponInputComponent } from '@modules/cart/components/coupon-input/coupon-input.component';
import { ShippingCalculatorComponent } from '@shared/components/shipping-calculator/shipping-calculator.component';

@Component({
  selector: 'app-cart-page',
  standalone: true,
  imports: [CommonModule, RouterLink, CartItemRowComponent, CouponInputComponent, ShippingCalculatorComponent],
  templateUrl: './cart.page.html',
  styleUrls: ['./cart.page.scss'],
})
export class CartPage implements OnInit {
  readonly authService = inject(AuthService);
  readonly cartService = inject(CartService);
  private readonly router = inject(Router);

  readonly skuOcupado = signal<string | null>(null);
  readonly cupomEmProcessamento = signal(false);
  readonly erroCupom = signal<string | null>(null);
  readonly erroLista = signal<string | null>(null);

  ngOnInit(): void {
    if (!this.authService.isAuthenticated()) return;
    this.cartService.load().subscribe({
      error: () => this.erroLista.set('Não foi possível carregar o carrinho.'),
    });
  }

  atualizarQty(sku: string, novaQty: number): void {
    this.skuOcupado.set(sku);
    this.erroLista.set(null);
    this.cartService.updateQty(sku, novaQty).subscribe({
      next: () => this.skuOcupado.set(null),
      error: (httpError: HttpErrorResponse) => {
        this.skuOcupado.set(null);
        this.erroLista.set(httpError.error?.detail ?? 'Não foi possível atualizar a quantidade.');
      },
    });
  }

  removerItem(sku: string): void {
    this.skuOcupado.set(sku);
    this.cartService.remove(sku).subscribe({
      next: () => this.skuOcupado.set(null),
      error: (httpError: HttpErrorResponse) => {
        this.skuOcupado.set(null);
        this.erroLista.set(httpError.error?.detail ?? 'Não foi possível remover o item.');
      },
    });
  }

  aplicarCupom(codigoCupom: string): void {
    this.cupomEmProcessamento.set(true);
    this.erroCupom.set(null);
    this.cartService.applyCoupon(codigoCupom).subscribe({
      next: () => this.cupomEmProcessamento.set(false),
      error: (httpError: HttpErrorResponse) => {
        this.cupomEmProcessamento.set(false);
        this.erroCupom.set(httpError.error?.detail ?? 'Cupom inválido.');
      },
    });
  }

  removerCupom(): void {
    this.cupomEmProcessamento.set(true);
    this.cartService.removeCoupon().subscribe({
      next: () => this.cupomEmProcessamento.set(false),
      error: () => this.cupomEmProcessamento.set(false),
    });
  }

  irParaCheckout(): void {
    this.router.navigate(['/checkout/endereco']);
  }
}
```

- [ ] **Step 4: Atualizar `MainLayoutComponent` adicionando cart badge**

Modify `src/app/core/layout/main-layout/main-layout.component.html` — substituir o conteúdo do `<nav>` para incluir o link "Carrinho" com badge:

```html
<div class="min-h-screen flex flex-col">
  <header class="border-b border-graphite-200 bg-graphite-0">
    <div class="mx-auto flex max-w-page items-center justify-between px-6 py-4">
      <a routerLink="/" class="text-xl font-display font-semibold text-graphite-900">
        Pet Hub
      </a>
      <nav class="flex items-center gap-6 text-sm">
        <a routerLink="/produtos" routerLinkActive="text-coral-500"
           class="text-graphite-700 hover:text-graphite-900">Catálogo</a>
        <a *ngIf="usuarioLogado()" routerLink="/carrinho" routerLinkActive="text-coral-500"
           class="relative text-graphite-700 hover:text-graphite-900">
          Carrinho
          <span *ngIf="quantidadeItensCarrinho() > 0"
                class="absolute -right-3 -top-2 inline-flex h-4 min-w-[1rem] items-center
                       justify-center rounded-full bg-coral-500 px-1 text-[10px] font-medium text-white">
            {{ quantidadeItensCarrinho() }}
          </span>
        </a>
        <ng-container *ngIf="usuarioLogado(); else navDeslogado">
          <a routerLink="/minha-conta" routerLinkActive="text-coral-500"
             class="text-graphite-700 hover:text-graphite-900">
            {{ nomeUsuario() }}
          </a>
          <button class="btn-ghost text-sm py-1.5 px-3" (click)="sairDaConta()">Sair</button>
        </ng-container>
        <ng-template #navDeslogado>
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
```

Modify `src/app/core/layout/main-layout/main-layout.component.ts` — adicionar inject do `CartService` e expor `quantidadeItensCarrinho`:

```ts
import { Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';

import { AuthService } from '@core/services/auth.service';
import { CartService } from '@modules/cart/services/cart.service';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './main-layout.component.html',
  styleUrls: ['./main-layout.component.scss'],
})
export class MainLayoutComponent {
  private readonly authService = inject(AuthService);
  private readonly cartService = inject(CartService);

  readonly usuarioLogado = this.authService.isAuthenticated;
  readonly nomeUsuario = computed(() => this.authService.currentUser()?.nome ?? '');
  readonly quantidadeItensCarrinho = this.cartService.totalItens;

  sairDaConta(): void {
    this.authService.logout().subscribe();
  }
}
```

- [ ] **Step 5: Registrar rota `/carrinho`**

Modify `src/app/app.routes.ts` — adicionar a rota entre `produtos/:sku` e `minha-conta`:

```ts
{
  path: 'carrinho',
  loadComponent: () =>
    import('@modules/cart/pages/cart/cart.page').then((moduleCart) => moduleCart.CartPage),
},
```

- [ ] **Step 6: Typecheck**

```bash
cd /home/ali/projects/pet-hub/frontend/storefront && npx ng build --configuration development
```
Expected: BUILD SUCCESS.

- [ ] **Step 7: Commit**

```bash
cd /home/ali/projects/pet-hub && git add \
  frontend/storefront/src/app/modules/cart/pages/cart \
  frontend/storefront/src/app/core/layout/main-layout \
  frontend/storefront/src/app/app.routes.ts
git commit -m "feat(storefront): add /carrinho page + cart badge in header"
```

---

## Task 10: Wizard step 1 — `/checkout/endereco`

**Files:**
- Create: `src/app/modules/checkout/pages/address/address.page.{ts,html,scss}`
- Modify: `src/app/app.routes.ts`

- [ ] **Step 1: Criar `address.page.html`**

Path: `src/app/modules/checkout/pages/address/address.page.html`

```html
<section class="mx-auto max-w-page px-6 py-12">
  <app-stepper [steps]="passosCheckout" [active]="1" />

  <h1 class="mt-8 text-2xl text-graphite-900">Endereço de entrega</h1>

  <div *ngIf="enderecosCadastrados().length" class="mt-6 space-y-3">
    <label *ngFor="let enderecoCadastrado of enderecosCadastrados()" class="block">
      <input type="radio" name="enderecoEntrega" class="peer sr-only"
             [value]="enderecoCadastrado.id"
             [checked]="enderecoEntregaId() === enderecoCadastrado.id"
             (change)="selecionarEnderecoEntrega(enderecoCadastrado.id)" />
      <div class="radio-card">
        <div class="flex justify-between">
          <p class="font-medium text-graphite-900">{{ enderecoCadastrado.apelido }}
            <span *ngIf="enderecoCadastrado.padraoEntrega"
                  class="ml-2 text-xs text-coral-500">padrão</span>
          </p>
          <p class="text-xs text-graphite-500">{{ enderecoCadastrado.tipo }}</p>
        </div>
        <p class="mt-1 text-sm text-graphite-700">
          {{ enderecoCadastrado.logradouro }}<ng-container *ngIf="enderecoCadastrado.numero">, {{ enderecoCadastrado.numero }}</ng-container>
          <ng-container *ngIf="enderecoCadastrado.complemento"> — {{ enderecoCadastrado.complemento }}</ng-container>
        </p>
        <p class="text-sm text-graphite-500">
          {{ enderecoCadastrado.bairro }}, {{ enderecoCadastrado.cidade }}/{{ enderecoCadastrado.uf }} ·
          {{ formatarCep(enderecoCadastrado.cep) }}
        </p>
      </div>
    </label>
  </div>

  <button *ngIf="enderecosCadastrados().length && !mostrandoFormularioNovo()"
          type="button"
          class="btn-ghost mt-4"
          (click)="mostrandoFormularioNovo.set(true)">
    + Adicionar novo endereço
  </button>

  <form *ngIf="mostrandoFormularioNovo() || enderecosCadastrados().length === 0"
        [formGroup]="formularioEndereco" (ngSubmit)="salvarNovoEndereco()"
        class="mt-6 surface space-y-4">
    <h2 class="text-base text-graphite-900">Novo endereço</h2>
    <div class="grid grid-cols-1 gap-4 sm:grid-cols-3">
      <div class="sm:col-span-1">
        <label class="label" for="cep">CEP</label>
        <input id="cep" class="input"
               formControlName="cep"
               inputmode="numeric" maxlength="9"
               placeholder="00000-000"
               (blur)="autopreencherViaCep()" />
        <p *ngIf="erroCep()" class="form-error">{{ erroCep() }}</p>
      </div>
      <div class="sm:col-span-2">
        <label class="label" for="apelido">Apelido</label>
        <input id="apelido" class="input" formControlName="apelido"
               placeholder="Casa, Trabalho..." />
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
        <input id="complemento" class="input" formControlName="complemento"
               placeholder="Apto 12, Bloco B..." />
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
          <option *ngFor="let unidadeFederativa of LISTA_UFS" [value]="unidadeFederativa">{{ unidadeFederativa }}</option>
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
    <p *ngIf="erroFormulario()" class="form-error">{{ erroFormulario() }}</p>
    <div class="flex gap-2">
      <button type="submit" class="btn-primary"
              [disabled]="formularioEndereco.invalid || salvandoEndereco()">
        <span *ngIf="!salvandoEndereco()">Salvar endereço</span>
        <span *ngIf="salvandoEndereco()">Salvando…</span>
      </button>
      <button *ngIf="enderecosCadastrados().length"
              type="button"
              class="btn-ghost"
              (click)="mostrandoFormularioNovo.set(false)">Cancelar</button>
    </div>
  </form>

  <div class="mt-8 surface">
    <label class="flex items-center gap-2">
      <input type="checkbox"
             [checked]="usarMesmoEnderecoCobranca()"
             (change)="alternarMesmoEnderecoCobranca()" />
      <span class="text-sm text-graphite-700">Usar mesmo endereço para cobrança</span>
    </label>
    <div *ngIf="!usarMesmoEnderecoCobranca()" class="mt-3 space-y-2">
      <p class="label">Endereço de cobrança</p>
      <label *ngFor="let enderecoCadastrado of enderecosCadastrados()" class="block">
        <input type="radio" name="enderecoCobranca" class="peer sr-only"
               [checked]="enderecoCobrancaId() === enderecoCadastrado.id"
               (change)="selecionarEnderecoCobranca(enderecoCadastrado.id)" />
        <div class="radio-card">
          <p class="text-sm">{{ enderecoCadastrado.apelido }} — {{ enderecoCadastrado.cidade }}/{{ enderecoCadastrado.uf }}</p>
        </div>
      </label>
    </div>
  </div>

  <div class="mt-8 flex justify-end">
    <button class="btn-primary"
            type="button"
            [disabled]="!enderecoEntregaId() || !enderecoCobrancaId()"
            (click)="prosseguirParaFrete()">
      Continuar para frete
    </button>
  </div>
</section>
```

- [ ] **Step 2: Criar `address.page.scss`** (vazio)

Path: `src/app/modules/checkout/pages/address/address.page.scss`

```scss
```

- [ ] **Step 3: Criar `address.page.ts`**

Path: `src/app/modules/checkout/pages/address/address.page.ts`

```ts
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { AddressService } from '@modules/customer/services/address.service';
import { CheckoutStateService } from '@modules/checkout/services/checkout-state.service';
import { EnderecoResponse, TipoEndereco, UFS, UnidadeFederativa } from '@modules/customer/models/address';
import { StepperComponent } from '@modules/checkout/components/stepper/stepper.component';

@Component({
  selector: 'app-checkout-address-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, StepperComponent],
  templateUrl: './address.page.html',
  styleUrls: ['./address.page.scss'],
})
export class CheckoutAddressPage implements OnInit {
  private readonly addressService = inject(AddressService);
  private readonly checkoutState = inject(CheckoutStateService);
  private readonly router = inject(Router);
  private readonly formBuilder = inject(FormBuilder);

  readonly LISTA_UFS = UFS;
  readonly passosCheckout = [
    { label: 'Endereço' },
    { label: 'Frete' },
    { label: 'Pagamento' },
    { label: 'Revisão' },
  ];

  readonly enderecosCadastrados = signal<EnderecoResponse[]>([]);
  readonly enderecoEntregaId = signal<number | null>(null);
  readonly enderecoCobrancaId = signal<number | null>(null);
  readonly usarMesmoEnderecoCobranca = signal(true);
  readonly mostrandoFormularioNovo = signal(false);
  readonly salvandoEndereco = signal(false);
  readonly erroCep = signal<string | null>(null);
  readonly erroFormulario = signal<string | null>(null);

  readonly formularioEndereco = this.formBuilder.nonNullable.group({
    apelido: ['', [Validators.required, Validators.maxLength(50)]],
    cep: ['', [Validators.required, Validators.pattern(/^\d{5}-?\d{3}$/)]],
    logradouro: ['', [Validators.required, Validators.maxLength(200)]],
    numero: [''],
    complemento: [''],
    bairro: ['', [Validators.required, Validators.maxLength(100)]],
    cidade: ['', [Validators.required, Validators.maxLength(100)]],
    uf: this.formBuilder.nonNullable.control<UnidadeFederativa>('SP', Validators.required),
    tipo: this.formBuilder.nonNullable.control<TipoEndereco>('RESIDENCIAL', Validators.required),
  });

  ngOnInit(): void {
    const estadoInicial = this.checkoutState.state();
    if (estadoInicial.enderecoEntregaId) this.enderecoEntregaId.set(estadoInicial.enderecoEntregaId);
    if (estadoInicial.enderecoCobrancaId) this.enderecoCobrancaId.set(estadoInicial.enderecoCobrancaId);
    if (estadoInicial.enderecoEntregaId && estadoInicial.enderecoCobrancaId
        && estadoInicial.enderecoEntregaId !== estadoInicial.enderecoCobrancaId) {
      this.usarMesmoEnderecoCobranca.set(false);
    }

    this.addressService.list().subscribe({
      next: (listaEnderecos) => {
        this.enderecosCadastrados.set(listaEnderecos);
        if (!this.enderecoEntregaId()) {
          const enderecoPadrao = listaEnderecos.find((endereco) => endereco.padraoEntrega) ?? listaEnderecos[0];
          if (enderecoPadrao) {
            this.enderecoEntregaId.set(enderecoPadrao.id);
            this.enderecoCobrancaId.set(enderecoPadrao.id);
          } else {
            this.mostrandoFormularioNovo.set(true);
          }
        }
      },
      error: () => this.mostrandoFormularioNovo.set(true),
    });
  }

  selecionarEnderecoEntrega(idEndereco: number): void {
    this.enderecoEntregaId.set(idEndereco);
    if (this.usarMesmoEnderecoCobranca()) this.enderecoCobrancaId.set(idEndereco);
  }

  selecionarEnderecoCobranca(idEndereco: number): void {
    this.enderecoCobrancaId.set(idEndereco);
  }

  alternarMesmoEnderecoCobranca(): void {
    const proximoValor = !this.usarMesmoEnderecoCobranca();
    this.usarMesmoEnderecoCobranca.set(proximoValor);
    if (proximoValor) this.enderecoCobrancaId.set(this.enderecoEntregaId());
  }

  autopreencherViaCep(): void {
    const cepSemMascara = this.formularioEndereco.controls.cep.value.replace(/\D/g, '');
    if (cepSemMascara.length !== 8) return;
    this.erroCep.set(null);
    this.addressService.lookupCep(cepSemMascara).subscribe({
      next: (respostaCep) => {
        if (respostaCep.erro) {
          this.erroCep.set('CEP não encontrado. Preencha manualmente.');
          return;
        }
        this.formularioEndereco.patchValue({
          logradouro: respostaCep.logradouro || this.formularioEndereco.controls.logradouro.value,
          bairro: respostaCep.bairro || this.formularioEndereco.controls.bairro.value,
          cidade: respostaCep.cidade || this.formularioEndereco.controls.cidade.value,
          uf: (respostaCep.uf as UnidadeFederativa) || this.formularioEndereco.controls.uf.value,
        });
      },
      error: () => this.erroCep.set('CEP não encontrado. Preencha manualmente.'),
    });
  }

  salvarNovoEndereco(): void {
    if (this.formularioEndereco.invalid) {
      this.formularioEndereco.markAllAsTouched();
      return;
    }
    this.salvandoEndereco.set(true);
    this.erroFormulario.set(null);
    const valoresFormulario = this.formularioEndereco.getRawValue();
    this.addressService.create({
      ...valoresFormulario,
      cep: valoresFormulario.cep.replace(/\D/g, ''),
      padraoEntrega: this.enderecosCadastrados().length === 0,
      padraoCobranca: this.enderecosCadastrados().length === 0,
    }).subscribe({
      next: (enderecoCriado) => {
        this.salvandoEndereco.set(false);
        this.enderecosCadastrados.update((lista) => [...lista, enderecoCriado]);
        this.enderecoEntregaId.set(enderecoCriado.id);
        if (this.usarMesmoEnderecoCobranca()) this.enderecoCobrancaId.set(enderecoCriado.id);
        this.mostrandoFormularioNovo.set(false);
        this.formularioEndereco.reset({
          uf: 'SP', tipo: 'RESIDENCIAL',
          apelido: '', cep: '', logradouro: '', numero: '',
          complemento: '', bairro: '', cidade: '',
        });
      },
      error: (httpError: HttpErrorResponse) => {
        this.salvandoEndereco.set(false);
        this.erroFormulario.set(httpError.error?.detail ?? 'Não foi possível salvar o endereço.');
      },
    });
  }

  prosseguirParaFrete(): void {
    this.checkoutState.patch({
      enderecoEntregaId: this.enderecoEntregaId(),
      enderecoCobrancaId: this.enderecoCobrancaId(),
    });
    this.router.navigate(['/checkout/frete']);
  }

  formatarCep(cep: string): string {
    const digitos = cep.replace(/\D/g, '');
    return digitos.length === 8 ? `${digitos.slice(0, 5)}-${digitos.slice(5)}` : cep;
  }
}
```

- [ ] **Step 4: Registrar rota**

Modify `src/app/app.routes.ts` — após a rota `carrinho`, importar guard e adicionar:

```ts
import { checkoutStepGuard } from '@modules/checkout/guards/checkout-step.guard';
```

```ts
{
  path: 'checkout/endereco',
  canActivate: [authGuard, checkoutStepGuard(['cart-not-empty'])],
  loadComponent: () =>
    import('@modules/checkout/pages/address/address.page')
      .then((moduleAddress) => moduleAddress.CheckoutAddressPage),
},
```

- [ ] **Step 5: Typecheck**
```bash
cd /home/ali/projects/pet-hub/frontend/storefront && npx ng build --configuration development
```

- [ ] **Step 6: Commit**
```bash
cd /home/ali/projects/pet-hub && git add \
  frontend/storefront/src/app/modules/checkout/pages/address \
  frontend/storefront/src/app/app.routes.ts
git commit -m "feat(storefront): checkout step 1 — address with ViaCEP autofill"
```

---

## Task 11: Wizard step 2 — `/checkout/frete`

**Files:**
- Create: `src/app/modules/checkout/pages/shipping/shipping.page.{ts,html,scss}`
- Modify: `src/app/app.routes.ts`

- [ ] **Step 1: Criar `shipping.page.html`**

Path: `src/app/modules/checkout/pages/shipping/shipping.page.html`

```html
<section class="mx-auto max-w-page px-6 py-12">
  <app-stepper [steps]="passosCheckout" [active]="2" />

  <h1 class="mt-8 text-2xl text-graphite-900">Frete</h1>
  <p *ngIf="enderecoSelecionado() as enderecoEntrega" class="mt-1 text-sm text-graphite-500">
    Entregando em {{ enderecoEntrega.cidade }}/{{ enderecoEntrega.uf }} ·
    {{ formatarCep(enderecoEntrega.cep) }}
  </p>

  <div *ngIf="loading()" class="mt-8 space-y-3">
    <div class="h-16 animate-pulse rounded bg-graphite-100"></div>
    <div class="h-16 animate-pulse rounded bg-graphite-100"></div>
    <div class="h-16 animate-pulse rounded bg-graphite-100"></div>
  </div>

  <p *ngIf="erroCalculo()" class="form-error mt-6">{{ erroCalculo() }}</p>

  <div *ngIf="!loading() && !erroCalculo()" class="mt-6 space-y-3">
    <label *ngFor="let opcaoDeFrete of opcoesDisponiveis()" class="block">
      <input type="radio" name="opcaoFrete" class="peer sr-only"
             [value]="opcaoDeFrete.codigo"
             [checked]="codigoOpcaoSelecionada() === opcaoDeFrete.codigo"
             (change)="selecionarOpcao(opcaoDeFrete)" />
      <div class="radio-card">
        <div class="flex justify-between">
          <div>
            <p class="font-medium text-graphite-900">{{ opcaoDeFrete.servico }}</p>
            <p class="text-xs text-graphite-500">
              {{ opcaoDeFrete.transportadora }} · entrega em até {{ opcaoDeFrete.prazoDias }} dia(s) úteis
            </p>
          </div>
          <p class="font-semibold text-graphite-900">
            {{ opcaoDeFrete.valor | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}
          </p>
        </div>
      </div>
    </label>
  </div>

  <div class="mt-8 flex justify-between">
    <button class="btn-ghost" type="button" (click)="voltarParaEndereco()">← Voltar para endereço</button>
    <button class="btn-primary"
            type="button"
            [disabled]="!codigoOpcaoSelecionada()"
            (click)="prosseguirParaPagamento()">
      Continuar para pagamento
    </button>
  </div>
</section>
```

- [ ] **Step 2: Criar `shipping.page.scss`** (vazio)

```scss
```

- [ ] **Step 3: Criar `shipping.page.ts`**

Path: `src/app/modules/checkout/pages/shipping/shipping.page.ts`

```ts
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

import { AddressService } from '@modules/customer/services/address.service';
import { CartService } from '@modules/cart/services/cart.service';
import { CheckoutStateService } from '@modules/checkout/services/checkout-state.service';
import { ShippingService } from '@shared/services/shipping.service';
import { EnderecoResponse } from '@modules/customer/models/address';
import { OpcaoFrete } from '@shared/models/shipping';
import { StepperComponent } from '@modules/checkout/components/stepper/stepper.component';

@Component({
  selector: 'app-checkout-shipping-page',
  standalone: true,
  imports: [CommonModule, StepperComponent],
  templateUrl: './shipping.page.html',
  styleUrls: ['./shipping.page.scss'],
})
export class CheckoutShippingPage implements OnInit {
  private readonly addressService = inject(AddressService);
  private readonly cartService = inject(CartService);
  private readonly shippingService = inject(ShippingService);
  private readonly checkoutState = inject(CheckoutStateService);
  private readonly router = inject(Router);

  readonly passosCheckout = [
    { label: 'Endereço' },
    { label: 'Frete' },
    { label: 'Pagamento' },
    { label: 'Revisão' },
  ];

  readonly enderecoSelecionado = signal<EnderecoResponse | null>(null);
  readonly opcoesDisponiveis = signal<OpcaoFrete[]>([]);
  readonly codigoOpcaoSelecionada = signal<string | null>(null);
  readonly loading = signal(true);
  readonly erroCalculo = signal<string | null>(null);

  ngOnInit(): void {
    const estadoSnapshot = this.checkoutState.state();
    if (estadoSnapshot.opcaoFreteCodigo) {
      this.codigoOpcaoSelecionada.set(estadoSnapshot.opcaoFreteCodigo);
    }

    this.addressService.list().subscribe({
      next: (listaEnderecos) => {
        const enderecoEscolhido = listaEnderecos.find((endereco) => endereco.id === estadoSnapshot.enderecoEntregaId);
        if (!enderecoEscolhido) {
          this.router.navigate(['/checkout/endereco']);
          return;
        }
        this.enderecoSelecionado.set(enderecoEscolhido);
        this.calcularOpcoesFrete(enderecoEscolhido);
      },
      error: () => this.router.navigate(['/checkout/endereco']),
    });
  }

  selecionarOpcao(opcaoFrete: OpcaoFrete): void {
    this.codigoOpcaoSelecionada.set(opcaoFrete.codigo);
  }

  prosseguirParaPagamento(): void {
    const opcaoEscolhida = this.opcoesDisponiveis().find(
      (opcao) => opcao.codigo === this.codigoOpcaoSelecionada(),
    );
    if (!opcaoEscolhida) return;
    this.checkoutState.patch({
      opcaoFreteCodigo: opcaoEscolhida.codigo,
      opcaoFreteSnapshot: opcaoEscolhida,
    });
    this.router.navigate(['/checkout/pagamento']);
  }

  voltarParaEndereco(): void {
    this.router.navigate(['/checkout/endereco']);
  }

  formatarCep(cep: string): string {
    const digitos = cep.replace(/\D/g, '');
    return digitos.length === 8 ? `${digitos.slice(0, 5)}-${digitos.slice(5)}` : cep;
  }

  private calcularOpcoesFrete(enderecoEntrega: EnderecoResponse): void {
    const itensCarrinho = (this.cartService.cart()?.items ?? []).map((itemDoCarrinho) => ({
      sku: itemDoCarrinho.sku,
      qty: itemDoCarrinho.qty,
    }));
    if (!itensCarrinho.length) {
      this.router.navigate(['/carrinho']);
      return;
    }
    this.shippingService.calculate({
      cepDestino: enderecoEntrega.cep,
      itens: itensCarrinho,
    }).subscribe({
      next: (listaOpcoes) => {
        this.loading.set(false);
        this.opcoesDisponiveis.set(listaOpcoes);
        if (!this.codigoOpcaoSelecionada() && listaOpcoes.length) {
          const opcaoMaisBarata = listaOpcoes.reduce(
            (atual, candidata) => (atual.valor <= candidata.valor ? atual : candidata),
          );
          this.codigoOpcaoSelecionada.set(opcaoMaisBarata.codigo);
        }
      },
      error: (httpError: HttpErrorResponse) => {
        this.loading.set(false);
        this.erroCalculo.set(httpError.error?.detail ?? 'Não foi possível calcular o frete.');
      },
    });
  }
}
```

- [ ] **Step 4: Registrar rota**

```ts
{
  path: 'checkout/frete',
  canActivate: [authGuard, checkoutStepGuard(['enderecoEntregaId', 'enderecoCobrancaId'])],
  loadComponent: () =>
    import('@modules/checkout/pages/shipping/shipping.page')
      .then((moduleShipping) => moduleShipping.CheckoutShippingPage),
},
```

- [ ] **Step 5: Typecheck + commit**

```bash
cd /home/ali/projects/pet-hub/frontend/storefront && npx ng build --configuration development
cd /home/ali/projects/pet-hub && git add \
  frontend/storefront/src/app/modules/checkout/pages/shipping \
  frontend/storefront/src/app/app.routes.ts
git commit -m "feat(storefront): checkout step 2 — shipping options with auto-cheapest"
```

---

## Task 12: Wizard step 3 — `/checkout/pagamento`

**Files:**
- Create: `src/app/modules/checkout/pages/payment/payment.page.{ts,html,scss}`
- Modify: `src/app/app.routes.ts`

> **AppSec crítica:** o PAN/CVV transita SÓ na request de tokenize. Não persistir em sessionStorage. Limpar campos após sucesso. Atributos `autocomplete="cc-*"` para que o gerenciador do navegador cuide do preenchimento.

- [ ] **Step 1: Criar `payment.page.html`**

Path: `src/app/modules/checkout/pages/payment/payment.page.html`

```html
<section class="mx-auto max-w-page px-6 py-12">
  <app-stepper [steps]="passosCheckout" [active]="3" />
  <h1 class="mt-8 text-2xl text-graphite-900">Pagamento</h1>

  <div class="mt-6 flex gap-2 border-b border-graphite-200">
    <button *ngFor="let aba of abasPagamento"
            type="button"
            class="px-4 py-2 text-sm"
            [class.text-coral-500]="abaAtiva() === aba.valor"
            [class.font-medium]="abaAtiva() === aba.valor"
            [class.border-b-2]="abaAtiva() === aba.valor"
            [class.border-coral-500]="abaAtiva() === aba.valor"
            [class.text-graphite-500]="abaAtiva() !== aba.valor"
            (click)="trocarAba(aba.valor)">
      {{ aba.label }}
    </button>
  </div>

  <!-- Aba CARTAO -->
  <div *ngIf="abaAtiva() === 'CARTAO'" class="mt-6 space-y-6">
    <div *ngIf="cartoesSalvos().length" class="space-y-3">
      <label *ngFor="let cartaoSalvo of cartoesSalvos()" class="block">
        <input type="radio" name="cartaoSalvo" class="peer sr-only"
               [value]="cartaoSalvo.id"
               [checked]="cartaoSelecionadoId() === cartaoSalvo.id"
               (change)="selecionarCartao(cartaoSalvo.id)" />
        <div class="radio-card">
          <div class="flex justify-between">
            <p class="font-medium text-graphite-900">
              {{ cartaoSalvo.bandeira }} ···· {{ cartaoSalvo.ultimosQuatroDigitos }}
              <span *ngIf="cartaoSalvo.padrao" class="ml-2 text-xs text-coral-500">padrão</span>
            </p>
            <p class="text-xs text-graphite-500">
              Válido até {{ cartaoSalvo.validadeMes }}/{{ cartaoSalvo.validadeAno }}
            </p>
          </div>
          <p class="mt-1 text-sm text-graphite-700">{{ cartaoSalvo.nomeImpresso }}</p>
        </div>
      </label>
    </div>

    <div *ngIf="cartaoSelecionadoId()" class="surface space-y-3">
      <label class="label" for="quantidadeParcelas">Parcelas</label>
      <select id="quantidadeParcelas" class="input"
              [value]="quantidadeParcelas()"
              (change)="atualizarParcelas($any($event.target).value)">
        <option *ngFor="let opcaoParcelas of OPCOES_PARCELAS" [value]="opcaoParcelas">{{ opcaoParcelas }}x sem juros</option>
      </select>
    </div>

    <button *ngIf="cartoesSalvos().length && !mostrandoFormularioCartao()"
            type="button"
            class="btn-ghost"
            (click)="mostrandoFormularioCartao.set(true)">
      + Adicionar novo cartão
    </button>

    <form *ngIf="mostrandoFormularioCartao() || cartoesSalvos().length === 0"
          [formGroup]="formularioCartao" (ngSubmit)="tokenizarECadastrarCartao()"
          class="surface space-y-4" autocomplete="on">
      <h2 class="text-base text-graphite-900">Novo cartão</h2>
      <div class="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <div class="sm:col-span-3">
          <label class="label" for="numeroCartao">Número</label>
          <input id="numeroCartao" class="input"
                 formControlName="numero"
                 inputmode="numeric"
                 autocomplete="cc-number"
                 maxlength="19"
                 placeholder="9999 9999 9999 9999" />
          <p *ngIf="formularioCartao.controls.numero.touched && formularioCartao.controls.numero.invalid"
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
      <p *ngIf="erroCartao()" class="form-error">{{ erroCartao() }}</p>
      <div class="flex gap-2">
        <button type="submit" class="btn-primary"
                [disabled]="formularioCartao.invalid || tokenizandoCartao()">
          <span *ngIf="!tokenizandoCartao()">Salvar cartão</span>
          <span *ngIf="tokenizandoCartao()">Validando…</span>
        </button>
        <button *ngIf="cartoesSalvos().length"
                type="button"
                class="btn-ghost"
                (click)="mostrandoFormularioCartao.set(false)">Cancelar</button>
      </div>
    </form>
  </div>

  <!-- Aba PIX -->
  <div *ngIf="abaAtiva() === 'PIX'" class="mt-6 surface">
    <p class="text-graphite-700">Você verá o QR Code após confirmar o pedido.</p>
    <p class="mt-1 text-sm text-graphite-500">Aprovação imediata; o pedido entra em separação ao receber a confirmação.</p>
  </div>

  <!-- Aba BOLETO -->
  <div *ngIf="abaAtiva() === 'BOLETO'" class="mt-6 surface">
    <p class="text-graphite-700">O boleto será gerado e enviado por e-mail após confirmar o pedido.</p>
    <p class="mt-1 text-sm text-graphite-500">Vencimento em 3 dias úteis.</p>
  </div>

  <div class="mt-8 flex justify-between">
    <button class="btn-ghost" type="button" (click)="voltarParaFrete()">← Voltar para frete</button>
    <button class="btn-primary"
            type="button"
            [disabled]="!podeProsseguir()"
            (click)="prosseguirParaRevisao()">
      Continuar para revisão
    </button>
  </div>
</section>
```

- [ ] **Step 2: Criar `payment.page.scss`** (vazio)

```scss
```

- [ ] **Step 3: Criar `payment.page.ts`**

Path: `src/app/modules/checkout/pages/payment/payment.page.ts`

```ts
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { CheckoutStateService } from '@modules/checkout/services/checkout-state.service';
import { PaymentMethodService } from '@modules/customer/services/payment-method.service';
import {
  CreateFormaPagamentoRequest,
  FormaPagamentoResponse,
  TipoPagamento,
} from '@modules/customer/models/payment-method';
import { StepperComponent } from '@modules/checkout/components/stepper/stepper.component';

type AbaPagamento = 'CARTAO' | 'PIX' | 'BOLETO';

@Component({
  selector: 'app-checkout-payment-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, StepperComponent],
  templateUrl: './payment.page.html',
  styleUrls: ['./payment.page.scss'],
})
export class CheckoutPaymentPage implements OnInit {
  private readonly paymentMethodService = inject(PaymentMethodService);
  private readonly checkoutState = inject(CheckoutStateService);
  private readonly router = inject(Router);
  private readonly formBuilder = inject(FormBuilder);

  readonly passosCheckout = [
    { label: 'Endereço' },
    { label: 'Frete' },
    { label: 'Pagamento' },
    { label: 'Revisão' },
  ];
  readonly abasPagamento: { valor: AbaPagamento; label: string }[] = [
    { valor: 'CARTAO', label: 'Cartão' },
    { valor: 'PIX', label: 'PIX' },
    { valor: 'BOLETO', label: 'Boleto' },
  ];
  readonly OPCOES_PARCELAS = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12];

  readonly abaAtiva = signal<AbaPagamento>('CARTAO');
  readonly metodosCadastrados = signal<FormaPagamentoResponse[]>([]);
  readonly cartoesSalvos = computed(() =>
    this.metodosCadastrados().filter((metodoCadastrado) => metodoCadastrado.tipo === 'CARTAO_CREDITO'),
  );
  readonly cartaoSelecionadoId = signal<number | null>(null);
  readonly quantidadeParcelas = signal(1);
  readonly mostrandoFormularioCartao = signal(false);
  readonly tokenizandoCartao = signal(false);
  readonly erroCartao = signal<string | null>(null);

  readonly formularioCartao = this.formBuilder.nonNullable.group({
    numero: ['', [Validators.required, Validators.pattern(/^\d{13,19}$/)]],
    nomeImpresso: ['', [Validators.required, Validators.maxLength(100)]],
    validadeMes: ['', [Validators.required, Validators.pattern(/^(0?[1-9]|1[0-2])$/)]],
    validadeAno: ['', [Validators.required, Validators.pattern(/^20\d{2}$/)]],
    cvv: ['', [Validators.required, Validators.pattern(/^\d{3,4}$/)]],
  });

  readonly podeProsseguir = computed(() => {
    if (this.abaAtiva() === 'CARTAO') return this.cartaoSelecionadoId() !== null;
    return true;
  });

  ngOnInit(): void {
    const estadoSnapshot = this.checkoutState.state();
    this.quantidadeParcelas.set(estadoSnapshot.parcelas || 1);
    this.carregarMetodos(estadoSnapshot.formaPagamentoId);
  }

  trocarAba(novaAba: AbaPagamento): void {
    this.abaAtiva.set(novaAba);
  }

  selecionarCartao(idCartao: number): void {
    this.cartaoSelecionadoId.set(idCartao);
  }

  atualizarParcelas(valorBruto: string): void {
    const valorNumerico = Number(valorBruto);
    if (Number.isInteger(valorNumerico) && valorNumerico >= 1 && valorNumerico <= 12) {
      this.quantidadeParcelas.set(valorNumerico);
    }
  }

  tokenizarECadastrarCartao(): void {
    if (this.formularioCartao.invalid) {
      this.formularioCartao.markAllAsTouched();
      return;
    }
    const valoresCartao = this.formularioCartao.getRawValue();
    this.tokenizandoCartao.set(true);
    this.erroCartao.set(null);
    this.paymentMethodService.tokenize({
      numero: valoresCartao.numero.replace(/\D/g, ''),
      cvv: valoresCartao.cvv,
      nomeImpresso: valoresCartao.nomeImpresso,
      validadeMes: Number(valoresCartao.validadeMes),
      validadeAno: Number(valoresCartao.validadeAno),
    }).subscribe({
      next: (respostaTokenize) => {
        // Limpa os campos sensíveis imediatamente após tokenizar
        this.formularioCartao.patchValue({ numero: '', cvv: '' });
        const requestCadastrarMetodo: CreateFormaPagamentoRequest = {
          tipo: 'CARTAO_CREDITO',
          gatewayToken: respostaTokenize.token,
          bandeira: respostaTokenize.bandeira,
          ultimosQuatroDigitos: respostaTokenize.ultimosQuatroDigitos,
          nomeImpresso: valoresCartao.nomeImpresso,
          validadeMes: Number(valoresCartao.validadeMes),
          validadeAno: Number(valoresCartao.validadeAno),
          padrao: this.cartoesSalvos().length === 0,
        };
        this.paymentMethodService.create(requestCadastrarMetodo).subscribe({
          next: (metodoCriado) => {
            this.tokenizandoCartao.set(false);
            this.metodosCadastrados.update((listaMetodos) => [...listaMetodos, metodoCriado]);
            this.cartaoSelecionadoId.set(metodoCriado.id);
            this.mostrandoFormularioCartao.set(false);
            this.formularioCartao.reset();
          },
          error: (httpError: HttpErrorResponse) => {
            this.tokenizandoCartao.set(false);
            this.erroCartao.set(httpError.error?.detail ?? 'Não foi possível salvar o cartão.');
          },
        });
      },
      error: (httpError: HttpErrorResponse) => {
        this.tokenizandoCartao.set(false);
        this.erroCartao.set(httpError.error?.detail ?? 'Número do cartão inválido.');
      },
    });
  }

  prosseguirParaRevisao(): void {
    if (this.abaAtiva() === 'CARTAO') {
      this.checkoutState.patch({
        formaPagamentoId: this.cartaoSelecionadoId(),
        parcelas: this.quantidadeParcelas(),
      });
      this.router.navigate(['/checkout/revisao']);
      return;
    }
    const tipoEscolhido: TipoPagamento = this.abaAtiva() === 'PIX' ? 'PIX' : 'BOLETO';
    const metodoExistente = this.metodosCadastrados().find((metodo) => metodo.tipo === tipoEscolhido);
    if (metodoExistente) {
      this.checkoutState.patch({ formaPagamentoId: metodoExistente.id, parcelas: 1 });
      this.router.navigate(['/checkout/revisao']);
      return;
    }
    this.paymentMethodService.create({ tipo: tipoEscolhido }).subscribe({
      next: (metodoCriado) => {
        this.metodosCadastrados.update((listaMetodos) => [...listaMetodos, metodoCriado]);
        this.checkoutState.patch({ formaPagamentoId: metodoCriado.id, parcelas: 1 });
        this.router.navigate(['/checkout/revisao']);
      },
      error: (httpError: HttpErrorResponse) => {
        this.erroCartao.set(httpError.error?.detail ?? 'Não foi possível registrar o método.');
      },
    });
  }

  voltarParaFrete(): void {
    this.router.navigate(['/checkout/frete']);
  }

  private carregarMetodos(idMetodoPreSelecionado: number | null): void {
    this.paymentMethodService.list().subscribe({
      next: (listaMetodos) => {
        this.metodosCadastrados.set(listaMetodos);
        if (idMetodoPreSelecionado) {
          const metodoPrevio = listaMetodos.find((metodo) => metodo.id === idMetodoPreSelecionado);
          if (metodoPrevio) {
            if (metodoPrevio.tipo === 'CARTAO_CREDITO') {
              this.abaAtiva.set('CARTAO');
              this.cartaoSelecionadoId.set(metodoPrevio.id);
            } else if (metodoPrevio.tipo === 'PIX') {
              this.abaAtiva.set('PIX');
            } else if (metodoPrevio.tipo === 'BOLETO') {
              this.abaAtiva.set('BOLETO');
            }
            return;
          }
        }
        const cartaoPadrao = listaMetodos.find((metodo) => metodo.tipo === 'CARTAO_CREDITO');
        if (cartaoPadrao) {
          this.cartaoSelecionadoId.set(cartaoPadrao.id);
        } else {
          this.mostrandoFormularioCartao.set(true);
        }
      },
      error: () => this.mostrandoFormularioCartao.set(true),
    });
  }
}
```

- [ ] **Step 4: Registrar rota**

```ts
{
  path: 'checkout/pagamento',
  canActivate: [authGuard, checkoutStepGuard(['opcaoFreteCodigo'])],
  loadComponent: () =>
    import('@modules/checkout/pages/payment/payment.page')
      .then((modulePayment) => modulePayment.CheckoutPaymentPage),
},
```

- [ ] **Step 5: Typecheck + Smoke AppSec**

```bash
cd /home/ali/projects/pet-hub/frontend/storefront && npx ng build --configuration development
```

Smoke manual com DevTools:
- Cartão `4111 1111 1111 1111` (Luhn válido) → request `POST /api/v1/customers/payment-methods/tokenize` carrega PAN; request seguinte (`POST /me/payment-methods`) carrega só `gatewayToken`.
- `sessionStorage['pethub:checkout:v1']` NÃO contém PAN/CVV.

- [ ] **Step 6: Commit**

```bash
cd /home/ali/projects/pet-hub && git add \
  frontend/storefront/src/app/modules/checkout/pages/payment \
  frontend/storefront/src/app/app.routes.ts
git commit -m "feat(storefront): checkout step 3 — payment tabs + card tokenization"
```

---

## Task 13: Wizard step 4 — `/checkout/revisao` (preview + idempotent place-order)

**Files:**
- Create: `src/app/modules/checkout/pages/review/review.page.{ts,html,scss}`
- Modify: `src/app/app.routes.ts`

- [ ] **Step 1: Criar `review.page.html`**

Path: `src/app/modules/checkout/pages/review/review.page.html`

```html
<section class="mx-auto max-w-page px-6 py-12">
  <app-stepper [steps]="passosCheckout" [active]="4" />
  <h1 class="mt-8 text-2xl text-graphite-900">Revisão do pedido</h1>

  <div *ngIf="loading()" class="mt-8 space-y-3">
    <div class="h-24 animate-pulse rounded bg-graphite-100"></div>
    <div class="h-24 animate-pulse rounded bg-graphite-100"></div>
    <div class="h-24 animate-pulse rounded bg-graphite-100"></div>
  </div>

  <p *ngIf="erroCarregamento()" class="form-error mt-6">{{ erroCarregamento() }}</p>

  <ng-container *ngIf="resumoPreview() as resumoPedido">
    <div class="mt-8 grid gap-8 lg:grid-cols-[1fr_360px]">
      <div class="space-y-6">
        <div class="surface">
          <div class="flex justify-between">
            <h2 class="text-base text-graphite-900">Endereço de entrega</h2>
            <a routerLink="/checkout/endereco" class="text-sm text-coral-500">Editar</a>
          </div>
          <p *ngIf="enderecoEscolhido() as enderecoEntrega"
             class="mt-2 text-sm text-graphite-700">
            {{ enderecoEntrega.apelido }} — {{ enderecoEntrega.logradouro }}<ng-container *ngIf="enderecoEntrega.numero">, {{ enderecoEntrega.numero }}</ng-container><br>
            {{ enderecoEntrega.bairro }}, {{ enderecoEntrega.cidade }}/{{ enderecoEntrega.uf }} ·
            {{ formatarCep(enderecoEntrega.cep) }}
          </p>
        </div>
        <div class="surface">
          <div class="flex justify-between">
            <h2 class="text-base text-graphite-900">Frete</h2>
            <a routerLink="/checkout/frete" class="text-sm text-coral-500">Editar</a>
          </div>
          <p class="mt-2 text-sm text-graphite-700">
            {{ resumoPedido.freteEscolhido.servico }} · {{ resumoPedido.freteEscolhido.transportadora }} ·
            entrega em até {{ resumoPedido.freteEscolhido.prazoDias }} dia(s) úteis
          </p>
        </div>
        <div class="surface">
          <div class="flex justify-between">
            <h2 class="text-base text-graphite-900">Pagamento</h2>
            <a routerLink="/checkout/pagamento" class="text-sm text-coral-500">Editar</a>
          </div>
          <p *ngIf="metodoPagamentoEscolhido() as metodoPagamento"
             class="mt-2 text-sm text-graphite-700">
            <ng-container *ngIf="metodoPagamento.tipo === 'CARTAO_CREDITO'">
              {{ metodoPagamento.bandeira }} ···· {{ metodoPagamento.ultimosQuatroDigitos }} em {{ checkoutState.state().parcelas }}x
            </ng-container>
            <ng-container *ngIf="metodoPagamento.tipo === 'PIX'">PIX (QR Code após confirmação)</ng-container>
            <ng-container *ngIf="metodoPagamento.tipo === 'BOLETO'">Boleto (3 dias úteis)</ng-container>
          </p>
        </div>
        <div class="surface">
          <h2 class="text-base text-graphite-900">Itens ({{ resumoPedido.itens.length }})</h2>
          <ul class="mt-2 divide-y divide-graphite-200">
            <li *ngFor="let itemPedido of resumoPedido.itens" class="flex justify-between py-2 text-sm">
              <span class="text-graphite-700">{{ itemPedido.qty }}× {{ itemPedido.nome }}</span>
              <span>{{ itemPedido.subtotal | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</span>
            </li>
          </ul>
        </div>
      </div>

      <aside class="surface space-y-3">
        <h2 class="text-lg text-graphite-900">Total</h2>
        <div class="space-y-1.5 text-sm">
          <div class="flex justify-between">
            <span class="text-graphite-500">Subtotal</span>
            <span>{{ resumoPedido.subtotal | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</span>
          </div>
          <div *ngIf="resumoPedido.descontoPromocoes > 0" class="flex justify-between text-leaf-700">
            <span>Promoções</span>
            <span>− {{ resumoPedido.descontoPromocoes | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</span>
          </div>
          <div *ngIf="resumoPedido.descontoCupom > 0" class="flex justify-between text-leaf-700">
            <span>Cupom {{ resumoPedido.cupomCodigo }}</span>
            <span>− {{ resumoPedido.descontoCupom | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</span>
          </div>
          <div *ngIf="resumoPedido.impostosCalculados.valorImpostos > 0" class="flex justify-between">
            <span class="text-graphite-500">Impostos</span>
            <span>{{ resumoPedido.impostosCalculados.valorImpostos | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</span>
          </div>
          <div class="flex justify-between">
            <span class="text-graphite-500">Frete</span>
            <span>{{ resumoPedido.freteEscolhido.valor | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</span>
          </div>
        </div>
        <hr class="border-graphite-200">
        <div class="flex justify-between font-semibold">
          <span>Total</span>
          <span class="text-lg">{{ resumoPedido.valorTotal | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</span>
        </div>
        <p *ngIf="erroSubmissao()" class="form-error">{{ erroSubmissao() }}</p>
        <button class="btn-primary w-full"
                type="button"
                [disabled]="submetendoPedido()"
                (click)="finalizarCompra()">
          <span *ngIf="!submetendoPedido()">Finalizar compra · {{ resumoPedido.valorTotal | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</span>
          <span *ngIf="submetendoPedido()">Processando…</span>
        </button>
      </aside>
    </div>
  </ng-container>

  <!-- Dialog "Estoque mudou" -->
  <div *ngIf="dialogEstoque()"
       class="fixed inset-0 z-50 flex items-center justify-center bg-graphite-900/60 px-4">
    <div class="w-full max-w-md rounded-lg bg-graphite-0 p-6">
      <h2 class="text-lg text-graphite-900">Estoque mudou</h2>
      <p class="mt-2 text-sm text-graphite-700">
        Algumas mudanças aconteceram no seu carrinho enquanto você revisava o pedido.
      </p>
      <p class="mt-2 rounded bg-graphite-100 px-3 py-2 text-sm font-mono text-graphite-700">
        {{ dialogEstoque()!.mensagem }}
      </p>
      <div class="mt-4 flex justify-end gap-2">
        <button class="btn-primary" type="button" (click)="ajustarCarrinho()">Ajustar carrinho</button>
      </div>
    </div>
  </div>
</section>
```

- [ ] **Step 2: Criar `review.page.scss`** (vazio)

```scss
```

- [ ] **Step 3: Criar `review.page.ts`**

Path: `src/app/modules/checkout/pages/review/review.page.ts`

```ts
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

import { AddressService } from '@modules/customer/services/address.service';
import { CartService } from '@modules/cart/services/cart.service';
import { CheckoutService } from '@modules/checkout/services/checkout.service';
import { CheckoutStateService } from '@modules/checkout/services/checkout-state.service';
import { PaymentMethodService } from '@modules/customer/services/payment-method.service';
import { CheckoutPreviewResponse } from '@modules/checkout/models/checkout';
import { EnderecoResponse } from '@modules/customer/models/address';
import { FormaPagamentoResponse } from '@modules/customer/models/payment-method';
import { StepperComponent } from '@modules/checkout/components/stepper/stepper.component';

@Component({
  selector: 'app-checkout-review-page',
  standalone: true,
  imports: [CommonModule, RouterLink, StepperComponent],
  templateUrl: './review.page.html',
  styleUrls: ['./review.page.scss'],
})
export class CheckoutReviewPage implements OnInit {
  private readonly addressService = inject(AddressService);
  private readonly paymentMethodService = inject(PaymentMethodService);
  private readonly checkoutService = inject(CheckoutService);
  private readonly cartService = inject(CartService);
  readonly checkoutState = inject(CheckoutStateService);
  private readonly router = inject(Router);

  readonly passosCheckout = [
    { label: 'Endereço' },
    { label: 'Frete' },
    { label: 'Pagamento' },
    { label: 'Revisão' },
  ];

  readonly resumoPreview = signal<CheckoutPreviewResponse | null>(null);
  readonly enderecoEscolhido = signal<EnderecoResponse | null>(null);
  readonly metodoPagamentoEscolhido = signal<FormaPagamentoResponse | null>(null);
  readonly loading = signal(true);
  readonly erroCarregamento = signal<string | null>(null);
  readonly submetendoPedido = signal(false);
  readonly erroSubmissao = signal<string | null>(null);
  readonly dialogEstoque = signal<{ mensagem: string } | null>(null);

  ngOnInit(): void {
    const estadoSnapshot = this.checkoutState.state();
    if (!estadoSnapshot.enderecoEntregaId || !estadoSnapshot.opcaoFreteCodigo || !estadoSnapshot.formaPagamentoId) {
      this.router.navigate(['/checkout/endereco']);
      return;
    }

    this.addressService.list().subscribe((listaEnderecos) => {
      this.enderecoEscolhido.set(
        listaEnderecos.find((endereco) => endereco.id === estadoSnapshot.enderecoEntregaId) ?? null,
      );
    });
    this.paymentMethodService.list().subscribe((listaMetodos) => {
      this.metodoPagamentoEscolhido.set(
        listaMetodos.find((metodo) => metodo.id === estadoSnapshot.formaPagamentoId) ?? null,
      );
    });

    this.checkoutService.preview({
      enderecoEntregaId: estadoSnapshot.enderecoEntregaId,
      opcaoFreteCodigo: estadoSnapshot.opcaoFreteCodigo,
      cupom: this.cartService.cart()?.cupom?.codigo,
    }).subscribe({
      next: (respostaPreview) => {
        this.resumoPreview.set(respostaPreview);
        this.loading.set(false);
      },
      error: (httpError: HttpErrorResponse) => {
        this.loading.set(false);
        if (this.ehErroDeEstoque(httpError)) {
          this.dialogEstoque.set({ mensagem: httpError.error.detail });
        } else {
          this.erroCarregamento.set(httpError.error?.detail ?? 'Não foi possível calcular o pedido.');
        }
      },
    });
  }

  async finalizarCompra(): Promise<void> {
    const estadoSnapshot = this.checkoutState.state();
    if (!estadoSnapshot.enderecoEntregaId || !estadoSnapshot.enderecoCobrancaId
        || !estadoSnapshot.opcaoFreteCodigo || !estadoSnapshot.formaPagamentoId) {
      this.router.navigate(['/checkout/endereco']);
      return;
    }
    const chaveIdempotencia = this.checkoutState.ensureIdempotencyKey();
    this.submetendoPedido.set(true);
    this.erroSubmissao.set(null);
    try {
      const respostaPedido = await firstValueFrom(this.checkoutService.placeOrder({
        enderecoEntregaId: estadoSnapshot.enderecoEntregaId,
        enderecoCobrancaId: estadoSnapshot.enderecoCobrancaId,
        opcaoFreteCodigo: estadoSnapshot.opcaoFreteCodigo,
        formaPagamentoId: estadoSnapshot.formaPagamentoId,
        cupom: this.cartService.cart()?.cupom?.codigo,
        parcelas: estadoSnapshot.parcelas,
        idempotencyKey: chaveIdempotencia,
      }));
      if (respostaPedido.status === 'APPROVED') {
        this.cartService.clearLocal();
        this.checkoutState.clear();
        this.router.navigate(['/checkout/sucesso', respostaPedido.referenciaPedido], {
          state: { snapshot: respostaPedido },
        });
      } else {
        // REJECTED — gera key nova na próxima tentativa
        this.checkoutState.patch({ idempotencyKey: null });
        this.submetendoPedido.set(false);
        this.erroSubmissao.set('Pagamento recusado. Troque a forma de pagamento ou tente novamente.');
      }
    } catch (erroSubmissao) {
      this.submetendoPedido.set(false);
      const httpError = erroSubmissao as HttpErrorResponse;
      if (this.ehErroDeEstoque(httpError)) {
        this.dialogEstoque.set({ mensagem: httpError.error.detail });
        return;
      }
      this.erroSubmissao.set(httpError.error?.detail
        ?? 'Falha ao confirmar. Verifique sua conexão e tente novamente.');
    }
  }

  ajustarCarrinho(): void {
    this.dialogEstoque.set(null);
    this.router.navigate(['/carrinho']);
  }

  formatarCep(cep: string): string {
    const digitos = cep.replace(/\D/g, '');
    return digitos.length === 8 ? `${digitos.slice(0, 5)}-${digitos.slice(5)}` : cep;
  }

  /**
   * Backend devolve 422 com ProblemDetail.detail = "Estoque insuficiente para: ..."
   * (CheckoutService.validarEstoqueDisponivel) ou "Estoque insuficiente para SKU X..."
   * (InventoryService.reservar). Não há campo estruturado — detecta pelo prefixo.
   */
  private ehErroDeEstoque(httpError: HttpErrorResponse): boolean {
    if (httpError.status !== 422) return false;
    const detalheBackend: string | undefined = httpError.error?.detail;
    return typeof detalheBackend === 'string' && detalheBackend.toLowerCase().includes('estoque insufic');
  }
}
```

- [ ] **Step 4: Registrar rota**

```ts
{
  path: 'checkout/revisao',
  canActivate: [authGuard, checkoutStepGuard(['formaPagamentoId'])],
  loadComponent: () =>
    import('@modules/checkout/pages/review/review.page')
      .then((moduleReview) => moduleReview.CheckoutReviewPage),
},
```

- [ ] **Step 5: Typecheck + commit**

```bash
cd /home/ali/projects/pet-hub/frontend/storefront && npx ng build --configuration development
cd /home/ali/projects/pet-hub && git add \
  frontend/storefront/src/app/modules/checkout/pages/review \
  frontend/storefront/src/app/app.routes.ts
git commit -m "feat(storefront): checkout step 4 — review + idempotent place-order + stock dialog"
```

---

## Task 14: Wizard terminal — `/checkout/sucesso/:numero` (confetti)

**Files:**
- Create: `src/app/modules/checkout/pages/success/success.page.{ts,html,scss}`
- Modify: `src/app/app.routes.ts`

- [ ] **Step 1: Criar `success.page.html`**

```html
<section class="mx-auto max-w-page px-6 py-16 text-center">
  <div class="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-leaf-100 text-3xl text-leaf-700">
    ✓
  </div>
  <h1 class="mt-6 text-3xl text-graphite-900">Pedido confirmado!</h1>
  <p class="mt-2 text-graphite-500">
    Número do pedido: <span class="font-mono text-graphite-900">{{ numeroPedido() }}</span>
  </p>

  <div *ngIf="snapshotPedido() as resumoFinalPedido"
       class="mx-auto mt-8 max-w-md surface text-left space-y-2 text-sm">
    <div class="flex justify-between">
      <span class="text-graphite-500">Total pago</span>
      <span class="font-semibold">{{ resumoFinalPedido.valorTotal | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</span>
    </div>
    <div class="flex justify-between">
      <span class="text-graphite-500">Método</span>
      <span>{{ humanizarMetodo(resumoFinalPedido.metodo) }}</span>
    </div>
    <div *ngIf="resumoFinalPedido.qrCode" class="flex justify-between">
      <span class="text-graphite-500">PIX QR Code</span>
      <span class="font-mono text-xs break-all">{{ resumoFinalPedido.qrCode }}</span>
    </div>
    <div *ngIf="resumoFinalPedido.boletoUrl" class="flex justify-between">
      <span class="text-graphite-500">Boleto</span>
      <a [href]="resumoFinalPedido.boletoUrl" target="_blank" rel="noopener"
         class="text-coral-500">Abrir PDF</a>
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
```

- [ ] **Step 2: Criar `success.page.scss`** (vazio)

```scss
```

- [ ] **Step 3: Criar `success.page.ts`**

```ts
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import confetti from 'canvas-confetti';

import { PlaceOrderResponse } from '@modules/checkout/models/checkout';

@Component({
  selector: 'app-checkout-success-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './success.page.html',
  styleUrls: ['./success.page.scss'],
})
export class CheckoutSuccessPage implements OnInit {
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly numeroPedido = signal<string>('');
  readonly snapshotPedido = signal<PlaceOrderResponse | null>(null);

  ngOnInit(): void {
    this.numeroPedido.set(this.activatedRoute.snapshot.paramMap.get('numero') ?? '');
    const navigationExtras = this.router.getCurrentNavigation();
    const snapshotRecebido =
      (navigationExtras?.extras.state as { snapshot?: PlaceOrderResponse } | undefined)?.snapshot
      ?? (history.state?.snapshot as PlaceOrderResponse | undefined);
    if (snapshotRecebido) this.snapshotPedido.set(snapshotRecebido);
    this.dispararConfete();
  }

  humanizarMetodo(metodoPagamento: string): string {
    switch (metodoPagamento) {
      case 'CARTAO_CREDITO': return 'Cartão de crédito';
      case 'CARTAO_DEBITO': return 'Cartão de débito';
      case 'PIX': return 'PIX';
      case 'BOLETO': return 'Boleto';
      default: return metodoPagamento;
    }
  }

  private dispararConfete(): void {
    if (typeof window === 'undefined') return;
    confetti({
      particleCount: 200,
      spread: 90,
      origin: { y: 0.6 },
      ticks: 200,
    });
  }
}
```

- [ ] **Step 4: Registrar rota**

```ts
{
  path: 'checkout/sucesso/:numero',
  canActivate: [authGuard],
  loadComponent: () =>
    import('@modules/checkout/pages/success/success.page')
      .then((moduleSuccess) => moduleSuccess.CheckoutSuccessPage),
},
```

- [ ] **Step 5: Typecheck + commit**

```bash
cd /home/ali/projects/pet-hub/frontend/storefront && npx ng build --configuration development
cd /home/ali/projects/pet-hub && git add \
  frontend/storefront/src/app/modules/checkout/pages/success \
  frontend/storefront/src/app/app.routes.ts
git commit -m "feat(storefront): checkout success page with confetti"
```

---

## Task 15: Smoke E2E completo + memory update + closing commit

Setup: infra + backend + storefront rodando. Login Maria `maria.fase2@pethub.com / Senha@123`.

- [ ] **Step 1: Smoke E2E manual em sequência** (sem fechar a aba entre passos)

a) **Carrinho**
- Adicionar `COLLAR-PRO-001` qty=1 do PDP. Header mostra badge `1`.
- `/carrinho`: foto/preço/subtotal corretos. `+` → qty=2, subtotal dobra; `-` em qty=1 remove.
- Cupom `10OFF` aplica → chip aparece. `EXPIRED` aceita anexar. `×` remove o chip.

b) **Endereço**
- `/checkout/endereco`: endereço da Maria pré-selecionado (ou criar com CEP `01310-100`).
- Desmarcar "mesmo para cobrança" → segunda lista aparece.

c) **Frete**
- 3 opções; mais barato auto-selecionado. CEP fora SP esconde motoboy.

d) **Pagamento**
- Cartão `4111 1111 1111 1111`, `Maria Teste`, `12/2030`, CVV `123` → tokenize 200, cartão auto-selecionado.
- DevTools Network: PAN só em `/tokenize`, ausente em outros requests.
- DevTools Application → SessionStorage `pethub:checkout:v1`: sem PAN/CVV/token.

e) **Revisão**
- Breakdown completo, linha "Cupom 10OFF" aparece.
- "Editar endereço" → passo 1 → trocar → voltar → preview recalcula.

f) **Idempotência**
- Slow 3G + "Finalizar" → loading bloqueia 2º clique.
- SessionStorage: `idempotencyKey` presente antes do submit; vai a `null` após APPROVED.

g) **Sucesso**
- `/checkout/sucesso/PH-2026-NNNNNN`: confete dispara 1×. Total + método. CTA "Acompanhar" disabled.
- `/carrinho` agora vazio.

h) **Recusa**
- Refazer com cartão `4000 0000 0000 4000` (Luhn válido, last4=4000 rejeita determinístico).
- "Finalizar" → toast erro vermelho. SessionStorage: `idempotencyKey` voltou a `null`.

i) **Estoque mudou**
- Em outro terminal:
  ```bash
  docker exec -i pethub-postgres psql -U pethub -d pethub <<'SQL'
  UPDATE estoque SET quantidade = 0
  WHERE produto_id = (SELECT id FROM produtos WHERE sku = 'COLLAR-PRO-001');
  SQL
  ```
- Recarregar revisão (ou clicar Finalizar) → dialog "Estoque mudou" → CTA "Ajustar carrinho" → `/carrinho`.
- Restaurar:
  ```bash
  docker exec -i pethub-postgres psql -U pethub -d pethub <<'SQL'
  UPDATE estoque SET quantidade = 50
  WHERE produto_id = (SELECT id FROM produtos WHERE sku = 'COLLAR-PRO-001');
  SQL
  ```

j) **AppSec spot-check**
- Cookies: `pethub_refresh` HttpOnly + SameSite=Lax.
- LocalStorage: vazio (ou só prefs).
- Console: zero erros não-handled.

- [ ] **Step 2: Atualizar `project_fase5_em_andamento.md`** marcando slice 3 como entregue (paths corretos para slice 4 retomar).

Path: `/home/ali/.claude/projects/-home-ali-projects-pet-hub/memory/project_fase5_em_andamento.md` — substituir conteúdo:

```markdown
---
name: project-fase5-em-andamento
description: Pet Hub Fase 5 (Storefront Angular) — slices 1, 2 e 3 entregues; estrutura migrada para padrão modules/; slice 4 pendente
metadata:
  node_type: memory
  type: project
---

Fase 5 do Pet Hub: 3 dos 4 slices em `origin/main`. Refactor estrutural em `d80e519` migrou `features/` → `modules/`, separou templates em `.html`+`.scss`, introduziu path aliases (`@core`, `@shared`, `@modules`, `@env`), eliminou variáveis de 1 letra. Auditoria OWASP 2025 mapeada em `AUDITORIA-OWASP-2025-PETHUB.md`. **Slice 4 (minha conta + timeline visual) é o último, pendente.**

## ✅ Slices entregues

- Slice 1 (`2778cf8`) — scaffold + auth.
- Slice 2 (`167a887`) — catálogo.
- Refactor (`d80e519`) — modules/, .html+.scss, aliases.
- OWASP mapping (`1f85cde`) — 5 GAPs novos em `appsec-pendencias.md`.
- Slice 3 — carrinho + checkout 4 passos + sucesso com confete (commits feat(storefront) ~9-14 do plano v2). Decisões congeladas: sessionStorage `pethub:checkout:v1`, cadastro inline de endereço/cartão no wizard, cupom só em `/carrinho`, tokenize via `POST /customers/payment-methods/tokenize` (PAN só nessa request).

## ⏳ Slice 4 — Minha conta + Timeline visual
- `/minha-conta` overview (cards perfil, endereços, cartões, pets, pedidos).
- CRUDs em `modules/customer/pages/{perfil,enderecos,cartoes,pets}/` (modules/customer já existe com models+services).
- `modules/orders/` (NOVO) com `pages/lista-pedidos`, `pages/detalhe-pedido` (timeline visual horizontal/vertical responsiva).
- Habilitar CTA "Acompanhar pedido" da tela de sucesso slice 3 (hoje disabled).
- `modules/shared/pipes/safe-html.pipe.ts` com DOMPurify para o `[innerHTML]` do product-detail.

## 🚀 Como retomar

```bash
cd /home/ali/projects/pet-hub
git log --oneline -10
docker compose --env-file .env.local -f infrastructure/docker/docker-compose.dev.yml up -d
cd backend && docker run --rm -d --name pethub-app-dev \
  --env-file /home/ali/projects/pet-hub/.env.local \
  -v "$PWD":/workspace -v /home/ali/.m2:/root/.m2 \
  --network host -w /workspace \
  maven:3.9-eclipse-temurin-21 \
  mvn -B -ntp -pl application spring-boot:run -DskipTests
cd /home/ali/projects/pet-hub/frontend/storefront
npx ng serve --host 127.0.0.1 --port 4242 --proxy-config proxy.conf.json
```

Storefront http://127.0.0.1:4242 — login `maria.fase2@pethub.com / Senha@123`.

## ⚠️ Dívidas e decisões para slice 4

- Testes Karma/Jest formais continuam dívida (mesmo bloqueio das Fases 1-4).
- AppSec pendências (`appsec-pendencias.md`): JWT-1, CORS-1, VAL-1 viram bloqueantes só no caminho para staging.
- `fase-5-pendencias.md` será criado depois do slice 4 fechar.

Linked: [[feedback-naming]], [[feedback-coding-patterns]].
```

- [ ] **Step 3: Verificar ROADMAP.md**

Se a Fase 5 ainda está `⬜`, atualizar para `🚧 (slices 1-3 done, slice 4 pending)`. Commit em separado se mudar:

```bash
cd /home/ali/projects/pet-hub && git add ROADMAP.md
git commit -m "docs(roadmap): slice 3 da Fase 5 entregue (carrinho + checkout + sucesso)"
```

---

## Done

Estado final:
- 6 rotas novas (`/carrinho` + 5 checkout) sob `modules/cart` e `modules/checkout`.
- 5 services + 1 guard organizados por módulo.
- 3 components no slice 3 (`stepper`, `cart-item-row`, `coupon-input`) em pastas dedicadas com `.ts + .html + .scss`.
- 6 pages do wizard em pastas dedicadas com `.ts + .html + .scss`.
- Auth interceptor inalterado.
- `canvas-confetti` na dep tree.
- PAN/CVV trafegam só em `/tokenize`; sessionStorage nunca recebe.
- Idempotency-key: reusada em retry de transporte, regenerada após REJECTED.
- Smoke E2E completo verificado.

Próxima sessão: slice 4. Memory atualizada para retomar com novo padrão de pastas e regras.
