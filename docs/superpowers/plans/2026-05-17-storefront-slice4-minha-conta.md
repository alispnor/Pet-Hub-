# Storefront Slice 4 — Minha conta + Timeline visual — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Entregar o último slice da Fase 5: área autenticada `/minha-conta` com overview de cards ao vivo, CRUDs de perfil/endereços/cartões/pets, módulo `orders` com listagem paginada e detalhe com `OrderTimelineComponent` (destaque visual), `SafeHtmlPipe` com DOMPurify, e habilitação do CTA "Acompanhar pedido" na success page do checkout.

**Architecture:** Angular 17 standalone components com Tailwind, signals + RxJS, lazy loading por rota. Layout shell `/minha-conta` com sidebar vertical (md+) ou drawer hambúrguer (mobile) + `<router-outlet>` para sub-rotas. Backend já provê todos os endpoints necessários (Fases 1-4). Services consumidos sem retries automáticos — erros tratados por toast/banner. Persistência: nada novo; estado de UI é local por página (signals).

**Tech Stack:** Angular 17, TypeScript strict, Tailwind 3, Reactive Forms, RxJS (forkJoin/debounceTime), DOMPurify (novo), `<dialog>` HTML5 nativo (sem libs de modal).

**Spec:** `docs/superpowers/specs/2026-05-17-storefront-slice4-minha-conta-design.md` (referência durante implementação para decisões tomadas, tabelas de campos, smoke checklist).

**Convenção de commits:** `feat(storefront): slice4 NN-descricao` (NN = 01..20). Não usar `--no-verify`.

**Política de testes:** Tests formais Karma/Jest são dívida acumulada (mesmo bloqueio fases 1-4). Verificação por task = `npm run build` e/ou smoke manual no browser. Smoke checklist completa rodada na Task 20.

---

## File Structure

**Arquivos novos:**

```
frontend/storefront/src/app/
├── shared/
│   ├── pipes/safe-html.pipe.ts
│   ├── components/skeleton/skeleton.component.ts
│   ├── components/toast-stack/toast-stack.component.{ts,html,scss}
│   ├── components/confirm-dialog/confirm-dialog.component.{ts,html,scss}
│   └── services/{toast.service.ts, confirm-dialog.service.ts}
├── modules/customer/
│   ├── models/{perfil.ts, pet.ts}
│   ├── services/{perfil.service.ts, pet.service.ts}
│   └── pages/
│       ├── account-shell/account-shell.page.{ts,html,scss}
│       ├── overview/overview.page.{ts,html,scss}
│       ├── perfil/perfil.page.{ts,html,scss}
│       ├── enderecos/enderecos.page.{ts,html,scss}
│       ├── cartoes/cartoes.page.{ts,html,scss}
│       └── pets/pets.page.{ts,html,scss}
└── modules/orders/
    ├── models/order.ts
    ├── services/order.service.ts
    ├── components/order-timeline/order-timeline.component.{ts,html,scss}
    └── pages/
        ├── lista/lista.page.{ts,html,scss}
        └── detalhe/detalhe.page.{ts,html,scss}
```

**Arquivos modificados:**

- `frontend/storefront/package.json` (+ `dompurify`, `@types/dompurify`)
- `frontend/storefront/src/app/app.routes.ts` (rotas filhas de `/minha-conta`)
- `frontend/storefront/src/app/core/layout/main-layout/main-layout.component.{ts,html}` (toast stack + confirm dialog hosts + botão hambúrguer condicional)
- `frontend/storefront/src/app/modules/customer/services/address.service.ts` (+ `update`, `remove`, `setDefault`)
- `frontend/storefront/src/app/modules/customer/services/payment-method.service.ts` (+ `remove`, `setDefault`)
- `frontend/storefront/src/app/modules/customer/models/address.ts` (+ `UpdateEnderecoRequest`, `SetDefaultRequest`)
- `frontend/storefront/src/app/modules/catalog/pages/product-detail/product-detail.page.{ts,html}` (usar SafeHtmlPipe, remover `descricaoSanitizada()`)
- `frontend/storefront/src/app/modules/checkout/pages/success/success.page.html` (habilitar CTA "Acompanhar pedido")
- `ai-memory/roadmap/fase-5-pendencias.md` (marcar slice 4 entregue)
- `ROADMAP.md` (Fase 5 ✅)

---

## Task 1: Instalar DOMPurify

**Files:**
- Modify: `frontend/storefront/package.json` (via npm install)

- [ ] **Step 1: Instalar dependência**

```bash
cd /home/ali/projects/pet-hub/frontend/storefront
npm install dompurify@^3 @types/dompurify
```

- [ ] **Step 2: Verificar que `package.json` ganhou entradas**

```bash
grep -E '"(dompurify|@types/dompurify)"' package.json
```

Expected: 2 linhas (uma em `dependencies`, uma em `devDependencies`).

- [ ] **Step 3: Build verifica que não quebrou nada**

```bash
npm run build
```

Expected: build sem erros.

- [ ] **Step 4: Commit**

```bash
cd /home/ali/projects/pet-hub
git add frontend/storefront/package.json frontend/storefront/package-lock.json
git commit -m "chore(storefront): slice4 01-add dompurify dependency"
```

---

## Task 2: SafeHtmlPipe e substituição no product-detail

**Files:**
- Create: `frontend/storefront/src/app/shared/pipes/safe-html.pipe.ts`
- Modify: `frontend/storefront/src/app/modules/catalog/pages/product-detail/product-detail.page.ts`
- Modify: `frontend/storefront/src/app/modules/catalog/pages/product-detail/product-detail.page.html`

- [ ] **Step 1: Criar `SafeHtmlPipe`**

Caminho: `frontend/storefront/src/app/shared/pipes/safe-html.pipe.ts`

```ts
import { Pipe, PipeTransform, inject } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import DOMPurify from 'dompurify';

@Pipe({ name: 'safeHtml', standalone: true })
export class SafeHtmlPipe implements PipeTransform {
  private readonly sanitizer = inject(DomSanitizer);

  transform(value: string | null | undefined): SafeHtml {
    if (!value) {
      return '';
    }
    const limpo = DOMPurify.sanitize(value, {
      ALLOWED_TAGS: ['p', 'br', 'strong', 'em', 'ul', 'ol', 'li', 'a'],
      ALLOWED_ATTR: ['href', 'target', 'rel'],
      ALLOW_DATA_ATTR: false,
    });
    return this.sanitizer.bypassSecurityTrustHtml(limpo);
  }
}
```

- [ ] **Step 2: Remover `descricaoSanitizada()` do product-detail.page.ts**

Em `frontend/storefront/src/app/modules/catalog/pages/product-detail/product-detail.page.ts`:

1. Remover o método `descricaoSanitizada(textoCru: string): string` e o JSDoc associado.
2. Adicionar `SafeHtmlPipe` em `imports`:

```ts
import { SafeHtmlPipe } from '@shared/pipes/safe-html.pipe';
// ...
imports: [/* já existentes */, SafeHtmlPipe],
```

- [ ] **Step 3: Atualizar template do product-detail**

Em `frontend/storefront/src/app/modules/catalog/pages/product-detail/product-detail.page.html`, substituir a linha:

```html
<div [innerHTML]="descricaoSanitizada(produtoExibido.descricaoCompleta)"></div>
```

por:

```html
<div [innerHTML]="produtoExibido.descricaoCompleta | safeHtml"></div>
```

(Manter classes/atributos da mesma `<div>`.)

- [ ] **Step 4: Build**

```bash
cd /home/ali/projects/pet-hub/frontend/storefront && npm run build
```

Expected: build sem erros.

- [ ] **Step 5: Smoke manual rápido**

Backend e storefront rodando (`docker ps` mostra `pethub-postgres/redis/pgadmin/backend`; storefront em :4242). Abrir `http://127.0.0.1:4242/produtos/COLEIRA-GPS-001` (ou outro SKU do seed) e verificar que a descrição renderiza. Se a descrição tiver tags HTML, devem renderizar (não escapadas).

- [ ] **Step 6: Commit**

```bash
git add frontend/storefront/src/app/shared/pipes/safe-html.pipe.ts \
        frontend/storefront/src/app/modules/catalog/pages/product-detail/product-detail.page.ts \
        frontend/storefront/src/app/modules/catalog/pages/product-detail/product-detail.page.html
git commit -m "feat(storefront): slice4 02-safe-html pipe with dompurify"
```

---

## Task 3: SkeletonComponent (shared)

**Files:**
- Create: `frontend/storefront/src/app/shared/components/skeleton/skeleton.component.ts`

- [ ] **Step 1: Criar `SkeletonComponent`**

```ts
import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

type Variante = 'text' | 'card' | 'avatar';

@Component({
  selector: 'app-skeleton',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div [class]="containerClasses" role="status" aria-live="polite" aria-busy="true">
      <ng-container [ngSwitch]="variant">
        <ng-container *ngSwitchCase="'avatar'">
          <div class="h-12 w-12 rounded-full bg-graphite-200 animate-pulse"></div>
        </ng-container>
        <ng-container *ngSwitchCase="'card'">
          <div class="h-32 w-full rounded-lg bg-graphite-200 animate-pulse"></div>
        </ng-container>
        <ng-container *ngSwitchDefault>
          <div *ngFor="let _ of repeticoes" class="h-3 w-full rounded bg-graphite-200 animate-pulse"></div>
        </ng-container>
      </ng-container>
      <span class="sr-only">Carregando...</span>
    </div>
  `,
})
export class SkeletonComponent {
  @Input() lines = 3;
  @Input() variant: Variante = 'text';

  get repeticoes(): number[] {
    return Array.from({ length: this.lines }, (_, indice) => indice);
  }

  get containerClasses(): string {
    return this.variant === 'text' ? 'space-y-2' : '';
  }
}
```

- [ ] **Step 2: Build**

```bash
cd /home/ali/projects/pet-hub/frontend/storefront && npm run build
```

Expected: build sem erros.

- [ ] **Step 3: Commit**

```bash
git add frontend/storefront/src/app/shared/components/skeleton/skeleton.component.ts
git commit -m "feat(storefront): slice4 03-skeleton shared component"
```

---

## Task 4: ToastService + ToastStackComponent + wire em MainLayout

**Files:**
- Create: `frontend/storefront/src/app/shared/services/toast.service.ts`
- Create: `frontend/storefront/src/app/shared/components/toast-stack/toast-stack.component.ts`
- Modify: `frontend/storefront/src/app/core/layout/main-layout/main-layout.component.ts`
- Modify: `frontend/storefront/src/app/core/layout/main-layout/main-layout.component.html`

- [ ] **Step 1: Criar `ToastService`**

```ts
import { Injectable, signal } from '@angular/core';

export type TipoToast = 'success' | 'error' | 'info';

export interface Toast {
  id: string;
  tipo: TipoToast;
  mensagem: string;
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  readonly mensagens = signal<Toast[]>([]);

  success(mensagem: string): void { this.empilhar('success', mensagem); }
  error(mensagem: string): void { this.empilhar('error', mensagem); }
  info(mensagem: string): void { this.empilhar('info', mensagem); }

  dismiss(id: string): void {
    this.mensagens.update(lista => lista.filter(toast => toast.id !== id));
  }

  private empilhar(tipo: TipoToast, mensagem: string): void {
    const id = crypto.randomUUID();
    this.mensagens.update(lista => [...lista, { id, tipo, mensagem }]);
    setTimeout(() => this.dismiss(id), 5000);
  }
}
```

- [ ] **Step 2: Criar `ToastStackComponent`**

```ts
import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastService, Toast } from '@shared/services/toast.service';

@Component({
  selector: 'app-toast-stack',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="fixed top-4 right-4 z-50 flex flex-col gap-2 max-w-sm" aria-live="polite" aria-atomic="false">
      <div *ngFor="let toast of toastService.mensagens()"
           [class]="classesPorTipo(toast)"
           class="rounded-md border px-4 py-3 shadow-md flex items-start gap-3 animate-fade-in">
        <span class="text-sm flex-1">{{ toast.mensagem }}</span>
        <button type="button" (click)="toastService.dismiss(toast.id)"
                class="text-sm opacity-70 hover:opacity-100" aria-label="Fechar notificação">✕</button>
      </div>
    </div>
  `,
  styles: [`
    @keyframes fade-in { from { opacity: 0; transform: translateY(-8px); } to { opacity: 1; transform: none; } }
    .animate-fade-in { animation: fade-in 150ms ease-out; }
  `],
})
export class ToastStackComponent {
  readonly toastService = inject(ToastService);

  classesPorTipo(toast: Toast): string {
    switch (toast.tipo) {
      case 'success': return 'bg-emerald-50 border-emerald-200 text-emerald-900';
      case 'error':   return 'bg-red-50 border-red-200 text-red-900';
      default:        return 'bg-blue-50 border-blue-200 text-blue-900';
    }
  }
}
```

- [ ] **Step 3: Wire `ToastStackComponent` no `MainLayoutComponent`**

Em `frontend/storefront/src/app/core/layout/main-layout/main-layout.component.ts`, adicionar import:

```ts
import { ToastStackComponent } from '@shared/components/toast-stack/toast-stack.component';
```

E adicionar `ToastStackComponent` ao array `imports`:

```ts
imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive, ToastStackComponent],
```

Em `main-layout.component.html`, adicionar a tag no final do `<div class="min-h-screen ...">`, antes do `</div>` raiz:

```html
<app-toast-stack />
```

- [ ] **Step 4: Build**

```bash
cd /home/ali/projects/pet-hub/frontend/storefront && npm run build
```

Expected: build sem erros.

- [ ] **Step 5: Commit**

```bash
git add frontend/storefront/src/app/shared/services/toast.service.ts \
        frontend/storefront/src/app/shared/components/toast-stack/toast-stack.component.ts \
        frontend/storefront/src/app/core/layout/main-layout/main-layout.component.ts \
        frontend/storefront/src/app/core/layout/main-layout/main-layout.component.html
git commit -m "feat(storefront): slice4 04-toast service and stack"
```

---

## Task 5: ConfirmDialog (service + component) + wire em MainLayout

**Files:**
- Create: `frontend/storefront/src/app/shared/services/confirm-dialog.service.ts`
- Create: `frontend/storefront/src/app/shared/components/confirm-dialog/confirm-dialog.component.ts`
- Modify: `frontend/storefront/src/app/core/layout/main-layout/main-layout.component.ts`
- Modify: `frontend/storefront/src/app/core/layout/main-layout/main-layout.component.html`

- [ ] **Step 1: Criar `ConfirmDialogService`**

```ts
import { Injectable, signal } from '@angular/core';

export type VarianteAcao = 'primary' | 'danger';

export interface ConfirmacaoPendente {
  titulo: string;
  mensagem: string;
  acaoLabel: string;
  acaoVariant: VarianteAcao;
  resolve: (confirmado: boolean) => void;
}

@Injectable({ providedIn: 'root' })
export class ConfirmDialogService {
  readonly pendente = signal<ConfirmacaoPendente | null>(null);

  open(opcoes: {
    titulo: string;
    mensagem: string;
    acaoLabel: string;
    acaoVariant?: VarianteAcao;
  }): Promise<boolean> {
    return new Promise<boolean>(resolve => {
      this.pendente.set({
        titulo: opcoes.titulo,
        mensagem: opcoes.mensagem,
        acaoLabel: opcoes.acaoLabel,
        acaoVariant: opcoes.acaoVariant ?? 'primary',
        resolve,
      });
    });
  }

  responder(confirmado: boolean): void {
    const atual = this.pendente();
    if (atual) {
      atual.resolve(confirmado);
      this.pendente.set(null);
    }
  }
}
```

- [ ] **Step 2: Criar `ConfirmDialogComponent`**

```ts
import { AfterViewInit, Component, ElementRef, ViewChild, effect, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ConfirmDialogService } from '@shared/services/confirm-dialog.service';

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  imports: [CommonModule],
  template: `
    <dialog #dialogo class="rounded-lg p-0 backdrop:bg-graphite-900/40 max-w-md w-full"
            (close)="aoFechar()" (cancel)="$event.preventDefault(); cancelar()">
      <ng-container *ngIf="service.pendente() as pendente">
        <div class="p-6">
          <h2 class="text-lg font-semibold text-graphite-900">{{ pendente.titulo }}</h2>
          <p class="mt-2 text-sm text-graphite-700">{{ pendente.mensagem }}</p>
          <div class="mt-6 flex justify-end gap-3">
            <button type="button" class="btn-ghost text-sm" (click)="cancelar()">Cancelar</button>
            <button type="button"
                    [class]="pendente.acaoVariant === 'danger' ? 'btn-danger' : 'btn-primary'"
                    class="text-sm" (click)="confirmar()">
              {{ pendente.acaoLabel }}
            </button>
          </div>
        </div>
      </ng-container>
    </dialog>
  `,
  styles: [`
    dialog[open] { display: block; }
    .btn-danger { background-color: rgb(220 38 38); color: white; padding: 0.5rem 1rem; border-radius: 0.375rem; }
    .btn-danger:hover { background-color: rgb(185 28 28); }
  `],
})
export class ConfirmDialogComponent implements AfterViewInit {
  @ViewChild('dialogo') dialogoRef!: ElementRef<HTMLDialogElement>;
  readonly service = inject(ConfirmDialogService);

  constructor() {
    effect(() => {
      const pendente = this.service.pendente();
      const dialogo = this.dialogoRef?.nativeElement;
      if (!dialogo) return;
      if (pendente && !dialogo.open) {
        dialogo.showModal();
      } else if (!pendente && dialogo.open) {
        dialogo.close();
      }
    });
  }

  ngAfterViewInit(): void { /* effect roda após inicializar */ }

  confirmar(): void { this.service.responder(true); }
  cancelar(): void { this.service.responder(false); }

  aoFechar(): void {
    if (this.service.pendente()) {
      this.service.responder(false);
    }
  }
}
```

- [ ] **Step 3: Wire `ConfirmDialogComponent` no `MainLayoutComponent`**

Em `frontend/storefront/src/app/core/layout/main-layout/main-layout.component.ts`, adicionar:

```ts
import { ConfirmDialogComponent } from '@shared/components/confirm-dialog/confirm-dialog.component';
```

E adicionar ao array `imports` (junto com `ToastStackComponent` que veio na Task 4):

```ts
imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive, ToastStackComponent, ConfirmDialogComponent],
```

Em `main-layout.component.html`, adicionar a tag antes do `</div>` raiz, junto com `<app-toast-stack />`:

```html
<app-toast-stack />
<app-confirm-dialog />
```

- [ ] **Step 4: Build**

```bash
cd /home/ali/projects/pet-hub/frontend/storefront && npm run build
```

Expected: build sem erros.

- [ ] **Step 5: Commit**

```bash
git add frontend/storefront/src/app/shared/services/confirm-dialog.service.ts \
        frontend/storefront/src/app/shared/components/confirm-dialog/confirm-dialog.component.ts \
        frontend/storefront/src/app/core/layout/main-layout/main-layout.component.ts \
        frontend/storefront/src/app/core/layout/main-layout/main-layout.component.html
git commit -m "feat(storefront): slice4 05-confirm dialog with native dialog element"
```

---

## Task 6: PerfilModel + PerfilService

**Files:**
- Create: `frontend/storefront/src/app/modules/customer/models/perfil.ts`
- Create: `frontend/storefront/src/app/modules/customer/services/perfil.service.ts`

- [ ] **Step 1: Criar `perfil.ts`**

```ts
export type Genero = 'MASCULINO' | 'FEMININO' | 'NAO_INFORMADO' | 'OUTRO';

export interface PerfilResponse {
  id: number;
  nome: string;
  email: string;
  cpfMascarado: string | null;
  dataNascimento: string | null;       // ISO date
  genero: Genero | null;
  telefoneAdicional: string | null;
  aceiteTermos: boolean;
  aceiteMarketing: boolean;
}

export interface UpdatePerfilRequest {
  dataNascimento?: string | null;
  genero?: Genero | null;
  telefoneAdicional?: string | null;
  aceiteTermos?: boolean;
  aceiteMarketing?: boolean;
}

export const GENEROS_LABEL: Record<Genero, string> = {
  MASCULINO: 'Masculino',
  FEMININO: 'Feminino',
  NAO_INFORMADO: 'Prefiro não informar',
  OUTRO: 'Outro',
};
```

- [ ] **Step 2: Criar `perfil.service.ts`**

```ts
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '@env/environment';
import { PerfilResponse, UpdatePerfilRequest } from '../models/perfil';

@Injectable({ providedIn: 'root' })
export class PerfilService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiBase;

  get(): Observable<PerfilResponse> {
    return this.http.get<PerfilResponse>(`${this.api}/customers/me/profile`);
  }

  update(req: UpdatePerfilRequest): Observable<PerfilResponse> {
    return this.http.put<PerfilResponse>(`${this.api}/customers/me/profile`, req);
  }
}
```

- [ ] **Step 3: Build**

```bash
cd /home/ali/projects/pet-hub/frontend/storefront && npm run build
```

- [ ] **Step 4: Commit**

```bash
git add frontend/storefront/src/app/modules/customer/models/perfil.ts \
        frontend/storefront/src/app/modules/customer/services/perfil.service.ts
git commit -m "feat(storefront): slice4 06-perfil model and service"
```

---

## Task 7: PetModel + PetService

**Files:**
- Create: `frontend/storefront/src/app/modules/customer/models/pet.ts`
- Create: `frontend/storefront/src/app/modules/customer/services/pet.service.ts`

- [ ] **Step 1: Criar `pet.ts`**

```ts
export type Especie = 'CACHORRO' | 'GATO' | 'AVE' | 'PEIXE' | 'REPTIL' | 'OUTROS';
export type Porte = 'PEQUENO' | 'MEDIO' | 'GRANDE' | 'GIGANTE';

export interface PetResponse {
  id: number;
  nome: string;
  especie: Especie;
  raca: string | null;
  dataNascimento: string | null;
  pesoKg: number | null;
  porte: Porte | null;
  observacoes: string | null;
  fotoUrl: string | null;
}

export interface CreatePetRequest {
  nome: string;
  especie: Especie;
  raca?: string;
  dataNascimento?: string;
  pesoKg?: number;
  porte?: Porte;
  observacoes?: string;
}

export type UpdatePetRequest = Partial<CreatePetRequest>;

export const ESPECIES_LABEL: Record<Especie, string> = {
  CACHORRO: 'Cachorro',
  GATO: 'Gato',
  AVE: 'Ave',
  PEIXE: 'Peixe',
  REPTIL: 'Réptil',
  OUTROS: 'Outros',
};

export const PORTES_LABEL: Record<Porte, string> = {
  PEQUENO: 'Pequeno',
  MEDIO: 'Médio',
  GRANDE: 'Grande',
  GIGANTE: 'Gigante',
};
```

- [ ] **Step 2: Criar `pet.service.ts`**

```ts
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '@env/environment';
import { CreatePetRequest, PetResponse, UpdatePetRequest } from '../models/pet';

@Injectable({ providedIn: 'root' })
export class PetService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiBase;

  list(): Observable<PetResponse[]> {
    return this.http.get<PetResponse[]>(`${this.api}/customers/me/pets`);
  }

  create(req: CreatePetRequest): Observable<PetResponse> {
    return this.http.post<PetResponse>(`${this.api}/customers/me/pets`, req);
  }

  update(id: number, req: UpdatePetRequest): Observable<PetResponse> {
    return this.http.put<PetResponse>(`${this.api}/customers/me/pets/${id}`, req);
  }

  remove(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/customers/me/pets/${id}`);
  }

  uploadPhoto(id: number, file: File): Observable<PetResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<PetResponse>(`${this.api}/customers/me/pets/${id}/photo`, formData);
  }
}
```

- [ ] **Step 3: Build**

```bash
cd /home/ali/projects/pet-hub/frontend/storefront && npm run build
```

- [ ] **Step 4: Commit**

```bash
git add frontend/storefront/src/app/modules/customer/models/pet.ts \
        frontend/storefront/src/app/modules/customer/services/pet.service.ts
git commit -m "feat(storefront): slice4 07-pet model and service"
```

---

## Task 8: OrderModel + OrderService

**Files:**
- Create: `frontend/storefront/src/app/modules/orders/models/order.ts`
- Create: `frontend/storefront/src/app/modules/orders/services/order.service.ts`

- [ ] **Step 1: Criar `order.ts`**

```ts
export type StatusPedido =
  | 'PENDENTE_PAGAMENTO'
  | 'PAGAMENTO_APROVADO'
  | 'PAGAMENTO_REJEITADO'
  | 'SEPARACAO'
  | 'EM_TRANSPORTE'
  | 'ENTREGUE'
  | 'CANCELADO'
  | 'DEVOLVIDO';

export type StatusEtapa = 'CONCLUIDA' | 'ATUAL' | 'PENDENTE';

export interface EtapaTimeline {
  nome: string;
  status: StatusEtapa;
  ocorridoEm: string | null;
  tempoDecorrido: string | null;
  previsaoEntrega: string | null;
}

export interface Timeline {
  numeroPedido: string;
  statusAtual: StatusPedido;
  etapas: EtapaTimeline[];
}

export interface PedidoResumoResponse {
  id: number;
  numeroPedido: string;
  status: StatusPedido;
  valorTotal: number;
  formaPagamentoTipo: string;
  totalItens: number;
  criadoEm: string;
}

export interface PedidoItemResponse {
  skuProduto: string;
  nomeProduto: string;
  precoUnitario: number;
  quantidade: number;
  fotoUrl: string | null;
}

export interface EnderecoSnapshot {
  apelido?: string;
  logradouro: string;
  numero?: string;
  complemento?: string;
  bairro: string;
  cidade: string;
  uf: string;
  cep: string;
}

export interface OpcaoFreteSnapshot {
  codigo: string;
  transportadora: string;
  valor: number;
  prazoDias: number;
}

export interface PedidoResponse {
  id: number;
  numeroPedido: string;
  clienteId: number;
  status: StatusPedido;
  itens: PedidoItemResponse[];
  enderecoEntrega: EnderecoSnapshot;
  enderecoCobranca: EnderecoSnapshot;
  opcaoFrete: OpcaoFreteSnapshot;
  cupomCodigo: string | null;
  valorSubtotal: number;
  valorDescontos: number;
  valorImpostos: number;
  valorFrete: number;
  valorTotal: number;
  formaPagamentoTipo: string;
  formaPagamentoUltimos4: string | null;
  formaPagamentoBandeira: string | null;
  tentativaPagamentoId: number | null;
  observacoes: string | null;
  criadoEm: string;
  atualizadoEm: string;
}

export interface PageResponse<T> {
  content: T[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export const STATUS_LABEL: Record<StatusPedido, string> = {
  PENDENTE_PAGAMENTO: 'Aguardando pagamento',
  PAGAMENTO_APROVADO: 'Pagamento aprovado',
  PAGAMENTO_REJEITADO: 'Pagamento rejeitado',
  SEPARACAO: 'Em separação',
  EM_TRANSPORTE: 'Em transporte',
  ENTREGUE: 'Entregue',
  CANCELADO: 'Cancelado',
  DEVOLVIDO: 'Devolvido',
};

export function ehStatusTerminal(status: StatusPedido): boolean {
  return status === 'ENTREGUE'
      || status === 'CANCELADO'
      || status === 'DEVOLVIDO'
      || status === 'PAGAMENTO_REJEITADO';
}

export function classesBadgeStatus(status: StatusPedido): string {
  switch (status) {
    case 'ENTREGUE':
    case 'PAGAMENTO_APROVADO':
      return 'bg-emerald-100 text-emerald-800';
    case 'CANCELADO':
    case 'PAGAMENTO_REJEITADO':
      return 'bg-red-100 text-red-800';
    case 'DEVOLVIDO':
      return 'bg-amber-100 text-amber-800';
    case 'SEPARACAO':
    case 'EM_TRANSPORTE':
      return 'bg-yellow-100 text-yellow-800';
    case 'PENDENTE_PAGAMENTO':
      return 'bg-blue-100 text-blue-800';
  }
}
```

- [ ] **Step 2: Criar `order.service.ts`**

```ts
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '@env/environment';
import {
  PageResponse,
  PedidoResponse,
  PedidoResumoResponse,
  StatusPedido,
  Timeline,
} from '../models/order';

export interface ListarPedidosParams {
  page?: number;
  size?: number;
  status?: StatusPedido;
  dataInicio?: string;     // ISO datetime
  dataFim?: string;        // ISO datetime
  sort?: string;
}

@Injectable({ providedIn: 'root' })
export class OrderService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiBase;

  list(params: ListarPedidosParams = {}): Observable<PageResponse<PedidoResumoResponse>> {
    let httpParams = new HttpParams();
    if (params.page !== undefined) httpParams = httpParams.set('page', params.page);
    if (params.size !== undefined) httpParams = httpParams.set('size', params.size);
    if (params.status) httpParams = httpParams.set('status', params.status);
    if (params.dataInicio) httpParams = httpParams.set('dataInicio', params.dataInicio);
    if (params.dataFim) httpParams = httpParams.set('dataFim', params.dataFim);
    if (params.sort) httpParams = httpParams.set('sort', params.sort);
    return this.http.get<PageResponse<PedidoResumoResponse>>(`${this.api}/orders`, { params: httpParams });
  }

  detalhe(numeroPedido: string): Observable<PedidoResponse> {
    return this.http.get<PedidoResponse>(`${this.api}/orders/${numeroPedido}`);
  }

  timeline(numeroPedido: string): Observable<Timeline> {
    return this.http.get<Timeline>(`${this.api}/orders/${numeroPedido}/timeline`);
  }
}
```

- [ ] **Step 3: Build**

```bash
cd /home/ali/projects/pet-hub/frontend/storefront && npm run build
```

- [ ] **Step 4: Commit**

```bash
git add frontend/storefront/src/app/modules/orders/models/order.ts \
        frontend/storefront/src/app/modules/orders/services/order.service.ts
git commit -m "feat(storefront): slice4 08-order model and service"
```

---

## Task 9: Estender AddressService e PaymentMethodService

**Files:**
- Modify: `frontend/storefront/src/app/modules/customer/models/address.ts` (+ `UpdateEnderecoRequest`, `SetDefaultRequest`)
- Modify: `frontend/storefront/src/app/modules/customer/services/address.service.ts` (+ `update`, `remove`, `setDefault`)
- Modify: `frontend/storefront/src/app/modules/customer/services/payment-method.service.ts` (+ `remove`, `setDefault`)

- [ ] **Step 1: Adicionar tipos ao `address.ts`**

Em `frontend/storefront/src/app/modules/customer/models/address.ts`, anexar (no fim do arquivo, mantendo o conteúdo existente):

```ts
export interface UpdateEnderecoRequest {
  apelido?: string;
  cep?: string;
  logradouro?: string;
  numero?: string;
  complemento?: string;
  bairro?: string;
  cidade?: string;
  uf?: UnidadeFederativa;
  tipo?: TipoEndereco;
  ativo?: boolean;
}

export interface SetDefaultRequest {
  padraoEntrega?: boolean;
  padraoCobranca?: boolean;
}
```

- [ ] **Step 2: Estender `address.service.ts`**

Substituir o conteúdo de `frontend/storefront/src/app/modules/customer/services/address.service.ts` por:

```ts
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '@env/environment';
import {
  CreateEnderecoRequest,
  EnderecoResponse,
  SetDefaultRequest,
  UpdateEnderecoRequest,
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

  update(id: number, req: UpdateEnderecoRequest): Observable<EnderecoResponse> {
    return this.http.put<EnderecoResponse>(`${this.api}/customers/me/addresses/${id}`, req);
  }

  remove(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/customers/me/addresses/${id}`);
  }

  setDefault(id: number, req: SetDefaultRequest): Observable<EnderecoResponse> {
    return this.http.post<EnderecoResponse>(`${this.api}/customers/me/addresses/${id}/default`, req);
  }

  /** CEP com 8 dígitos numéricos, sem máscara. */
  lookupCep(cep: string): Observable<ViaCepResponse> {
    return this.http.get<ViaCepResponse>(`${this.api}/customers/cep/${cep}`);
  }
}
```

- [ ] **Step 3: Estender `payment-method.service.ts`**

Substituir o conteúdo de `frontend/storefront/src/app/modules/customer/services/payment-method.service.ts` por:

```ts
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '@env/environment';
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

  remove(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/customers/me/payment-methods/${id}`);
  }

  setDefault(id: number): Observable<FormaPagamentoResponse> {
    return this.http.post<FormaPagamentoResponse>(`${this.api}/customers/me/payment-methods/${id}/default`, {});
  }

  /**
   * Trafega PAN + CVV. Deve ser a única request que carrega esses campos.
   */
  tokenize(req: TokenizeCardRequest): Observable<TokenizeCardResponse> {
    return this.http.post<TokenizeCardResponse>(
      `${this.api}/customers/payment-methods/tokenize`,
      req,
    );
  }
}
```

- [ ] **Step 4: Build**

```bash
cd /home/ali/projects/pet-hub/frontend/storefront && npm run build
```

Expected: build sem erros (CheckoutAddressPage e demais consumidores do AddressService.create()/list() continuam funcionando — só ganharam métodos novos).

- [ ] **Step 5: Commit**

```bash
git add frontend/storefront/src/app/modules/customer/models/address.ts \
        frontend/storefront/src/app/modules/customer/services/address.service.ts \
        frontend/storefront/src/app/modules/customer/services/payment-method.service.ts
git commit -m "feat(storefront): slice4 09-extend address and payment services"
```

---

## Task 10: AccountShellPage + rotas filhas

**Files:**
- Create: `frontend/storefront/src/app/modules/customer/pages/account-shell/account-shell.page.ts`
- Create: `frontend/storefront/src/app/modules/customer/pages/account-shell/account-shell.page.html`
- Create: `frontend/storefront/src/app/modules/customer/pages/account-shell/account-shell.page.scss`
- Create: `frontend/storefront/src/app/modules/customer/pages/overview/overview.page.ts` (stub temporário)
- Create: `frontend/storefront/src/app/modules/customer/pages/overview/overview.page.html` (stub temporário)
- Modify: `frontend/storefront/src/app/app.routes.ts`

- [ ] **Step 1: Criar stub `overview.page.ts` (será expandido na Task 11)**

```ts
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-overview-page',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './overview.page.html',
})
export class OverviewPage {}
```

- [ ] **Step 2: Criar stub `overview.page.html`**

```html
<section class="px-4 py-6"><h1 class="text-2xl font-display">Visão geral</h1><p class="mt-2 text-graphite-600">Em construção (Task 11).</p></section>
```

- [ ] **Step 3: Criar `account-shell.page.ts`**

```ts
import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AuthService } from '@core/services/auth.service';

interface ItemMenu {
  rota: string;
  rotuloMenu: string;
  iconeEmoji: string;
  exact?: boolean;
}

@Component({
  selector: 'app-account-shell-page',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './account-shell.page.html',
  styleUrls: ['./account-shell.page.scss'],
})
export class AccountShellPage {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly drawerAberto = signal(false);
  readonly nomeUsuario = this.authService.currentUser;

  readonly itensMenu: ItemMenu[] = [
    { rota: '/minha-conta',           rotuloMenu: 'Visão geral', iconeEmoji: '📋', exact: true },
    { rota: '/minha-conta/perfil',    rotuloMenu: 'Perfil',      iconeEmoji: '👤' },
    { rota: '/minha-conta/enderecos', rotuloMenu: 'Endereços',   iconeEmoji: '📍' },
    { rota: '/minha-conta/cartoes',   rotuloMenu: 'Cartões',     iconeEmoji: '💳' },
    { rota: '/minha-conta/pets',      rotuloMenu: 'Pets',        iconeEmoji: '🐾' },
    { rota: '/minha-conta/pedidos',   rotuloMenu: 'Pedidos',     iconeEmoji: '📦' },
  ];

  abrirDrawer(): void { this.drawerAberto.set(true); }
  fecharDrawer(): void { this.drawerAberto.set(false); }

  sairDaConta(): void {
    this.authService.logout().subscribe({
      complete: () => this.router.navigateByUrl('/'),
    });
  }
}
```

- [ ] **Step 4: Criar `account-shell.page.html`**

```html
<div class="flex min-h-[calc(100vh-9rem)] bg-graphite-50">

  <!-- Botão hambúrguer no mobile (md:hidden) -->
  <button type="button" class="md:hidden fixed top-20 left-4 z-30 rounded-md bg-graphite-0 border border-graphite-200 p-2 shadow-sm"
          (click)="abrirDrawer()" aria-label="Abrir menu da conta">☰</button>

  <!-- Sidebar (md+: visível; mobile: drawer com backdrop) -->
  <aside class="hidden md:flex w-60 flex-col border-r border-graphite-200 bg-graphite-0 px-4 py-6">
    <ng-container *ngTemplateOutlet="conteudoSidebar"></ng-container>
  </aside>

  <!-- Drawer mobile -->
  <div *ngIf="drawerAberto()" class="md:hidden fixed inset-0 z-40 flex" (keydown.escape)="fecharDrawer()">
    <div class="absolute inset-0 bg-graphite-900/40" (click)="fecharDrawer()" aria-hidden="true"></div>
    <aside class="relative flex flex-col w-64 bg-graphite-0 px-4 py-6 shadow-xl animate-slide-in">
      <button type="button" class="self-end mb-4 text-graphite-700" (click)="fecharDrawer()" aria-label="Fechar menu">✕</button>
      <ng-container *ngTemplateOutlet="conteudoSidebar"></ng-container>
    </aside>
  </div>

  <!-- Conteúdo da rota filha -->
  <main class="flex-1 px-4 py-6 md:px-8 md:py-8 max-w-page mx-auto w-full">
    <router-outlet />
  </main>
</div>

<!-- Template reutilizado entre sidebar md+ e drawer mobile -->
<ng-template #conteudoSidebar>
  <p class="text-xs uppercase tracking-wide text-graphite-500 mb-2">Minha conta</p>
  <p class="text-sm font-medium text-graphite-900 mb-6">{{ nomeUsuario()?.nome ?? '' }}</p>
  <nav class="flex-1 flex flex-col gap-1">
    <a *ngFor="let item of itensMenu"
       [routerLink]="item.rota"
       [routerLinkActiveOptions]="{ exact: !!item.exact }"
       routerLinkActive="bg-coral-50 text-coral-700"
       class="flex items-center gap-3 rounded-md px-3 py-2 text-sm text-graphite-700 hover:bg-graphite-100"
       (click)="fecharDrawer()">
      <span aria-hidden="true">{{ item.iconeEmoji }}</span>
      <span>{{ item.rotuloMenu }}</span>
    </a>
  </nav>
  <button type="button" class="mt-4 btn-ghost text-sm py-2" (click)="sairDaConta()">Sair</button>
</ng-template>
```

- [ ] **Step 5: Criar `account-shell.page.scss`**

```scss
@keyframes slide-in {
  from { transform: translateX(-100%); }
  to { transform: translateX(0); }
}
.animate-slide-in { animation: slide-in 200ms ease-out; }
```

- [ ] **Step 6: Atualizar `app.routes.ts`**

Substituir o bloco `{ path: 'minha-conta', ... }` existente por:

```ts
{
  path: 'minha-conta',
  canActivate: [authGuard],
  loadComponent: () =>
    import('@modules/customer/pages/account-shell/account-shell.page')
      .then(moduleShell => moduleShell.AccountShellPage),
  children: [
    {
      path: '',
      loadComponent: () =>
        import('@modules/customer/pages/overview/overview.page')
          .then(moduleOverview => moduleOverview.OverviewPage),
    },
    {
      path: 'perfil',
      loadComponent: () =>
        import('@modules/customer/pages/perfil/perfil.page')
          .then(modulePerfil => modulePerfil.PerfilPage),
    },
    {
      path: 'enderecos',
      loadComponent: () =>
        import('@modules/customer/pages/enderecos/enderecos.page')
          .then(moduleEnderecos => moduleEnderecos.EnderecosPage),
    },
    {
      path: 'cartoes',
      loadComponent: () =>
        import('@modules/customer/pages/cartoes/cartoes.page')
          .then(moduleCartoes => moduleCartoes.CartoesPage),
    },
    {
      path: 'pets',
      loadComponent: () =>
        import('@modules/customer/pages/pets/pets.page')
          .then(modulePets => modulePets.PetsPage),
    },
    {
      path: 'pedidos',
      loadComponent: () =>
        import('@modules/orders/pages/lista/lista.page')
          .then(moduleLista => moduleLista.OrdersListaPage),
    },
    {
      path: 'pedidos/:numero',
      loadComponent: () =>
        import('@modules/orders/pages/detalhe/detalhe.page')
          .then(moduleDetalhe => moduleDetalhe.OrdersDetalhePage),
    },
  ],
},
```

Notar que isso refere componentes que serão criados nas tasks 11-18. Build vai falhar até as páginas existirem como stubs — adicionar stubs vazios nessas pastas como parte desta task pra não bloquear build.

- [ ] **Step 7: Criar stubs vazios para perfil, enderecos, cartoes, pets, orders/lista, orders/detalhe**

Para cada uma das 6 páginas, criar `.ts` + `.html` mínimos. Exemplo para `perfil.page.ts`:

```ts
// frontend/storefront/src/app/modules/customer/pages/perfil/perfil.page.ts
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-perfil-page',
  standalone: true,
  imports: [CommonModule],
  template: `<section class="px-4 py-6"><h1 class="text-2xl font-display">Perfil</h1><p class="mt-2 text-graphite-600">Em construção.</p></section>`,
})
export class PerfilPage {}
```

Repetir o mesmo formato para:
- `modules/customer/pages/enderecos/enderecos.page.ts` (classe `EnderecosPage`, h1 "Endereços")
- `modules/customer/pages/cartoes/cartoes.page.ts` (classe `CartoesPage`, h1 "Cartões")
- `modules/customer/pages/pets/pets.page.ts` (classe `PetsPage`, h1 "Pets")
- `modules/orders/pages/lista/lista.page.ts` (classe `OrdersListaPage`, h1 "Meus pedidos")
- `modules/orders/pages/detalhe/detalhe.page.ts` (classe `OrdersDetalhePage`, h1 "Detalhe do pedido")

Não precisa criar `.html` separado pra esses stubs — o template inline já basta.

- [ ] **Step 8: Build + smoke manual**

```bash
cd /home/ali/projects/pet-hub/frontend/storefront && npm run build
```

Smoke: abrir `http://127.0.0.1:4242/minha-conta` (logado). Deve mostrar shell com sidebar à esquerda em desktop e os 6 itens do menu. Clicar em cada item deve navegar e o item ativo destacar. Logout no rodapé do sidebar deve funcionar. Em viewport `<768px` (DevTools), sidebar some e botão hambúrguer aparece — clicar abre drawer da esquerda; ESC ou clicar fora fecha.

- [ ] **Step 9: Commit**

```bash
git add frontend/storefront/src/app/modules/customer/pages/account-shell \
        frontend/storefront/src/app/modules/customer/pages/overview \
        frontend/storefront/src/app/modules/customer/pages/perfil \
        frontend/storefront/src/app/modules/customer/pages/enderecos \
        frontend/storefront/src/app/modules/customer/pages/cartoes \
        frontend/storefront/src/app/modules/customer/pages/pets \
        frontend/storefront/src/app/modules/orders/pages \
        frontend/storefront/src/app/app.routes.ts
git commit -m "feat(storefront): slice4 10-account shell with sidebar and drawer"
```

---

## Task 11: OverviewPage com 5 cards forkJoin

**Files:**
- Modify: `frontend/storefront/src/app/modules/customer/pages/overview/overview.page.ts`
- Create: `frontend/storefront/src/app/modules/customer/pages/overview/overview.page.html`
- Create: `frontend/storefront/src/app/modules/customer/pages/overview/overview.page.scss`

- [ ] **Step 1: Substituir `overview.page.ts`**

```ts
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { PerfilService } from '@modules/customer/services/perfil.service';
import { AddressService } from '@modules/customer/services/address.service';
import { PaymentMethodService } from '@modules/customer/services/payment-method.service';
import { PetService } from '@modules/customer/services/pet.service';
import { OrderService } from '@modules/orders/services/order.service';
import { PerfilResponse } from '@modules/customer/models/perfil';
import { EnderecoResponse } from '@modules/customer/models/address';
import { FormaPagamentoResponse } from '@modules/customer/models/payment-method';
import { PetResponse, ESPECIES_LABEL } from '@modules/customer/models/pet';
import {
  PedidoResumoResponse,
  STATUS_LABEL,
  classesBadgeStatus,
} from '@modules/orders/models/order';
import { SkeletonComponent } from '@shared/components/skeleton/skeleton.component';

type EstadoCard<T> =
  | { tipo: 'loading' }
  | { tipo: 'sucesso'; dados: T }
  | { tipo: 'erro' };

@Component({
  selector: 'app-overview-page',
  standalone: true,
  imports: [CommonModule, RouterLink, SkeletonComponent],
  templateUrl: './overview.page.html',
  styleUrls: ['./overview.page.scss'],
})
export class OverviewPage implements OnInit {
  private readonly perfilService = inject(PerfilService);
  private readonly addressService = inject(AddressService);
  private readonly paymentMethodService = inject(PaymentMethodService);
  private readonly petService = inject(PetService);
  private readonly orderService = inject(OrderService);

  readonly STATUS_LABEL = STATUS_LABEL;
  readonly ESPECIES_LABEL = ESPECIES_LABEL;
  readonly classesBadge = classesBadgeStatus;

  readonly cardPerfil = signal<EstadoCard<PerfilResponse>>({ tipo: 'loading' });
  readonly cardEnderecos = signal<EstadoCard<EnderecoResponse[]>>({ tipo: 'loading' });
  readonly cardCartoes = signal<EstadoCard<FormaPagamentoResponse[]>>({ tipo: 'loading' });
  readonly cardPets = signal<EstadoCard<PetResponse[]>>({ tipo: 'loading' });
  readonly cardPedidos = signal<EstadoCard<PedidoResumoResponse[]>>({ tipo: 'loading' });

  readonly iniciais = computed(() => {
    const card = this.cardPerfil();
    if (card.tipo !== 'sucesso') return '';
    return card.dados.nome.split(' ').slice(0, 2).map(parte => parte[0] ?? '').join('').toUpperCase();
  });

  ngOnInit(): void {
    this.carregarTudo();
  }

  carregarTudo(): void {
    this.cardPerfil.set({ tipo: 'loading' });
    this.cardEnderecos.set({ tipo: 'loading' });
    this.cardCartoes.set({ tipo: 'loading' });
    this.cardPets.set({ tipo: 'loading' });
    this.cardPedidos.set({ tipo: 'loading' });

    forkJoin({
      perfil:    this.perfilService.get().pipe(catchError(() => of(null))),
      enderecos: this.addressService.list().pipe(catchError(() => of(null))),
      cartoes:   this.paymentMethodService.list().pipe(catchError(() => of(null))),
      pets:      this.petService.list().pipe(catchError(() => of(null))),
      pedidos:   this.orderService.list({ page: 0, size: 3, sort: 'criadoEm,desc' })
                   .pipe(catchError(() => of(null))),
    }).subscribe(resultado => {
      this.cardPerfil.set(resultado.perfil ? { tipo: 'sucesso', dados: resultado.perfil } : { tipo: 'erro' });
      this.cardEnderecos.set(resultado.enderecos ? { tipo: 'sucesso', dados: resultado.enderecos } : { tipo: 'erro' });
      this.cardCartoes.set(resultado.cartoes ? { tipo: 'sucesso', dados: resultado.cartoes } : { tipo: 'erro' });
      this.cardPets.set(resultado.pets ? { tipo: 'sucesso', dados: resultado.pets } : { tipo: 'erro' });
      this.cardPedidos.set(resultado.pedidos ? { tipo: 'sucesso', dados: resultado.pedidos.content } : { tipo: 'erro' });
    });
  }

  recarregarPerfil(): void {
    this.cardPerfil.set({ tipo: 'loading' });
    this.perfilService.get().subscribe({
      next: dados => this.cardPerfil.set({ tipo: 'sucesso', dados }),
      error: () => this.cardPerfil.set({ tipo: 'erro' }),
    });
  }

  recarregarEnderecos(): void {
    this.cardEnderecos.set({ tipo: 'loading' });
    this.addressService.list().subscribe({
      next: dados => this.cardEnderecos.set({ tipo: 'sucesso', dados }),
      error: () => this.cardEnderecos.set({ tipo: 'erro' }),
    });
  }

  recarregarCartoes(): void {
    this.cardCartoes.set({ tipo: 'loading' });
    this.paymentMethodService.list().subscribe({
      next: dados => this.cardCartoes.set({ tipo: 'sucesso', dados }),
      error: () => this.cardCartoes.set({ tipo: 'erro' }),
    });
  }

  recarregarPets(): void {
    this.cardPets.set({ tipo: 'loading' });
    this.petService.list().subscribe({
      next: dados => this.cardPets.set({ tipo: 'sucesso', dados }),
      error: () => this.cardPets.set({ tipo: 'erro' }),
    });
  }

  recarregarPedidos(): void {
    this.cardPedidos.set({ tipo: 'loading' });
    this.orderService.list({ page: 0, size: 3, sort: 'criadoEm,desc' }).subscribe({
      next: pagina => this.cardPedidos.set({ tipo: 'sucesso', dados: pagina.content }),
      error: () => this.cardPedidos.set({ tipo: 'erro' }),
    });
  }
}
```

- [ ] **Step 2: Criar `overview.page.html`**

```html
<section>
  <h1 class="text-2xl font-display font-semibold text-graphite-900">Visão geral</h1>
  <p class="mt-1 text-sm text-graphite-600">Resumo da sua conta — perfil, endereços, cartões, pets e pedidos.</p>

  <div class="mt-6 grid gap-4 grid-cols-1 md:grid-cols-2 lg:grid-cols-3">

    <!-- Perfil -->
    <article class="rounded-lg border border-graphite-200 bg-graphite-0 p-5">
      <header class="flex items-center justify-between">
        <h2 class="text-sm font-medium text-graphite-700">Perfil</h2>
        <a routerLink="/minha-conta/perfil" class="text-xs text-coral-600 hover:text-coral-700">Editar →</a>
      </header>
      <ng-container [ngSwitch]="cardPerfil().tipo">
        <app-skeleton *ngSwitchCase="'loading'" [lines]="3" />
        <div *ngSwitchCase="'erro'" class="mt-3 text-sm text-red-600">
          Não foi possível carregar.
          <button type="button" class="ml-2 text-coral-600 hover:underline" (click)="recarregarPerfil()">Tentar de novo</button>
        </div>
        <ng-container *ngSwitchCase="'sucesso'">
          <div class="mt-4 flex items-center gap-3">
            <div class="h-12 w-12 rounded-full bg-coral-100 text-coral-700 flex items-center justify-center font-semibold">{{ iniciais() }}</div>
            <div>
              <p class="text-sm font-medium text-graphite-900">{{ $any(cardPerfil()).dados.nome }}</p>
              <p class="text-xs text-graphite-500">{{ $any(cardPerfil()).dados.email }}</p>
            </div>
          </div>
        </ng-container>
      </ng-container>
    </article>

    <!-- Endereços -->
    <article class="rounded-lg border border-graphite-200 bg-graphite-0 p-5">
      <header class="flex items-center justify-between">
        <h2 class="text-sm font-medium text-graphite-700">Endereços</h2>
        <a routerLink="/minha-conta/enderecos" class="text-xs text-coral-600 hover:text-coral-700">Gerenciar →</a>
      </header>
      <ng-container [ngSwitch]="cardEnderecos().tipo">
        <app-skeleton *ngSwitchCase="'loading'" [lines]="3" />
        <div *ngSwitchCase="'erro'" class="mt-3 text-sm text-red-600">
          Não foi possível carregar.
          <button type="button" class="ml-2 text-coral-600 hover:underline" (click)="recarregarEnderecos()">Tentar de novo</button>
        </div>
        <ng-container *ngSwitchCase="'sucesso'">
          <p class="mt-3 text-2xl font-display text-graphite-900">{{ $any(cardEnderecos()).dados.length }}</p>
          <p class="text-xs text-graphite-500">cadastrado(s)</p>
          <ng-container *ngFor="let endereco of $any(cardEnderecos()).dados">
            <p *ngIf="endereco.padraoEntrega" class="mt-2 text-xs text-graphite-700">
              ★ {{ endereco.logradouro }}, {{ endereco.numero ?? 's/n' }} · {{ endereco.cidade }}/{{ endereco.uf }}
            </p>
          </ng-container>
        </ng-container>
      </ng-container>
    </article>

    <!-- Cartões -->
    <article class="rounded-lg border border-graphite-200 bg-graphite-0 p-5">
      <header class="flex items-center justify-between">
        <h2 class="text-sm font-medium text-graphite-700">Cartões</h2>
        <a routerLink="/minha-conta/cartoes" class="text-xs text-coral-600 hover:text-coral-700">Gerenciar →</a>
      </header>
      <ng-container [ngSwitch]="cardCartoes().tipo">
        <app-skeleton *ngSwitchCase="'loading'" [lines]="3" />
        <div *ngSwitchCase="'erro'" class="mt-3 text-sm text-red-600">
          Não foi possível carregar.
          <button type="button" class="ml-2 text-coral-600 hover:underline" (click)="recarregarCartoes()">Tentar de novo</button>
        </div>
        <ng-container *ngSwitchCase="'sucesso'">
          <p class="mt-3 text-2xl font-display text-graphite-900">{{ $any(cardCartoes()).dados.length }}</p>
          <p class="text-xs text-graphite-500">cadastrado(s)</p>
          <ng-container *ngFor="let cartao of $any(cardCartoes()).dados">
            <p *ngIf="cartao.padrao" class="mt-2 text-xs text-graphite-700">
              ★ {{ cartao.bandeira ?? cartao.tipo }} •••• {{ cartao.ultimosQuatroDigitos ?? '----' }}
            </p>
          </ng-container>
        </ng-container>
      </ng-container>
    </article>

    <!-- Pets -->
    <article class="rounded-lg border border-graphite-200 bg-graphite-0 p-5">
      <header class="flex items-center justify-between">
        <h2 class="text-sm font-medium text-graphite-700">Pets</h2>
        <a routerLink="/minha-conta/pets" class="text-xs text-coral-600 hover:text-coral-700">Gerenciar →</a>
      </header>
      <ng-container [ngSwitch]="cardPets().tipo">
        <app-skeleton *ngSwitchCase="'loading'" [lines]="3" />
        <div *ngSwitchCase="'erro'" class="mt-3 text-sm text-red-600">
          Não foi possível carregar.
          <button type="button" class="ml-2 text-coral-600 hover:underline" (click)="recarregarPets()">Tentar de novo</button>
        </div>
        <ng-container *ngSwitchCase="'sucesso'">
          <p class="mt-3 text-2xl font-display text-graphite-900">{{ $any(cardPets()).dados.length }}</p>
          <p class="text-xs text-graphite-500">cadastrado(s)</p>
          <p *ngIf="$any(cardPets()).dados[0] as primeiroPet" class="mt-2 text-xs text-graphite-700">
            🐾 {{ primeiroPet.nome }} · {{ ESPECIES_LABEL[primeiroPet.especie] }}
          </p>
        </ng-container>
      </ng-container>
    </article>

    <!-- Pedidos (span 2 colunas em md+) -->
    <article class="rounded-lg border border-graphite-200 bg-graphite-0 p-5 md:col-span-2">
      <header class="flex items-center justify-between">
        <h2 class="text-sm font-medium text-graphite-700">Pedidos recentes</h2>
        <a routerLink="/minha-conta/pedidos" class="text-xs text-coral-600 hover:text-coral-700">Ver todos →</a>
      </header>
      <ng-container [ngSwitch]="cardPedidos().tipo">
        <app-skeleton *ngSwitchCase="'loading'" [lines]="6" />
        <div *ngSwitchCase="'erro'" class="mt-3 text-sm text-red-600">
          Não foi possível carregar.
          <button type="button" class="ml-2 text-coral-600 hover:underline" (click)="recarregarPedidos()">Tentar de novo</button>
        </div>
        <ng-container *ngSwitchCase="'sucesso'">
          <p *ngIf="$any(cardPedidos()).dados.length === 0" class="mt-4 text-sm text-graphite-500">
            Você ainda não fez nenhum pedido.
          </p>
          <ul class="mt-4 divide-y divide-graphite-200">
            <li *ngFor="let pedido of $any(cardPedidos()).dados" class="py-3 flex items-center justify-between gap-3">
              <div class="min-w-0">
                <a [routerLink]="['/minha-conta/pedidos', pedido.numeroPedido]"
                   class="text-sm font-medium text-graphite-900 hover:text-coral-700">{{ pedido.numeroPedido }}</a>
                <p class="text-xs text-graphite-500">{{ pedido.criadoEm | date:'dd/MM/yyyy HH:mm' }} · {{ pedido.totalItens }} item(s)</p>
              </div>
              <div class="text-right shrink-0">
                <span [class]="classesBadge(pedido.status)" class="inline-block rounded-full px-2 py-0.5 text-xs">
                  {{ STATUS_LABEL[pedido.status] }}
                </span>
                <p class="text-sm font-semibold text-graphite-900 mt-1">{{ pedido.valorTotal | currency:'BRL':'symbol':'1.2-2' }}</p>
              </div>
            </li>
          </ul>
        </ng-container>
      </ng-container>
    </article>

  </div>
</section>
```

- [ ] **Step 3: Criar `overview.page.scss` (vazio — só pra existir)**

```scss
/* sem estilos específicos por enquanto; tudo via Tailwind */
```

- [ ] **Step 4: Build + smoke manual**

```bash
cd /home/ali/projects/pet-hub/frontend/storefront && npm run build
```

Smoke: abrir `/minha-conta` logado. Deve mostrar os 5 cards com skeletons → dados ao vivo. Forçar erro num endpoint (parar backend brevemente) verifica que cada card mostra "Tentar de novo" e que outros cards mantêm seus dados.

- [ ] **Step 5: Commit**

```bash
git add frontend/storefront/src/app/modules/customer/pages/overview
git commit -m "feat(storefront): slice4 11-overview page with live cards"
```

---

## Task 12: PerfilPage

**Files:**
- Modify: `frontend/storefront/src/app/modules/customer/pages/perfil/perfil.page.ts`
- Create: `frontend/storefront/src/app/modules/customer/pages/perfil/perfil.page.html`

- [ ] **Step 1: Substituir `perfil.page.ts`**

```ts
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { PerfilService } from '@modules/customer/services/perfil.service';
import { PerfilResponse, Genero, GENEROS_LABEL, UpdatePerfilRequest } from '@modules/customer/models/perfil';
import { SkeletonComponent } from '@shared/components/skeleton/skeleton.component';
import { ToastService } from '@shared/services/toast.service';

@Component({
  selector: 'app-perfil-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, SkeletonComponent],
  templateUrl: './perfil.page.html',
})
export class PerfilPage implements OnInit {
  private readonly perfilService = inject(PerfilService);
  private readonly toast = inject(ToastService);
  private readonly fb = inject(FormBuilder);

  readonly GENEROS_LABEL = GENEROS_LABEL;
  readonly listaGeneros: Genero[] = ['MASCULINO', 'FEMININO', 'NAO_INFORMADO', 'OUTRO'];

  readonly carregando = signal(true);
  readonly salvando = signal(false);
  readonly erroCarregamento = signal(false);
  readonly perfilAtual = signal<PerfilResponse | null>(null);

  readonly formulario = this.fb.nonNullable.group({
    telefoneAdicional: ['', [Validators.pattern(/^\d{10,11}$/)]],
    dataNascimento: [''],
    genero: this.fb.nonNullable.control<Genero | ''>(''),
    aceiteTermos: [false],
    aceiteMarketing: [false],
  });

  ngOnInit(): void { this.carregar(); }

  carregar(): void {
    this.carregando.set(true);
    this.erroCarregamento.set(false);
    this.perfilService.get().subscribe({
      next: dados => {
        this.perfilAtual.set(dados);
        this.formulario.reset({
          telefoneAdicional: dados.telefoneAdicional ?? '',
          dataNascimento: dados.dataNascimento ?? '',
          genero: dados.genero ?? '',
          aceiteTermos: dados.aceiteTermos,
          aceiteMarketing: dados.aceiteMarketing,
        });
        this.carregando.set(false);
      },
      error: () => {
        this.erroCarregamento.set(true);
        this.carregando.set(false);
      },
    });
  }

  salvar(): void {
    if (this.formulario.invalid || !this.formulario.dirty) return;
    this.salvando.set(true);
    const valor = this.formulario.getRawValue();
    const requisicao: UpdatePerfilRequest = {
      telefoneAdicional: valor.telefoneAdicional || null,
      dataNascimento: valor.dataNascimento || null,
      genero: valor.genero === '' ? null : valor.genero as Genero,
      aceiteTermos: valor.aceiteTermos,
      aceiteMarketing: valor.aceiteMarketing,
    };
    this.perfilService.update(requisicao).subscribe({
      next: atualizado => {
        this.perfilAtual.set(atualizado);
        this.formulario.markAsPristine();
        this.salvando.set(false);
        this.toast.success('Perfil atualizado com sucesso.');
      },
      error: (erro: HttpErrorResponse) => {
        this.salvando.set(false);
        const mensagem = erro.error?.detail ?? 'Não foi possível salvar.';
        this.toast.error(mensagem);
      },
    });
  }
}
```

- [ ] **Step 2: Criar `perfil.page.html`**

```html
<section>
  <h1 class="text-2xl font-display font-semibold text-graphite-900">Perfil</h1>
  <p class="mt-1 text-sm text-graphite-600">Atualize seus dados pessoais. Nome, e-mail e CPF não podem ser editados aqui.</p>

  <app-skeleton *ngIf="carregando()" [lines]="6" class="mt-6 block max-w-xl" />

  <div *ngIf="erroCarregamento()" class="mt-6 rounded-md border border-red-200 bg-red-50 p-4 text-sm text-red-700 max-w-xl">
    Não foi possível carregar seu perfil.
    <button type="button" class="ml-2 underline" (click)="carregar()">Tentar de novo</button>
  </div>

  <form *ngIf="!carregando() && !erroCarregamento() && perfilAtual() as perfil"
        [formGroup]="formulario" (ngSubmit)="salvar()" class="mt-6 max-w-xl space-y-4">

    <div>
      <label class="block text-sm font-medium text-graphite-700">Nome</label>
      <input type="text" [value]="perfil.nome" readonly disabled
             class="mt-1 w-full rounded-md border-graphite-200 bg-graphite-50 text-graphite-700" />
    </div>

    <div>
      <label class="block text-sm font-medium text-graphite-700">E-mail</label>
      <input type="email" [value]="perfil.email" readonly disabled
             class="mt-1 w-full rounded-md border-graphite-200 bg-graphite-50 text-graphite-700" />
    </div>

    <div>
      <label class="block text-sm font-medium text-graphite-700">CPF</label>
      <input type="text" [value]="perfil.cpfMascarado ?? 'Não cadastrado'" readonly disabled
             class="mt-1 w-full rounded-md border-graphite-200 bg-graphite-50 text-graphite-700" />
    </div>

    <div>
      <label class="block text-sm font-medium text-graphite-700" for="telefoneAdicional">Telefone</label>
      <input id="telefoneAdicional" type="tel" formControlName="telefoneAdicional" inputmode="numeric"
             placeholder="11912345678" class="mt-1 w-full rounded-md border-graphite-300" />
      <p *ngIf="formulario.controls.telefoneAdicional.errors?.['pattern']" class="text-xs text-red-600 mt-1">
        Telefone deve ter 10 ou 11 dígitos (apenas números).
      </p>
    </div>

    <div>
      <label class="block text-sm font-medium text-graphite-700" for="dataNascimento">Data de nascimento</label>
      <input id="dataNascimento" type="date" formControlName="dataNascimento"
             class="mt-1 w-full rounded-md border-graphite-300" />
    </div>

    <div>
      <label class="block text-sm font-medium text-graphite-700" for="genero">Gênero</label>
      <select id="genero" formControlName="genero" class="mt-1 w-full rounded-md border-graphite-300">
        <option value="">Selecione...</option>
        <option *ngFor="let valor of listaGeneros" [value]="valor">{{ GENEROS_LABEL[valor] }}</option>
      </select>
    </div>

    <label class="flex items-center gap-2 text-sm text-graphite-700">
      <input type="checkbox" formControlName="aceiteTermos" />
      Aceito os termos de uso
    </label>

    <label class="flex items-center gap-2 text-sm text-graphite-700">
      <input type="checkbox" formControlName="aceiteMarketing" />
      Aceito receber comunicações de marketing
    </label>

    <div class="pt-2">
      <button type="submit" class="btn-primary text-sm py-2 px-4"
              [disabled]="formulario.invalid || !formulario.dirty || salvando()">
        {{ salvando() ? 'Salvando...' : 'Salvar alterações' }}
      </button>
    </div>
  </form>
</section>
```

- [ ] **Step 3: Build + smoke manual**

```bash
cd /home/ali/projects/pet-hub/frontend/storefront && npm run build
```

Smoke: abrir `/minha-conta/perfil`. Form carrega valores atuais. Editar telefone com 11 dígitos válidos, salvar → toast verde. Editar com 5 dígitos → erro inline, botão fica disabled.

- [ ] **Step 4: Commit**

```bash
git add frontend/storefront/src/app/modules/customer/pages/perfil
git commit -m "feat(storefront): slice4 12-perfil page with reactive form"
```

---

## Task 13: EnderecosPage com form inline + ViaCEP + confirm

**Files:**
- Modify: `frontend/storefront/src/app/modules/customer/pages/enderecos/enderecos.page.ts`
- Create: `frontend/storefront/src/app/modules/customer/pages/enderecos/enderecos.page.html`

- [ ] **Step 1: Substituir `enderecos.page.ts`**

```ts
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { AddressService } from '@modules/customer/services/address.service';
import {
  EnderecoResponse,
  TipoEndereco,
  UFS,
  UnidadeFederativa,
  CreateEnderecoRequest,
  UpdateEnderecoRequest,
} from '@modules/customer/models/address';
import { SkeletonComponent } from '@shared/components/skeleton/skeleton.component';
import { ToastService } from '@shared/services/toast.service';
import { ConfirmDialogService } from '@shared/services/confirm-dialog.service';

@Component({
  selector: 'app-enderecos-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, SkeletonComponent],
  templateUrl: './enderecos.page.html',
})
export class EnderecosPage implements OnInit {
  private readonly addressService = inject(AddressService);
  private readonly toast = inject(ToastService);
  private readonly confirmDialog = inject(ConfirmDialogService);
  private readonly fb = inject(FormBuilder);

  readonly LISTA_UFS = UFS;
  readonly tiposEndereco: TipoEndereco[] = ['RESIDENCIAL', 'COMERCIAL'];

  readonly carregando = signal(true);
  readonly erroCarregamento = signal(false);
  readonly enderecos = signal<EnderecoResponse[]>([]);
  readonly idEmEdicao = signal<number | null>(null);   // null = nenhum, -1 = novo
  readonly salvando = signal(false);
  readonly buscandoCep = signal(false);

  readonly formulario = this.fb.nonNullable.group({
    apelido: ['', [Validators.required, Validators.maxLength(50)]],
    cep: ['', [Validators.required, Validators.pattern(/^\d{8}$/)]],
    logradouro: ['', [Validators.required, Validators.maxLength(200)]],
    numero: [''],
    complemento: [''],
    bairro: ['', [Validators.required, Validators.maxLength(100)]],
    cidade: ['', [Validators.required, Validators.maxLength(100)]],
    uf: this.fb.nonNullable.control<UnidadeFederativa>('SP', Validators.required),
    tipo: this.fb.nonNullable.control<TipoEndereco>('RESIDENCIAL', Validators.required),
    padraoEntrega: [false],
    padraoCobranca: [false],
  });

  ngOnInit(): void { this.carregar(); }

  carregar(): void {
    this.carregando.set(true);
    this.erroCarregamento.set(false);
    this.addressService.list().subscribe({
      next: lista => {
        this.enderecos.set(lista);
        this.carregando.set(false);
      },
      error: () => {
        this.erroCarregamento.set(true);
        this.carregando.set(false);
      },
    });
  }

  iniciarNovo(): void {
    this.idEmEdicao.set(-1);
    this.formulario.reset({
      apelido: '', cep: '', logradouro: '', numero: '', complemento: '',
      bairro: '', cidade: '', uf: 'SP', tipo: 'RESIDENCIAL',
      padraoEntrega: false, padraoCobranca: false,
    });
  }

  iniciarEdicao(endereco: EnderecoResponse): void {
    this.idEmEdicao.set(endereco.id);
    this.formulario.reset({
      apelido: endereco.apelido,
      cep: endereco.cep,
      logradouro: endereco.logradouro,
      numero: endereco.numero ?? '',
      complemento: endereco.complemento ?? '',
      bairro: endereco.bairro,
      cidade: endereco.cidade,
      uf: endereco.uf,
      tipo: endereco.tipo,
      padraoEntrega: endereco.padraoEntrega,
      padraoCobranca: endereco.padraoCobranca,
    });
  }

  cancelarEdicao(): void { this.idEmEdicao.set(null); }

  buscarCep(): void {
    const valorCep = (this.formulario.controls.cep.value || '').replace(/\D/g, '');
    if (valorCep.length !== 8) return;
    this.buscandoCep.set(true);
    this.addressService.lookupCep(valorCep).subscribe({
      next: viaCep => {
        if (viaCep.erro) {
          this.toast.error('CEP não encontrado.');
        } else {
          this.formulario.patchValue({
            logradouro: viaCep.logradouro,
            bairro: viaCep.bairro,
            cidade: viaCep.cidade,
            uf: viaCep.uf as UnidadeFederativa,
          });
        }
        this.buscandoCep.set(false);
      },
      error: () => {
        this.toast.error('Erro ao consultar CEP.');
        this.buscandoCep.set(false);
      },
    });
  }

  salvar(): void {
    if (this.formulario.invalid) return;
    this.salvando.set(true);
    const valor = this.formulario.getRawValue();
    const idAtual = this.idEmEdicao();

    if (idAtual === -1) {
      const requisicao: CreateEnderecoRequest = {
        apelido: valor.apelido,
        cep: valor.cep,
        logradouro: valor.logradouro,
        numero: valor.numero || undefined,
        complemento: valor.complemento || undefined,
        bairro: valor.bairro,
        cidade: valor.cidade,
        uf: valor.uf,
        tipo: valor.tipo,
        padraoEntrega: valor.padraoEntrega,
        padraoCobranca: valor.padraoCobranca,
      };
      this.addressService.create(requisicao).subscribe({
        next: () => this.aoSalvarComSucesso('Endereço cadastrado.'),
        error: (erro: HttpErrorResponse) => this.aoFalharSalvar(erro),
      });
    } else if (idAtual !== null) {
      const requisicao: UpdateEnderecoRequest = {
        apelido: valor.apelido,
        cep: valor.cep,
        logradouro: valor.logradouro,
        numero: valor.numero,
        complemento: valor.complemento,
        bairro: valor.bairro,
        cidade: valor.cidade,
        uf: valor.uf,
        tipo: valor.tipo,
      };
      this.addressService.update(idAtual, requisicao).subscribe({
        next: () => this.aoSalvarComSucesso('Endereço atualizado.'),
        error: (erro: HttpErrorResponse) => this.aoFalharSalvar(erro),
      });
    }
  }

  private aoSalvarComSucesso(mensagem: string): void {
    this.salvando.set(false);
    this.idEmEdicao.set(null);
    this.toast.success(mensagem);
    this.carregar();
  }

  private aoFalharSalvar(erro: HttpErrorResponse): void {
    this.salvando.set(false);
    this.toast.error(erro.error?.detail ?? 'Não foi possível salvar.');
  }

  async definirComoPadrao(endereco: EnderecoResponse, tipoPadrao: 'entrega' | 'cobranca'): Promise<void> {
    this.addressService.setDefault(endereco.id, tipoPadrao === 'entrega'
      ? { padraoEntrega: true }
      : { padraoCobranca: true }
    ).subscribe({
      next: () => {
        this.toast.success(`Definido como padrão de ${tipoPadrao}.`);
        this.carregar();
      },
      error: () => this.toast.error('Não foi possível definir como padrão.'),
    });
  }

  async remover(endereco: EnderecoResponse): Promise<void> {
    const confirmado = await this.confirmDialog.open({
      titulo: 'Remover endereço?',
      mensagem: `Remover "${endereco.apelido}"? Esta ação não pode ser desfeita.`,
      acaoLabel: 'Remover',
      acaoVariant: 'danger',
    });
    if (!confirmado) return;

    this.addressService.remove(endereco.id).subscribe({
      next: () => {
        this.toast.success('Endereço removido.');
        this.carregar();
      },
      error: () => this.toast.error('Não foi possível remover.'),
    });
  }
}
```

- [ ] **Step 2: Criar `enderecos.page.html`**

```html
<section>
  <header class="flex items-center justify-between">
    <h1 class="text-2xl font-display font-semibold text-graphite-900">Endereços</h1>
    <button type="button" class="btn-primary text-sm py-2 px-4"
            (click)="iniciarNovo()" [disabled]="idEmEdicao() !== null">
      + Adicionar endereço
    </button>
  </header>

  <app-skeleton *ngIf="carregando()" [lines]="4" class="mt-6 block" />

  <div *ngIf="erroCarregamento()" class="mt-6 rounded-md border border-red-200 bg-red-50 p-4 text-sm text-red-700">
    Não foi possível carregar seus endereços.
    <button type="button" class="ml-2 underline" (click)="carregar()">Tentar de novo</button>
  </div>

  <!-- Form de novo endereço (no topo, quando ativo) -->
  <article *ngIf="idEmEdicao() === -1" class="mt-6 rounded-lg border-2 border-coral-300 bg-coral-50/30 p-5">
    <h2 class="text-base font-medium text-graphite-900">Novo endereço</h2>
    <ng-container *ngTemplateOutlet="formularioEndereco"></ng-container>
  </article>

  <ul class="mt-6 grid gap-4">
    <li *ngFor="let endereco of enderecos()" class="rounded-lg border border-graphite-200 bg-graphite-0 p-5">
      <ng-container *ngIf="idEmEdicao() === endereco.id; else cardSomenteLeitura">
        <h2 class="text-base font-medium text-graphite-900">Editando: {{ endereco.apelido }}</h2>
        <ng-container *ngTemplateOutlet="formularioEndereco"></ng-container>
      </ng-container>

      <ng-template #cardSomenteLeitura>
        <div class="flex items-start justify-between gap-3">
          <div class="min-w-0">
            <p class="text-sm font-medium text-graphite-900">{{ endereco.apelido }}</p>
            <p class="mt-1 text-sm text-graphite-700">
              {{ endereco.logradouro }}, {{ endereco.numero ?? 's/n' }}
              <span *ngIf="endereco.complemento"> · {{ endereco.complemento }}</span>
            </p>
            <p class="text-sm text-graphite-700">
              {{ endereco.bairro }} · {{ endereco.cidade }} / {{ endereco.uf }} · CEP {{ endereco.cep }}
            </p>
            <p class="mt-2 flex gap-2 text-xs">
              <span *ngIf="endereco.padraoEntrega" class="rounded-full bg-emerald-100 text-emerald-800 px-2 py-0.5">★ Padrão entrega</span>
              <span *ngIf="endereco.padraoCobranca" class="rounded-full bg-blue-100 text-blue-800 px-2 py-0.5">★ Padrão cobrança</span>
            </p>
          </div>
          <div class="flex flex-col gap-2 shrink-0">
            <button type="button" class="btn-ghost text-xs py-1 px-2" (click)="iniciarEdicao(endereco)">Editar</button>
            <button *ngIf="!endereco.padraoEntrega" type="button" class="btn-ghost text-xs py-1 px-2"
                    (click)="definirComoPadrao(endereco, 'entrega')">Padrão entrega</button>
            <button *ngIf="!endereco.padraoCobranca" type="button" class="btn-ghost text-xs py-1 px-2"
                    (click)="definirComoPadrao(endereco, 'cobranca')">Padrão cobrança</button>
            <button type="button" class="text-xs py-1 px-2 text-red-600 hover:text-red-800" (click)="remover(endereco)">Remover</button>
          </div>
        </div>
      </ng-template>
    </li>
  </ul>

  <p *ngIf="!carregando() && !erroCarregamento() && enderecos().length === 0 && idEmEdicao() !== -1"
     class="mt-6 text-sm text-graphite-500">
    Você ainda não cadastrou endereços.
  </p>
</section>

<!-- Template do formulário compartilhado entre novo e edição -->
<ng-template #formularioEndereco>
  <form [formGroup]="formulario" (ngSubmit)="salvar()" class="mt-4 grid gap-3 md:grid-cols-2">
    <div class="md:col-span-2">
      <label class="text-xs text-graphite-600">Apelido</label>
      <input type="text" formControlName="apelido" class="mt-1 w-full rounded-md border-graphite-300" />
    </div>

    <div>
      <label class="text-xs text-graphite-600">CEP</label>
      <div class="mt-1 flex gap-2">
        <input type="text" formControlName="cep" inputmode="numeric" maxlength="8" placeholder="01310100"
               class="flex-1 rounded-md border-graphite-300" (blur)="buscarCep()" />
        <button type="button" class="btn-ghost text-xs px-3" (click)="buscarCep()" [disabled]="buscandoCep()">
          {{ buscandoCep() ? '...' : 'Buscar' }}
        </button>
      </div>
    </div>

    <div>
      <label class="text-xs text-graphite-600">UF</label>
      <select formControlName="uf" class="mt-1 w-full rounded-md border-graphite-300">
        <option *ngFor="let uf of LISTA_UFS" [value]="uf">{{ uf }}</option>
      </select>
    </div>

    <div class="md:col-span-2">
      <label class="text-xs text-graphite-600">Logradouro</label>
      <input type="text" formControlName="logradouro" class="mt-1 w-full rounded-md border-graphite-300" />
    </div>

    <div>
      <label class="text-xs text-graphite-600">Número</label>
      <input type="text" formControlName="numero" class="mt-1 w-full rounded-md border-graphite-300" />
    </div>

    <div>
      <label class="text-xs text-graphite-600">Complemento</label>
      <input type="text" formControlName="complemento" class="mt-1 w-full rounded-md border-graphite-300" />
    </div>

    <div>
      <label class="text-xs text-graphite-600">Bairro</label>
      <input type="text" formControlName="bairro" class="mt-1 w-full rounded-md border-graphite-300" />
    </div>

    <div>
      <label class="text-xs text-graphite-600">Cidade</label>
      <input type="text" formControlName="cidade" class="mt-1 w-full rounded-md border-graphite-300" />
    </div>

    <div>
      <label class="text-xs text-graphite-600">Tipo</label>
      <select formControlName="tipo" class="mt-1 w-full rounded-md border-graphite-300">
        <option *ngFor="let tipo of tiposEndereco" [value]="tipo">{{ tipo }}</option>
      </select>
    </div>

    <label class="flex items-center gap-2 text-sm md:col-span-2" *ngIf="idEmEdicao() === -1">
      <input type="checkbox" formControlName="padraoEntrega" /> Definir como padrão de entrega
    </label>
    <label class="flex items-center gap-2 text-sm md:col-span-2" *ngIf="idEmEdicao() === -1">
      <input type="checkbox" formControlName="padraoCobranca" /> Definir como padrão de cobrança
    </label>

    <div class="md:col-span-2 flex gap-3 pt-2">
      <button type="submit" class="btn-primary text-sm py-2 px-4" [disabled]="formulario.invalid || salvando()">
        {{ salvando() ? 'Salvando...' : 'Salvar' }}
      </button>
      <button type="button" class="btn-ghost text-sm py-2 px-4" (click)="cancelarEdicao()">Cancelar</button>
    </div>
  </form>
</ng-template>
```

- [ ] **Step 3: Build + smoke manual**

```bash
cd /home/ali/projects/pet-hub/frontend/storefront && npm run build
```

Smoke: criar, editar, remover endereço; ViaCEP autofill funciona; confirm dialog aparece no delete; setDefault funciona.

- [ ] **Step 4: Commit**

```bash
git add frontend/storefront/src/app/modules/customer/pages/enderecos
git commit -m "feat(storefront): slice4 13-enderecos page with inline form"
```

---

## Task 14: CartoesPage com tokenização

**Files:**
- Modify: `frontend/storefront/src/app/modules/customer/pages/cartoes/cartoes.page.ts`
- Create: `frontend/storefront/src/app/modules/customer/pages/cartoes/cartoes.page.html`

- [ ] **Step 1: Substituir `cartoes.page.ts`**

```ts
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { PaymentMethodService } from '@modules/customer/services/payment-method.service';
import { FormaPagamentoResponse, CreateFormaPagamentoRequest } from '@modules/customer/models/payment-method';
import { SkeletonComponent } from '@shared/components/skeleton/skeleton.component';
import { ToastService } from '@shared/services/toast.service';
import { ConfirmDialogService } from '@shared/services/confirm-dialog.service';

@Component({
  selector: 'app-cartoes-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, SkeletonComponent],
  templateUrl: './cartoes.page.html',
})
export class CartoesPage implements OnInit {
  private readonly pm = inject(PaymentMethodService);
  private readonly toast = inject(ToastService);
  private readonly confirmDialog = inject(ConfirmDialogService);
  private readonly fb = inject(FormBuilder);

  readonly carregando = signal(true);
  readonly erroCarregamento = signal(false);
  readonly cartoes = signal<FormaPagamentoResponse[]>([]);
  readonly mostrandoFormulario = signal(false);
  readonly salvando = signal(false);
  readonly anosValidade = Array.from({ length: 15 }, (_, indice) => new Date().getFullYear() + indice);
  readonly mesesValidade = Array.from({ length: 12 }, (_, indice) => indice + 1);

  readonly formularioCartao = this.fb.nonNullable.group({
    numero: ['', [Validators.required, Validators.pattern(/^\d{13,19}$/)]],
    nomeImpresso: ['', [Validators.required, Validators.maxLength(50)]],
    validadeMes: this.fb.nonNullable.control<number>(1, [Validators.required, Validators.min(1), Validators.max(12)]),
    validadeAno: this.fb.nonNullable.control<number>(new Date().getFullYear(), Validators.required),
    cvv: ['', [Validators.required, Validators.pattern(/^\d{3,4}$/)]],
    apelido: [''],
    padrao: [false],
  });

  ngOnInit(): void { this.carregar(); }

  carregar(): void {
    this.carregando.set(true);
    this.erroCarregamento.set(false);
    this.pm.list().subscribe({
      next: lista => { this.cartoes.set(lista); this.carregando.set(false); },
      error: () => { this.erroCarregamento.set(true); this.carregando.set(false); },
    });
  }

  iniciarAdicao(): void {
    this.mostrandoFormulario.set(true);
    this.formularioCartao.reset({
      numero: '', nomeImpresso: '', validadeMes: 1,
      validadeAno: new Date().getFullYear(), cvv: '', apelido: '', padrao: false,
    });
  }

  cancelarAdicao(): void { this.mostrandoFormulario.set(false); }

  salvar(): void {
    if (this.formularioCartao.invalid) return;
    this.salvando.set(true);
    const valor = this.formularioCartao.getRawValue();

    this.pm.tokenize({
      numero: valor.numero,
      cvv: valor.cvv,
      nomeImpresso: valor.nomeImpresso,
      validadeMes: valor.validadeMes,
      validadeAno: valor.validadeAno,
    }).subscribe({
      next: tokenResp => {
        // Limpar PAN/CVV IMEDIATAMENTE antes do create encadeado
        this.formularioCartao.patchValue({ numero: '', cvv: '' });

        const requisicao: CreateFormaPagamentoRequest = {
          tipo: 'CARTAO_CREDITO',
          apelido: valor.apelido || undefined,
          gatewayToken: tokenResp.token,
          bandeira: tokenResp.bandeira,
          ultimosQuatroDigitos: tokenResp.ultimosQuatroDigitos,
          nomeImpresso: valor.nomeImpresso,
          validadeMes: valor.validadeMes,
          validadeAno: valor.validadeAno,
          padrao: valor.padrao,
        };
        this.pm.create(requisicao).subscribe({
          next: () => {
            this.salvando.set(false);
            this.mostrandoFormulario.set(false);
            this.toast.success('Cartão cadastrado.');
            this.carregar();
          },
          error: (erro: HttpErrorResponse) => {
            this.salvando.set(false);
            this.toast.error(erro.error?.detail ?? 'Não foi possível salvar cartão.');
          },
        });
      },
      error: (erro: HttpErrorResponse) => {
        this.salvando.set(false);
        this.toast.error(erro.error?.detail ?? 'Não foi possível tokenizar cartão.');
      },
    });
  }

  tornarPadrao(cartao: FormaPagamentoResponse): void {
    this.pm.setDefault(cartao.id).subscribe({
      next: () => { this.toast.success('Cartão definido como padrão.'); this.carregar(); },
      error: () => this.toast.error('Não foi possível definir como padrão.'),
    });
  }

  async remover(cartao: FormaPagamentoResponse): Promise<void> {
    const confirmado = await this.confirmDialog.open({
      titulo: 'Remover cartão?',
      mensagem: `Remover cartão ${cartao.bandeira ?? cartao.tipo} •••• ${cartao.ultimosQuatroDigitos ?? '----'}?`,
      acaoLabel: 'Remover',
      acaoVariant: 'danger',
    });
    if (!confirmado) return;

    this.pm.remove(cartao.id).subscribe({
      next: () => { this.toast.success('Cartão removido.'); this.carregar(); },
      error: () => this.toast.error('Não foi possível remover.'),
    });
  }
}
```

- [ ] **Step 2: Criar `cartoes.page.html`**

```html
<section>
  <header class="flex items-center justify-between">
    <h1 class="text-2xl font-display font-semibold text-graphite-900">Cartões</h1>
    <button type="button" class="btn-primary text-sm py-2 px-4"
            (click)="iniciarAdicao()" [disabled]="mostrandoFormulario()">
      + Adicionar cartão
    </button>
  </header>

  <app-skeleton *ngIf="carregando()" [lines]="3" class="mt-6 block" />

  <div *ngIf="erroCarregamento()" class="mt-6 rounded-md border border-red-200 bg-red-50 p-4 text-sm text-red-700">
    Não foi possível carregar.
    <button type="button" class="ml-2 underline" (click)="carregar()">Tentar de novo</button>
  </div>

  <article *ngIf="mostrandoFormulario()" class="mt-6 rounded-lg border-2 border-coral-300 bg-coral-50/30 p-5">
    <h2 class="text-base font-medium text-graphite-900">Novo cartão</h2>
    <form [formGroup]="formularioCartao" (ngSubmit)="salvar()" class="mt-4 grid gap-3 md:grid-cols-2" autocomplete="on">

      <div class="md:col-span-2">
        <label class="text-xs text-graphite-600">Número do cartão</label>
        <input type="text" formControlName="numero" inputmode="numeric" autocomplete="cc-number"
               maxlength="19" class="mt-1 w-full rounded-md border-graphite-300" />
      </div>

      <div class="md:col-span-2">
        <label class="text-xs text-graphite-600">Nome impresso no cartão</label>
        <input type="text" formControlName="nomeImpresso" autocomplete="cc-name"
               class="mt-1 w-full rounded-md border-graphite-300" />
      </div>

      <div>
        <label class="text-xs text-graphite-600">Mês</label>
        <select formControlName="validadeMes" autocomplete="cc-exp-month" class="mt-1 w-full rounded-md border-graphite-300">
          <option *ngFor="let mes of mesesValidade" [value]="mes">{{ mes }}</option>
        </select>
      </div>

      <div>
        <label class="text-xs text-graphite-600">Ano</label>
        <select formControlName="validadeAno" autocomplete="cc-exp-year" class="mt-1 w-full rounded-md border-graphite-300">
          <option *ngFor="let ano of anosValidade" [value]="ano">{{ ano }}</option>
        </select>
      </div>

      <div>
        <label class="text-xs text-graphite-600">CVV</label>
        <input type="password" formControlName="cvv" inputmode="numeric" autocomplete="cc-csc" maxlength="4"
               class="mt-1 w-full rounded-md border-graphite-300" />
      </div>

      <div>
        <label class="text-xs text-graphite-600">Apelido (opcional)</label>
        <input type="text" formControlName="apelido" class="mt-1 w-full rounded-md border-graphite-300" />
      </div>

      <label class="md:col-span-2 flex items-center gap-2 text-sm text-graphite-700">
        <input type="checkbox" formControlName="padrao" /> Definir como cartão padrão
      </label>

      <div class="md:col-span-2 flex gap-3 pt-2">
        <button type="submit" class="btn-primary text-sm py-2 px-4" [disabled]="formularioCartao.invalid || salvando()">
          {{ salvando() ? 'Salvando...' : 'Salvar' }}
        </button>
        <button type="button" class="btn-ghost text-sm py-2 px-4" (click)="cancelarAdicao()">Cancelar</button>
      </div>
    </form>
  </article>

  <ul class="mt-6 grid gap-4">
    <li *ngFor="let cartao of cartoes()" class="rounded-lg border border-graphite-200 bg-graphite-0 p-5 flex items-start justify-between gap-3">
      <div>
        <p class="text-sm font-medium text-graphite-900">
          💳 {{ cartao.bandeira ?? cartao.tipo }} •••• {{ cartao.ultimosQuatroDigitos ?? '----' }}
        </p>
        <p *ngIf="cartao.nomeImpresso" class="text-xs text-graphite-700 mt-1">
          {{ cartao.nomeImpresso }}
          <span *ngIf="cartao.validadeMes && cartao.validadeAno">
            · {{ cartao.validadeMes }}/{{ cartao.validadeAno }}
          </span>
        </p>
        <p *ngIf="cartao.padrao" class="mt-2 text-xs">
          <span class="rounded-full bg-emerald-100 text-emerald-800 px-2 py-0.5">★ Padrão</span>
        </p>
      </div>
      <div class="flex flex-col gap-2 shrink-0">
        <button *ngIf="!cartao.padrao" type="button" class="btn-ghost text-xs py-1 px-2"
                (click)="tornarPadrao(cartao)">Tornar padrão</button>
        <button type="button" class="text-xs py-1 px-2 text-red-600 hover:text-red-800" (click)="remover(cartao)">Remover</button>
      </div>
    </li>
  </ul>

  <p *ngIf="!carregando() && !erroCarregamento() && cartoes().length === 0 && !mostrandoFormulario()"
     class="mt-6 text-sm text-graphite-500">
    Você ainda não cadastrou cartões.
  </p>
</section>
```

- [ ] **Step 3: Build + smoke manual**

```bash
cd /home/ali/projects/pet-hub/frontend/storefront && npm run build
```

Smoke: cadastrar cartão com `4111 1111 1111 1111` aprova; após sucesso, abrir DevTools e verificar que campos `numero`/`cvv` do form estão vazios (limpos imediatamente). Tornar padrão e remover funcionam.

- [ ] **Step 4: Commit**

```bash
git add frontend/storefront/src/app/modules/customer/pages/cartoes
git commit -m "feat(storefront): slice4 14-cartoes page with tokenization"
```

---

## Task 15: PetsPage com upload de foto

**Files:**
- Modify: `frontend/storefront/src/app/modules/customer/pages/pets/pets.page.ts`
- Create: `frontend/storefront/src/app/modules/customer/pages/pets/pets.page.html`

- [ ] **Step 1: Substituir `pets.page.ts`**

```ts
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

import { PetService } from '@modules/customer/services/pet.service';
import {
  PetResponse,
  Especie,
  Porte,
  ESPECIES_LABEL,
  PORTES_LABEL,
  CreatePetRequest,
  UpdatePetRequest,
} from '@modules/customer/models/pet';
import { SkeletonComponent } from '@shared/components/skeleton/skeleton.component';
import { ToastService } from '@shared/services/toast.service';
import { ConfirmDialogService } from '@shared/services/confirm-dialog.service';

const TAMANHO_MAX_FOTO_BYTES = 5 * 1024 * 1024;

@Component({
  selector: 'app-pets-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, SkeletonComponent],
  templateUrl: './pets.page.html',
})
export class PetsPage implements OnInit {
  private readonly petService = inject(PetService);
  private readonly toast = inject(ToastService);
  private readonly confirmDialog = inject(ConfirmDialogService);
  private readonly fb = inject(FormBuilder);

  readonly ESPECIES_LABEL = ESPECIES_LABEL;
  readonly PORTES_LABEL = PORTES_LABEL;
  readonly listaEspecies: Especie[] = ['CACHORRO', 'GATO', 'AVE', 'PEIXE', 'REPTIL', 'OUTROS'];
  readonly listaPortes: Porte[] = ['PEQUENO', 'MEDIO', 'GRANDE', 'GIGANTE'];

  readonly carregando = signal(true);
  readonly erroCarregamento = signal(false);
  readonly pets = signal<PetResponse[]>([]);
  readonly idEmEdicao = signal<number | null>(null);  // null = nenhum; -1 = novo
  readonly salvando = signal(false);
  readonly arquivoFoto = signal<File | null>(null);
  readonly previewFotoUrl = signal<string | null>(null);

  readonly formulario = this.fb.nonNullable.group({
    nome: ['', [Validators.required, Validators.maxLength(100)]],
    especie: this.fb.nonNullable.control<Especie>('CACHORRO', Validators.required),
    raca: [''],
    dataNascimento: [''],
    pesoKg: this.fb.nonNullable.control<number | null>(null),
    porte: this.fb.nonNullable.control<Porte | ''>(''),
    observacoes: [''],
  });

  ngOnInit(): void { this.carregar(); }

  carregar(): void {
    this.carregando.set(true);
    this.erroCarregamento.set(false);
    this.petService.list().subscribe({
      next: lista => { this.pets.set(lista); this.carregando.set(false); },
      error: () => { this.erroCarregamento.set(true); this.carregando.set(false); },
    });
  }

  iniciarNovo(): void {
    this.idEmEdicao.set(-1);
    this.formulario.reset({
      nome: '', especie: 'CACHORRO', raca: '', dataNascimento: '',
      pesoKg: null, porte: '', observacoes: '',
    });
    this.arquivoFoto.set(null);
    this.previewFotoUrl.set(null);
  }

  iniciarEdicao(pet: PetResponse): void {
    this.idEmEdicao.set(pet.id);
    this.formulario.reset({
      nome: pet.nome,
      especie: pet.especie,
      raca: pet.raca ?? '',
      dataNascimento: pet.dataNascimento ?? '',
      pesoKg: pet.pesoKg,
      porte: pet.porte ?? '',
      observacoes: pet.observacoes ?? '',
    });
    this.arquivoFoto.set(null);
    this.previewFotoUrl.set(null);
  }

  cancelarEdicao(): void {
    this.idEmEdicao.set(null);
    this.previewFotoUrl.set(null);
  }

  aoSelecionarArquivo(eventoInput: Event): void {
    const input = eventoInput.target as HTMLInputElement;
    const arquivo = input.files?.[0] ?? null;
    if (!arquivo) {
      this.arquivoFoto.set(null);
      this.previewFotoUrl.set(null);
      return;
    }
    if (arquivo.size > TAMANHO_MAX_FOTO_BYTES) {
      this.toast.error('Foto não pode passar de 5 MB.');
      input.value = '';
      return;
    }
    this.arquivoFoto.set(arquivo);
    this.previewFotoUrl.set(URL.createObjectURL(arquivo));
  }

  salvar(): void {
    if (this.formulario.invalid) return;
    this.salvando.set(true);
    const valor = this.formulario.getRawValue();
    const idAtual = this.idEmEdicao();

    const dadosBase = {
      nome: valor.nome,
      especie: valor.especie,
      raca: valor.raca || undefined,
      dataNascimento: valor.dataNascimento || undefined,
      pesoKg: valor.pesoKg ?? undefined,
      porte: valor.porte === '' ? undefined : valor.porte as Porte,
      observacoes: valor.observacoes || undefined,
    };

    if (idAtual === -1) {
      const requisicao: CreatePetRequest = dadosBase as CreatePetRequest;
      this.petService.create(requisicao).subscribe({
        next: petCriado => this.aoSalvarPetSuccesso(petCriado, 'Pet cadastrado.'),
        error: (erro: HttpErrorResponse) => this.aoFalharSalvar(erro),
      });
    } else if (idAtual !== null) {
      const requisicao: UpdatePetRequest = dadosBase;
      this.petService.update(idAtual, requisicao).subscribe({
        next: petAtualizado => this.aoSalvarPetSuccesso(petAtualizado, 'Pet atualizado.'),
        error: (erro: HttpErrorResponse) => this.aoFalharSalvar(erro),
      });
    }
  }

  private aoSalvarPetSuccesso(pet: PetResponse, mensagem: string): void {
    const arquivo = this.arquivoFoto();
    if (arquivo) {
      this.petService.uploadPhoto(pet.id, arquivo).subscribe({
        next: () => this.finalizarSalvar(mensagem),
        error: () => {
          this.toast.error('Pet salvo, mas não foi possível enviar a foto.');
          this.finalizarSalvar(mensagem);
        },
      });
    } else {
      this.finalizarSalvar(mensagem);
    }
  }

  private finalizarSalvar(mensagem: string): void {
    this.salvando.set(false);
    this.idEmEdicao.set(null);
    this.arquivoFoto.set(null);
    this.previewFotoUrl.set(null);
    this.toast.success(mensagem);
    this.carregar();
  }

  private aoFalharSalvar(erro: HttpErrorResponse): void {
    this.salvando.set(false);
    this.toast.error(erro.error?.detail ?? 'Não foi possível salvar.');
  }

  async remover(pet: PetResponse): Promise<void> {
    const confirmado = await this.confirmDialog.open({
      titulo: 'Remover pet?',
      mensagem: `Remover "${pet.nome}"? Esta ação não pode ser desfeita.`,
      acaoLabel: 'Remover',
      acaoVariant: 'danger',
    });
    if (!confirmado) return;

    this.petService.remove(pet.id).subscribe({
      next: () => { this.toast.success('Pet removido.'); this.carregar(); },
      error: () => this.toast.error('Não foi possível remover.'),
    });
  }

  calcularIdade(dataNascimentoIso: string | null): string {
    if (!dataNascimentoIso) return '';
    const nascimento = new Date(dataNascimentoIso);
    const agora = new Date();
    let anos = agora.getFullYear() - nascimento.getFullYear();
    const m = agora.getMonth() - nascimento.getMonth();
    if (m < 0 || (m === 0 && agora.getDate() < nascimento.getDate())) anos--;
    if (anos <= 0) return 'menos de 1 ano';
    return anos === 1 ? '1 ano' : `${anos} anos`;
  }
}
```

- [ ] **Step 2: Criar `pets.page.html`**

```html
<section>
  <header class="flex items-center justify-between">
    <h1 class="text-2xl font-display font-semibold text-graphite-900">Pets</h1>
    <button type="button" class="btn-primary text-sm py-2 px-4"
            (click)="iniciarNovo()" [disabled]="idEmEdicao() !== null">
      + Adicionar pet
    </button>
  </header>

  <app-skeleton *ngIf="carregando()" [lines]="4" class="mt-6 block" />

  <div *ngIf="erroCarregamento()" class="mt-6 rounded-md border border-red-200 bg-red-50 p-4 text-sm text-red-700">
    Não foi possível carregar.
    <button type="button" class="ml-2 underline" (click)="carregar()">Tentar de novo</button>
  </div>

  <article *ngIf="idEmEdicao() === -1" class="mt-6 rounded-lg border-2 border-coral-300 bg-coral-50/30 p-5">
    <h2 class="text-base font-medium text-graphite-900">Novo pet</h2>
    <ng-container *ngTemplateOutlet="formularioPet"></ng-container>
  </article>

  <ul class="mt-6 grid gap-4 md:grid-cols-2">
    <li *ngFor="let pet of pets()" class="rounded-lg border border-graphite-200 bg-graphite-0 p-5">
      <ng-container *ngIf="idEmEdicao() === pet.id; else cardSomenteLeitura">
        <h2 class="text-base font-medium text-graphite-900">Editando: {{ pet.nome }}</h2>
        <ng-container *ngTemplateOutlet="formularioPet"></ng-container>
      </ng-container>

      <ng-template #cardSomenteLeitura>
        <div class="flex gap-4">
          <img *ngIf="pet.fotoUrl; else semFoto" [src]="pet.fotoUrl" alt="Foto de {{ pet.nome }}"
               class="h-20 w-20 rounded-full object-cover" />
          <ng-template #semFoto>
            <div class="h-20 w-20 rounded-full bg-graphite-100 flex items-center justify-center text-2xl" aria-hidden="true">🐾</div>
          </ng-template>
          <div class="flex-1 min-w-0">
            <p class="text-sm font-medium text-graphite-900">{{ pet.nome }}</p>
            <p class="text-xs text-graphite-700">
              {{ ESPECIES_LABEL[pet.especie] }}
              <span *ngIf="pet.raca"> · {{ pet.raca }}</span>
            </p>
            <p class="text-xs text-graphite-500 mt-1">
              <span *ngIf="pet.dataNascimento">{{ calcularIdade(pet.dataNascimento) }}</span>
              <span *ngIf="pet.pesoKg"> · {{ pet.pesoKg }} kg</span>
              <span *ngIf="pet.porte"> · {{ PORTES_LABEL[pet.porte] }}</span>
            </p>
          </div>
          <div class="flex flex-col gap-2 shrink-0">
            <button type="button" class="btn-ghost text-xs py-1 px-2" (click)="iniciarEdicao(pet)">Editar</button>
            <button type="button" class="text-xs py-1 px-2 text-red-600 hover:text-red-800" (click)="remover(pet)">Remover</button>
          </div>
        </div>
      </ng-template>
    </li>
  </ul>

  <p *ngIf="!carregando() && !erroCarregamento() && pets().length === 0 && idEmEdicao() !== -1"
     class="mt-6 text-sm text-graphite-500">
    Você ainda não cadastrou pets.
  </p>
</section>

<ng-template #formularioPet>
  <form [formGroup]="formulario" (ngSubmit)="salvar()" class="mt-4 grid gap-3 md:grid-cols-2">
    <div class="md:col-span-2">
      <label class="text-xs text-graphite-600">Nome</label>
      <input type="text" formControlName="nome" class="mt-1 w-full rounded-md border-graphite-300" />
    </div>

    <div>
      <label class="text-xs text-graphite-600">Espécie</label>
      <select formControlName="especie" class="mt-1 w-full rounded-md border-graphite-300">
        <option *ngFor="let valor of listaEspecies" [value]="valor">{{ ESPECIES_LABEL[valor] }}</option>
      </select>
    </div>

    <div>
      <label class="text-xs text-graphite-600">Raça</label>
      <input type="text" formControlName="raca" class="mt-1 w-full rounded-md border-graphite-300" />
    </div>

    <div>
      <label class="text-xs text-graphite-600">Data de nascimento</label>
      <input type="date" formControlName="dataNascimento" class="mt-1 w-full rounded-md border-graphite-300" />
    </div>

    <div>
      <label class="text-xs text-graphite-600">Peso (kg)</label>
      <input type="number" formControlName="pesoKg" min="0.01" max="999.99" step="0.01"
             class="mt-1 w-full rounded-md border-graphite-300" />
    </div>

    <div>
      <label class="text-xs text-graphite-600">Porte</label>
      <select formControlName="porte" class="mt-1 w-full rounded-md border-graphite-300">
        <option value="">Não informado</option>
        <option *ngFor="let valor of listaPortes" [value]="valor">{{ PORTES_LABEL[valor] }}</option>
      </select>
    </div>

    <div class="md:col-span-2">
      <label class="text-xs text-graphite-600">Observações</label>
      <textarea formControlName="observacoes" rows="3" maxlength="2000"
                class="mt-1 w-full rounded-md border-graphite-300"></textarea>
    </div>

    <div class="md:col-span-2">
      <label class="text-xs text-graphite-600">Foto (máx 5 MB)</label>
      <input type="file" accept="image/*" (change)="aoSelecionarArquivo($event)"
             class="mt-1 block text-sm" />
      <img *ngIf="previewFotoUrl()" [src]="previewFotoUrl()" alt="Preview"
           class="mt-2 h-24 w-24 rounded-md object-cover" />
    </div>

    <div class="md:col-span-2 flex gap-3 pt-2">
      <button type="submit" class="btn-primary text-sm py-2 px-4" [disabled]="formulario.invalid || salvando()">
        {{ salvando() ? 'Salvando...' : 'Salvar' }}
      </button>
      <button type="button" class="btn-ghost text-sm py-2 px-4" (click)="cancelarEdicao()">Cancelar</button>
    </div>
  </form>
</ng-template>
```

- [ ] **Step 3: Build + smoke manual**

```bash
cd /home/ali/projects/pet-hub/frontend/storefront && npm run build
```

Smoke: criar pet com foto (preview aparece, upload acontece); editar; remover com confirm.

- [ ] **Step 4: Commit**

```bash
git add frontend/storefront/src/app/modules/customer/pages/pets
git commit -m "feat(storefront): slice4 15-pets page with photo upload"
```

---

## Task 16: OrderTimelineComponent (destaque visual)

**Files:**
- Create: `frontend/storefront/src/app/modules/orders/components/order-timeline/order-timeline.component.ts`
- Create: `frontend/storefront/src/app/modules/orders/components/order-timeline/order-timeline.component.html`
- Create: `frontend/storefront/src/app/modules/orders/components/order-timeline/order-timeline.component.scss`

- [ ] **Step 1: Criar `order-timeline.component.ts`**

```ts
import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Timeline, EtapaTimeline } from '@modules/orders/models/order';

@Component({
  selector: 'app-order-timeline',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './order-timeline.component.html',
  styleUrls: ['./order-timeline.component.scss'],
})
export class OrderTimelineComponent {
  @Input({ required: true }) timeline!: Timeline;
  @Input() compact = false;

  ehTerminalNegativo(etapa: EtapaTimeline): boolean {
    const nomeNormalizado = etapa.nome.toLowerCase();
    return nomeNormalizado.includes('cancel') || nomeNormalizado.includes('rejei');
  }

  ehTerminalPositivoComAlerta(etapa: EtapaTimeline): boolean {
    return etapa.nome.toLowerCase().includes('devolv');
  }

  classesBolinha(etapa: EtapaTimeline): string {
    if (this.ehTerminalNegativo(etapa)) {
      return 'bg-red-500 text-white border-red-500';
    }
    if (this.ehTerminalPositivoComAlerta(etapa)) {
      return 'bg-amber-500 text-white border-amber-500';
    }
    switch (etapa.status) {
      case 'CONCLUIDA':
        return 'bg-coral-600 text-white border-coral-600';
      case 'ATUAL':
        return 'bg-coral-600 text-white border-coral-600 ring-4 ring-coral-200 motion-safe:animate-pulse';
      case 'PENDENTE':
        return 'bg-white text-graphite-400 border-graphite-300 border-2';
    }
  }

  classesConector(etapa: EtapaTimeline): string {
    if (this.ehTerminalNegativo(etapa)) return 'bg-red-300';
    if (etapa.status === 'PENDENTE') return 'border-dashed border-graphite-300 border-l-2 md:border-l-0 md:border-t-2 bg-transparent';
    return 'bg-coral-600';
  }

  classesLabel(etapa: EtapaTimeline): string {
    if (this.ehTerminalNegativo(etapa)) return 'text-red-700 font-semibold';
    if (etapa.status === 'ATUAL') return 'text-graphite-900 font-semibold';
    if (etapa.status === 'PENDENTE') return 'text-graphite-500';
    return 'text-graphite-700';
  }

  ariaCurrent(etapa: EtapaTimeline): 'step' | null {
    return etapa.status === 'ATUAL' ? 'step' : null;
  }
}
```

- [ ] **Step 2: Criar `order-timeline.component.html`**

```html
<ol [class.is-compact]="compact" class="order-timeline">
  <li *ngFor="let etapa of timeline.etapas; let ultima = last"
      [attr.aria-current]="ariaCurrent(etapa)"
      class="order-timeline__item">

    <div class="order-timeline__indicador">
      <span [class]="classesBolinha(etapa)"
            class="order-timeline__bolinha"
            aria-hidden="true">
        <ng-container *ngIf="etapa.status === 'CONCLUIDA' && !ehTerminalNegativo(etapa) && !ehTerminalPositivoComAlerta(etapa)">✓</ng-container>
        <ng-container *ngIf="ehTerminalNegativo(etapa)">✕</ng-container>
        <ng-container *ngIf="ehTerminalPositivoComAlerta(etapa)">↩</ng-container>
      </span>
      <span *ngIf="!ultima" [class]="classesConector(etapa)" class="order-timeline__conector"></span>
    </div>

    <div class="order-timeline__conteudo">
      <p [class]="classesLabel(etapa)" class="order-timeline__nome text-sm">{{ etapa.nome }}</p>
      <ng-container *ngIf="!compact">
        <p *ngIf="etapa.tempoDecorrido" class="text-xs text-graphite-500 mt-0.5">{{ etapa.tempoDecorrido }}</p>
        <p *ngIf="!etapa.tempoDecorrido && etapa.previsaoEntrega" class="text-xs text-graphite-500 mt-0.5">
          Previsão: {{ etapa.previsaoEntrega }}
        </p>
      </ng-container>
    </div>
  </li>
</ol>
```

- [ ] **Step 3: Criar `order-timeline.component.scss`**

```scss
.order-timeline {
  display: flex;
  flex-direction: column;
  gap: 0;
  list-style: none;
  padding: 0;
  margin: 0;
}

.order-timeline__item {
  display: grid;
  grid-template-columns: 32px 1fr;
  gap: 0.75rem;
  align-items: start;
}

.order-timeline__indicador {
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
}

.order-timeline__bolinha {
  width: 1.5rem;
  height: 1.5rem;
  border-radius: 9999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 0.75rem;
  border-width: 1px;
  flex-shrink: 0;
}

.order-timeline__conector {
  flex: 1;
  width: 2px;
  margin-top: 0.25rem;
  min-height: 1.5rem;
}

.order-timeline__conteudo {
  padding-bottom: 1.25rem;
}

.order-timeline__item:last-child .order-timeline__conteudo {
  padding-bottom: 0;
}

.is-compact .order-timeline__bolinha {
  width: 0.75rem;
  height: 0.75rem;
  font-size: 0;
}

.is-compact .order-timeline__conteudo {
  padding-bottom: 0.5rem;
}

@media (min-width: 768px) {
  :host:not(.compact-host) .order-timeline:not(.is-compact) {
    flex-direction: row;
    align-items: flex-start;
  }
  :host:not(.compact-host) .order-timeline:not(.is-compact) .order-timeline__item {
    grid-template-columns: 1fr;
    grid-template-rows: auto auto;
    text-align: center;
    flex: 1;
  }
  :host:not(.compact-host) .order-timeline:not(.is-compact) .order-timeline__indicador {
    flex-direction: row;
    height: auto;
  }
  :host:not(.compact-host) .order-timeline:not(.is-compact) .order-timeline__conector {
    width: 100%;
    height: 2px;
    margin-top: 0.75rem;
    margin-left: 0;
    min-height: 0;
  }
  :host:not(.compact-host) .order-timeline:not(.is-compact) .order-timeline__conteudo {
    padding-bottom: 0;
    padding-top: 0.5rem;
  }
}

@media (prefers-reduced-motion: reduce) {
  .order-timeline__bolinha {
    animation: none !important;
  }
}
```

- [ ] **Step 4: Build**

```bash
cd /home/ali/projects/pet-hub/frontend/storefront && npm run build
```

- [ ] **Step 5: Commit**

```bash
git add frontend/storefront/src/app/modules/orders/components/order-timeline
git commit -m "feat(storefront): slice4 16-order timeline component (visual highlight)"
```

---

## Task 17: OrdersListaPage com paginação e filtros sync URL

**Files:**
- Modify: `frontend/storefront/src/app/modules/orders/pages/lista/lista.page.ts`
- Create: `frontend/storefront/src/app/modules/orders/pages/lista/lista.page.html`

- [ ] **Step 1: Substituir `lista.page.ts`**

```ts
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Subject, debounceTime, takeUntil } from 'rxjs';

import { OrderService, ListarPedidosParams } from '@modules/orders/services/order.service';
import {
  PageResponse,
  PedidoResumoResponse,
  StatusPedido,
  STATUS_LABEL,
  classesBadgeStatus,
} from '@modules/orders/models/order';
import { SkeletonComponent } from '@shared/components/skeleton/skeleton.component';

@Component({
  selector: 'app-orders-lista-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, SkeletonComponent],
  templateUrl: './lista.page.html',
})
export class OrdersListaPage implements OnInit {
  private readonly orderService = inject(OrderService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly destroy$ = new Subject<void>();

  readonly STATUS_LABEL = STATUS_LABEL;
  readonly classesBadge = classesBadgeStatus;
  readonly listaStatus: StatusPedido[] = [
    'PENDENTE_PAGAMENTO', 'PAGAMENTO_APROVADO', 'PAGAMENTO_REJEITADO',
    'SEPARACAO', 'EM_TRANSPORTE', 'ENTREGUE', 'CANCELADO', 'DEVOLVIDO',
  ];

  readonly carregando = signal(true);
  readonly erro = signal(false);
  readonly pagina = signal<PageResponse<PedidoResumoResponse> | null>(null);

  readonly filtros = this.fb.nonNullable.group({
    status: '',
    dataInicio: '',
    dataFim: '',
  });

  ngOnInit(): void {
    this.route.queryParamMap.pipe(takeUntil(this.destroy$)).subscribe(parametros => {
      this.filtros.patchValue({
        status: parametros.get('status') ?? '',
        dataInicio: parametros.get('dataInicio') ?? '',
        dataFim: parametros.get('dataFim') ?? '',
      }, { emitEvent: false });
      this.carregar(Number(parametros.get('page') ?? 0));
    });

    this.filtros.valueChanges.pipe(debounceTime(250), takeUntil(this.destroy$)).subscribe(valor => {
      this.router.navigate([], {
        queryParams: {
          page: 0,
          status: valor.status || null,
          dataInicio: valor.dataInicio || null,
          dataFim: valor.dataFim || null,
        },
        queryParamsHandling: 'merge',
      });
    });
  }

  carregar(page: number): void {
    this.carregando.set(true);
    this.erro.set(false);
    const valor = this.filtros.getRawValue();
    const parametros: ListarPedidosParams = {
      page,
      size: 10,
      sort: 'criadoEm,desc',
      status: (valor.status as StatusPedido) || undefined,
      dataInicio: valor.dataInicio ? `${valor.dataInicio}T00:00:00` : undefined,
      dataFim: valor.dataFim ? `${valor.dataFim}T23:59:59` : undefined,
    };
    this.orderService.list(parametros).subscribe({
      next: pagina => { this.pagina.set(pagina); this.carregando.set(false); },
      error: () => { this.erro.set(true); this.carregando.set(false); },
    });
  }

  irParaPagina(novaPagina: number): void {
    this.router.navigate([], {
      queryParams: { page: novaPagina },
      queryParamsHandling: 'merge',
    });
  }

  limparFiltros(): void {
    this.filtros.reset({ status: '', dataInicio: '', dataFim: '' });
    this.router.navigate([], { queryParams: {} });
  }

  ngOnDestroy(): void { this.destroy$.next(); this.destroy$.complete(); }
}
```

- [ ] **Step 2: Criar `lista.page.html`**

```html
<section>
  <h1 class="text-2xl font-display font-semibold text-graphite-900">Meus pedidos</h1>

  <form [formGroup]="filtros" class="mt-6 flex flex-wrap items-end gap-3">
    <div>
      <label class="text-xs text-graphite-600">Status</label>
      <select formControlName="status" class="mt-1 rounded-md border-graphite-300">
        <option value="">Todos</option>
        <option *ngFor="let valor of listaStatus" [value]="valor">{{ STATUS_LABEL[valor] }}</option>
      </select>
    </div>
    <div>
      <label class="text-xs text-graphite-600">De</label>
      <input type="date" formControlName="dataInicio" class="mt-1 rounded-md border-graphite-300" />
    </div>
    <div>
      <label class="text-xs text-graphite-600">Até</label>
      <input type="date" formControlName="dataFim" class="mt-1 rounded-md border-graphite-300" />
    </div>
    <button type="button" class="btn-ghost text-sm py-2 px-3" (click)="limparFiltros()">Limpar</button>
  </form>

  <app-skeleton *ngIf="carregando()" [lines]="6" class="mt-6 block" />

  <div *ngIf="erro()" class="mt-6 rounded-md border border-red-200 bg-red-50 p-4 text-sm text-red-700">
    Não foi possível carregar pedidos.
    <button type="button" class="ml-2 underline" (click)="carregar(0)">Tentar de novo</button>
  </div>

  <ng-container *ngIf="!carregando() && !erro() && pagina() as pagina">
    <p *ngIf="pagina.content.length === 0" class="mt-6 text-sm text-graphite-500">
      Nenhum pedido encontrado com esses filtros.
    </p>

    <ul class="mt-6 grid gap-3">
      <li *ngFor="let pedido of pagina.content"
          class="rounded-lg border border-graphite-200 bg-graphite-0 p-4 flex items-center justify-between gap-3">
        <div class="min-w-0">
          <a [routerLink]="['/minha-conta/pedidos', pedido.numeroPedido]"
             class="text-sm font-medium text-graphite-900 hover:text-coral-700">
            {{ pedido.numeroPedido }}
          </a>
          <p class="text-xs text-graphite-500 mt-1">
            {{ pedido.criadoEm | date:'dd/MM/yyyy HH:mm' }} · {{ pedido.totalItens }} item(s) · {{ pedido.formaPagamentoTipo }}
          </p>
        </div>
        <div class="text-right shrink-0">
          <span [class]="classesBadge(pedido.status)" class="inline-block rounded-full px-2 py-0.5 text-xs">
            {{ STATUS_LABEL[pedido.status] }}
          </span>
          <p class="text-sm font-semibold text-graphite-900 mt-1">{{ pedido.valorTotal | currency:'BRL':'symbol':'1.2-2' }}</p>
        </div>
      </li>
    </ul>

    <nav *ngIf="pagina.totalPages > 1" class="mt-6 flex items-center justify-between">
      <button type="button" class="btn-ghost text-sm py-2 px-3"
              [disabled]="pagina.first" (click)="irParaPagina(pagina.number - 1)">« Anterior</button>
      <span class="text-sm text-graphite-600">Página {{ pagina.number + 1 }} de {{ pagina.totalPages }}</span>
      <button type="button" class="btn-ghost text-sm py-2 px-3"
              [disabled]="pagina.last" (click)="irParaPagina(pagina.number + 1)">Próximo »</button>
    </nav>
  </ng-container>
</section>
```

- [ ] **Step 3: Build + smoke**

```bash
cd /home/ali/projects/pet-hub/frontend/storefront && npm run build
```

Smoke: filtros sync URL (mudar Status → recarrega lista, URL ganha `?status=...`); paginação funciona; "Limpar" reseta tudo.

- [ ] **Step 4: Commit**

```bash
git add frontend/storefront/src/app/modules/orders/pages/lista
git commit -m "feat(storefront): slice4 17-orders list with url-synced filters"
```

---

## Task 18: OrdersDetalhePage com timeline full

**Files:**
- Modify: `frontend/storefront/src/app/modules/orders/pages/detalhe/detalhe.page.ts`
- Create: `frontend/storefront/src/app/modules/orders/pages/detalhe/detalhe.page.html`

- [ ] **Step 1: Substituir `detalhe.page.ts`**

```ts
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import { OrderService } from '@modules/orders/services/order.service';
import {
  PedidoResponse,
  Timeline,
  STATUS_LABEL,
  classesBadgeStatus,
} from '@modules/orders/models/order';
import { OrderTimelineComponent } from '@modules/orders/components/order-timeline/order-timeline.component';
import { SkeletonComponent } from '@shared/components/skeleton/skeleton.component';

@Component({
  selector: 'app-orders-detalhe-page',
  standalone: true,
  imports: [CommonModule, RouterLink, OrderTimelineComponent, SkeletonComponent],
  templateUrl: './detalhe.page.html',
})
export class OrdersDetalhePage implements OnInit {
  private readonly orderService = inject(OrderService);
  private readonly route = inject(ActivatedRoute);

  readonly STATUS_LABEL = STATUS_LABEL;
  readonly classesBadge = classesBadgeStatus;

  readonly carregando = signal(true);
  readonly erro = signal(false);
  readonly pedido = signal<PedidoResponse | null>(null);
  readonly timeline = signal<Timeline | null>(null);

  ngOnInit(): void {
    const numero = this.route.snapshot.paramMap.get('numero');
    if (!numero) {
      this.erro.set(true);
      this.carregando.set(false);
      return;
    }
    this.carregar(numero);
  }

  carregar(numero: string): void {
    this.carregando.set(true);
    this.erro.set(false);
    forkJoin({
      pedido: this.orderService.detalhe(numero),
      timeline: this.orderService.timeline(numero),
    }).subscribe({
      next: ({ pedido, timeline }) => {
        this.pedido.set(pedido);
        this.timeline.set(timeline);
        this.carregando.set(false);
      },
      error: () => {
        this.erro.set(true);
        this.carregando.set(false);
      },
    });
  }
}
```

- [ ] **Step 2: Criar `detalhe.page.html`**

```html
<section>
  <a routerLink="/minha-conta/pedidos" class="text-sm text-coral-600 hover:text-coral-700">← Voltar para Meus pedidos</a>

  <app-skeleton *ngIf="carregando()" [lines]="8" class="mt-6 block" />

  <div *ngIf="erro()" class="mt-6 rounded-md border border-red-200 bg-red-50 p-4 text-sm text-red-700">
    Não foi possível carregar o pedido.
  </div>

  <ng-container *ngIf="!carregando() && !erro() && pedido() as pedido">
    <header class="mt-4 flex flex-wrap items-baseline justify-between gap-3">
      <h1 class="text-2xl font-display font-semibold text-graphite-900">Pedido {{ pedido.numeroPedido }}</h1>
      <span [class]="classesBadge(pedido.status)" class="inline-block rounded-full px-3 py-1 text-sm">
        {{ STATUS_LABEL[pedido.status] }}
      </span>
    </header>
    <p class="text-sm text-graphite-600">Realizado em {{ pedido.criadoEm | date:'dd/MM/yyyy HH:mm' }}</p>

    <div *ngIf="timeline() as timelineDados" class="mt-6 rounded-lg border border-graphite-200 bg-graphite-0 p-6">
      <app-order-timeline [timeline]="timelineDados" />
    </div>

    <div class="mt-6 grid gap-6 md:grid-cols-2">
      <article class="rounded-lg border border-graphite-200 bg-graphite-0 p-5">
        <h2 class="text-sm font-medium text-graphite-700">Itens</h2>
        <ul class="mt-3 divide-y divide-graphite-200">
          <li *ngFor="let item of pedido.itens" class="py-3 flex items-center gap-3">
            <img *ngIf="item.fotoUrl" [src]="item.fotoUrl" alt="" class="h-12 w-12 rounded object-cover" />
            <div class="flex-1 min-w-0">
              <p class="text-sm font-medium text-graphite-900 truncate">{{ item.nomeProduto }}</p>
              <p class="text-xs text-graphite-500">SKU {{ item.skuProduto }}</p>
            </div>
            <p class="text-sm text-graphite-700 shrink-0">
              {{ item.quantidade }}× {{ item.precoUnitario | currency:'BRL':'symbol':'1.2-2' }}
            </p>
          </li>
        </ul>
      </article>

      <article class="rounded-lg border border-graphite-200 bg-graphite-0 p-5">
        <h2 class="text-sm font-medium text-graphite-700">Resumo</h2>
        <dl class="mt-3 space-y-2 text-sm">
          <div class="flex justify-between"><dt class="text-graphite-600">Subtotal</dt>
            <dd class="text-graphite-900">{{ pedido.valorSubtotal | currency:'BRL':'symbol':'1.2-2' }}</dd></div>
          <div *ngIf="pedido.valorDescontos > 0" class="flex justify-between">
            <dt class="text-graphite-600">Desconto<span *ngIf="pedido.cupomCodigo"> ({{ pedido.cupomCodigo }})</span></dt>
            <dd class="text-emerald-700">-{{ pedido.valorDescontos | currency:'BRL':'symbol':'1.2-2' }}</dd></div>
          <div class="flex justify-between"><dt class="text-graphite-600">Frete</dt>
            <dd class="text-graphite-900">{{ pedido.valorFrete | currency:'BRL':'symbol':'1.2-2' }}</dd></div>
          <div class="flex justify-between border-t border-graphite-200 pt-2">
            <dt class="font-semibold text-graphite-900">Total</dt>
            <dd class="font-semibold text-graphite-900">{{ pedido.valorTotal | currency:'BRL':'symbol':'1.2-2' }}</dd></div>
        </dl>
        <p class="mt-4 text-xs text-graphite-600">
          Pagamento: {{ pedido.formaPagamentoTipo }}
          <span *ngIf="pedido.formaPagamentoBandeira"> · {{ pedido.formaPagamentoBandeira }}</span>
          <span *ngIf="pedido.formaPagamentoUltimos4"> •••• {{ pedido.formaPagamentoUltimos4 }}</span>
        </p>
      </article>

      <article class="rounded-lg border border-graphite-200 bg-graphite-0 p-5">
        <h2 class="text-sm font-medium text-graphite-700">Endereço de entrega</h2>
        <p class="mt-3 text-sm text-graphite-700">
          {{ pedido.enderecoEntrega.logradouro }}, {{ pedido.enderecoEntrega.numero ?? 's/n' }}<br>
          <span *ngIf="pedido.enderecoEntrega.complemento">{{ pedido.enderecoEntrega.complemento }}<br></span>
          {{ pedido.enderecoEntrega.bairro }} · {{ pedido.enderecoEntrega.cidade }} / {{ pedido.enderecoEntrega.uf }}<br>
          CEP {{ pedido.enderecoEntrega.cep }}
        </p>
      </article>

      <article class="rounded-lg border border-graphite-200 bg-graphite-0 p-5">
        <h2 class="text-sm font-medium text-graphite-700">Endereço de cobrança</h2>
        <p class="mt-3 text-sm text-graphite-700">
          {{ pedido.enderecoCobranca.logradouro }}, {{ pedido.enderecoCobranca.numero ?? 's/n' }}<br>
          <span *ngIf="pedido.enderecoCobranca.complemento">{{ pedido.enderecoCobranca.complemento }}<br></span>
          {{ pedido.enderecoCobranca.bairro }} · {{ pedido.enderecoCobranca.cidade }} / {{ pedido.enderecoCobranca.uf }}<br>
          CEP {{ pedido.enderecoCobranca.cep }}
        </p>
      </article>
    </div>
  </ng-container>
</section>
```

- [ ] **Step 3: Build + smoke**

```bash
cd /home/ali/projects/pet-hub/frontend/storefront && npm run build
```

Smoke: abrir `/minha-conta/pedidos/PH-2026-000002` (algum número que exista no banco). Timeline renderiza horizontal em desktop, vertical em mobile. Estados terminais (CANCELADO/ENTREGUE) renderizam corretamente.

- [ ] **Step 4: Commit**

```bash
git add frontend/storefront/src/app/modules/orders/pages/detalhe
git commit -m "feat(storefront): slice4 18-order detail page with full timeline"
```

---

## Task 19: Habilitar CTA "Acompanhar pedido" na success page

**Files:**
- Modify: `frontend/storefront/src/app/modules/checkout/pages/success/success.page.html`

- [ ] **Step 1: Inspecionar a estrutura atual**

```bash
grep -n "Acompanhar\|disabled" /home/ali/projects/pet-hub/frontend/storefront/src/app/modules/checkout/pages/success/success.page.html
```

Anote os números de linha do botão `Acompanhar pedido` e do atributo `disabled`.

- [ ] **Step 2: Substituir o `<button disabled>` por `<a routerLink>`**

O bloco atual tem aproximadamente:

```html
<button
  type="button"
  class="..."
  disabled
  title="Disponível em breve">
  Acompanhar pedido
</button>
```

Substituir por:

```html
<a
  [routerLink]="['/minha-conta/pedidos', numeroPedido()]"
  class="btn-primary text-sm py-2 px-4 text-center"
  aria-label="Acompanhar pedido">
  Acompanhar pedido
</a>
```

Notas:
- O signal/property que tem o número do pedido na success page (provavelmente `numeroPedido` — verificar com `grep -n "numeroPedido" success.page.ts`). Usar o nome exato.
- Manter as classes Tailwind do botão original adaptadas para o `<a>` (verificar visualmente que o estilo é consistente).
- Se ainda houver imports relacionados a `disabled` ou `title` lógica no `.ts`, remover.

- [ ] **Step 3: Build + smoke**

```bash
cd /home/ali/projects/pet-hub/frontend/storefront && npm run build
```

Smoke: fazer um pedido novo (login, carrinho, checkout completo). Na tela de sucesso, clicar "Acompanhar pedido" → navega para `/minha-conta/pedidos/PH-...` com timeline.

- [ ] **Step 4: Commit**

```bash
git add frontend/storefront/src/app/modules/checkout/pages/success/success.page.html
git commit -m "feat(storefront): slice4 19-enable acompanhar pedido cta"
```

---

## Task 20: Smoke E2E completo + atualizar pendencias.md + ROADMAP

**Files:**
- Modify: `ai-memory/roadmap/fase-5-pendencias.md`
- Modify: `ROADMAP.md`

- [ ] **Step 1: Build limpo**

```bash
cd /home/ali/projects/pet-hub/frontend/storefront && npm run build
```

Expected: build sem erros nem warnings novos. Anotar tamanho dos chunks (compare com `git log` anterior se quiser monitorar bloat).

- [ ] **Step 2: Smoke checklist do spec §9 (manual no browser)**

Storefront em `http://127.0.0.1:4242`, logado com `cliente.demo@pethub.dev / Senha@123` (ou o que estiver no seed V16).

Verificar:

- [ ] `/minha-conta` carrega 5 cards com dados.
- [ ] Forçar erro em 1 endpoint (parar backend brevemente ou apertar DevTools throttling): card específico mostra "Tentar de novo", outros mantêm dados.
- [ ] Sidebar destaca item ativo, hambúrguer no mobile abre drawer, ESC fecha drawer.
- [ ] Logout no rodapé do sidebar redireciona para `/`.
- [ ] `/perfil` carrega dados, salva alterações, dirty desativa botão se reverter.
- [ ] `/enderecos` add/edit/delete com confirm; ViaCEP autofill; marcar padrão.
- [ ] `/cartoes` add com tokenize (DevTools: campos `numero`/`cvv` zerados após tokenize); marcar padrão; delete.
- [ ] `/pets` add/edit/delete; upload de foto com preview client-side antes do submit.
- [ ] `/pedidos` lista paginada; filtros sync URL (verificar `?status=...` na barra); badge correto por status.
- [ ] `/pedidos/:numero` timeline full horizontal em desktop, vertical em mobile; snapshots dos itens corretos.
- [ ] Estado terminal CANCELADO: timeline mostra etapa "Cancelado" em vermelho.
- [ ] CTA "Acompanhar pedido" da success do checkout navega para o detalhe.
- [ ] `product-detail` renderiza `<strong>`/`<p>` (testar com produto cuja `descricaoCompleta` tenha HTML). Tentar injetar `<script>` via DB direto e verificar que é stripado.
- [ ] `prefers-reduced-motion: reduce` no DevTools → pulse da etapa ATUAL desabilitado.
- [ ] Teclado: Tab navega cards/forms, Enter submete forms, ESC fecha drawer/dialog.

Documentar qualquer falha encontrada como sub-task antes de continuar.

- [ ] **Step 3: Atualizar `ai-memory/roadmap/fase-5-pendencias.md`**

Editar o arquivo:
- Mudar primeira linha: `> **Status:** Slices 1-4 ✅ entregues em 2026-05-14 a 2026-05-17. Fase 5 concluída.`
- Mover seção "⏳ Slice 4" para "✅ Slices entregues" e renomear para "Slice 4 — Minha conta + Timeline visual"; listar os 19 commits sequenciais (`feat(storefront): slice4 01-...` até `19-...`).
- Manter a seção "⚠️ Dívida técnica restante" — adicionar lá os testes formais do slice 4 já listados no spec §10.1.
- Atualizar a seção "🚀 Comando rápido para retomar" — não há mais slice pendente; substituir por nota "Fase 5 entregue. Próxima: Fase 6 (Admin)."

- [ ] **Step 4: Atualizar `ROADMAP.md`**

Trocar status da Fase 5: de `🚧 (slices 1-3 ✅, slice 4 pendente)` para `✅`.

Encontrar a linha (aproximadamente linha 128):

```
## Fase 5 — Frontend Storefront (Angular) · 🚧 (slices 1-3 ✅, slice 4 pendente)
```

Substituir por:

```
## Fase 5 — Frontend Storefront (Angular) · ✅
```

Adicionar (logo abaixo do título) uma nota como as outras fases:

```
> **Entregue em 2026-05-17.** Slice 4 fechou a Fase 5 com /minha-conta (sidebar + 6 sub-rotas), CRUDs completos (perfil, endereços, cartões, pets), módulo orders com OrderTimelineComponent (destaque visual híbrido h-md+/v-mobile) e SafeHtmlPipe com DOMPurify. Testes Karma/Jest formais continuam dívida acumulada (mesmo bloqueio das fases 1-4). Histórico em [`ai-memory/roadmap/fase-5-pendencias.md`](./ai-memory/roadmap/fase-5-pendencias.md).
```

- [ ] **Step 5: Commit final**

```bash
cd /home/ali/projects/pet-hub
git add ai-memory/roadmap/fase-5-pendencias.md ROADMAP.md
git commit -m "$(cat <<'EOF'
docs(roadmap): Fase 5 concluída — slice 4 entregue

Slice 4 fechou a Fase 5: minha-conta (shell + 6 sub-rotas), CRUDs
(perfil, endereços, cartões, pets), módulo orders com OrderTimelineComponent
e SafeHtmlPipe com DOMPurify. Smoke checklist verde.

Próxima fase: 6 (Admin back-office).
EOF
)"
```

- [ ] **Step 6: Verificar histórico do slice 4**

```bash
git log --oneline | grep "slice4" | head -25
```

Expected: 19 commits sequenciais (slice4 01..19) + 1 commit final docs(roadmap) — total 20 commits desta slice (mais o spec e o spec fix).

---

## Verificação final

**Critérios de "pronto" (do spec §11):**

- [ ] Todas as 7 rotas novas acessíveis (`/minha-conta`, `/perfil`, `/enderecos`, `/cartoes`, `/pets`, `/pedidos`, `/pedidos/:numero`).
- [ ] Smoke checklist §9 do spec verde.
- [ ] `npm run build` sem erros nem warnings novos.
- [ ] CTA "Acompanhar pedido" habilitado e navega corretamente.
- [ ] `product-detail` usa `safeHtml` (escape manual removido).
- [ ] Commits Conventional Commits, um por marco (~20 commits).
- [ ] `ai-memory/roadmap/fase-5-pendencias.md` atualizado.
- [ ] `ROADMAP.md` atualizado (Fase 5 ✅).

**Dívida técnica registrada:**

- Testes formais Karma/Jest (mesmo bloqueio fases 1-4).
- Cancelar pedido pelo cliente.
- Mudar email/senha do perfil (fluxo dedicado).
- Reordenar/favoritar endereços.
- Histórico de cartões removidos.
- Notificações em tempo real (WebSocket/SSE) — Fase 6+.
- AppSec: LOG-2 endereçar na transição para Fase 6; JWT-1/CORS-1/VAL-1 bloqueantes só em staging.
