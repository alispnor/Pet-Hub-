# Admin Slice 6.1 — Foundation (scaffold + auth + shell + dashboard placeholder)

> **Status:** Draft · 2026-05-17
> **Owner:** Ali
> **Phase:** 6 (Frontend Admin) — Slice 1 de 5
> **Predecessor:** Fase 5 ✅ concluída
> **Successor:** Slice 6.2 (Catálogo)
> **Decomposição completa da fase:** [`ai-memory/roadmap/fase-6-decomposicao.md`](../../../ai-memory/roadmap/fase-6-decomposicao.md)

## 1. Objetivo

Estabelecer a fundação do app `frontend/admin/` — segundo app Angular 17 separado do storefront, para uso de operadores, gerentes e administradores da loja. Este slice entrega só a estrutura sustentável; nenhuma feature de negócio (catálogo, pedidos, dashboards reais) entra aqui.

**Entregáveis concretos:**
- Scaffold Angular 17 standalone em `frontend/admin/` com Tailwind 3 + design tokens espelhando o storefront.
- Porta dev `4244` (storefront é 4242), proxy `/api/v1/*` → `:8080`.
- `AdminAuthService` reusando padrão do storefront, com validação de role no login (cliente comum é rejeitado).
- `AdminShellPage` — layout topbar 56px + sidebar densa 64px (com toggle de pin pra 200px expandida).
- `roleGuard(perfis: Role[])` reusável (exportado, mas não aplicado em nenhuma rota neste slice).
- `/login` (form simples) e `/dashboard` (placeholder com 3 cards estáticos).
- Componentes shared duplicados do storefront: `SkeletonComponent`, `ToastService`+`ToastStackComponent`, `ConfirmDialogService`+`ConfirmDialogComponent`.
- HTTP `authInterceptor` (anexa Bearer + retry em 401 via refresh).
- Bootstrap de sessão via `tryRefreshOnBoot()` no `app.component`.

**Não-objetivos:**
- Dashboard real com KPIs (slice 6.5).
- Qualquer CRUD (slices 6.2-6.4).
- Audit log (slice 6.3+).
- WebSocket/SSE (slice 6.5).
- "Esqueci minha senha" (backlog).
- Testes formais Karma/Jest (continua dívida acumulada).
- AppSec pendências (mesmo bloqueio das fases anteriores).

## 2. Decisões tomadas (durante o brainstorming)

| Decisão | Escolha | Justificativa |
|---|---|---|
| Apps separados (storefront vs admin) | **Sim, dois apps Angular distintos** | Carga inicial menor pro cliente, permissões mais simples, deploy independente, padrão de mercado e-commerce. |
| Code sharing entre apps | **Duplicar (não pacote shared)** | Velocidade > DRY no momento. Aceita divergência pequena. Storefront e admin evoluem em ritmos diferentes. |
| Visual identity | **Mesma paleta `coral`/`graphite`, layout admin-style** | Consistência de marca; sinaliza "admin" pelo layout (sidebar densa, topbar fina), não por cores diferentes. |
| Login | **Mesmo endpoint `POST /auth/login`, frontend valida role** | Sem mudança no backend. Frontend rejeita `ROLE_CLIENTE` com toast e invalida session via `POST /auth/logout`. Validação dupla (backend também enforce via `@PreAuthorize` nos endpoints admin). |
| Sidebar | **Compacta 64px + toggle de pin para 200px** | Operadores trabalham com tela cheia; expandido vira opt-in. Estado em `localStorage`. |
| Refresh token | **Mesma estratégia do storefront (cookie HttpOnly)** | AppSec já validado na Fase 5; reusar é seguro. |
| "Manter conectado" | **NÃO no admin** | Admin sempre usa TTL padrão de 7 dias (menos exposição em caso de comprometimento). |
| Porta dev | **4244** | Evita conflito com storefront (4242) e com Pet Diary (5173/8000). |
| Dashboard slice 6.1 | **Placeholder de 3 cards `--`** | Estabelece grid + visual; valores reais entram no slice 6.5. |
| `roleGuard` | **Implementar + exportar, sem aplicar** | Disponível pros slices 6.2+ (cupons só GERENTE+, etc). |

## 3. Arquitetura

### 3.1 Estrutura de arquivos

```
frontend/admin/                              ⊕ NOVO
├── angular.json
├── package.json
├── package-lock.json
├── proxy.conf.json                          → /api/v1/* via :8080, withCredentials
├── tailwind.config.js                       (cópia do storefront, mesmos tokens)
├── postcss.config.js
├── tsconfig.json + tsconfig.app.json + tsconfig.spec.json
├── README.md
└── src/
    ├── index.html
    ├── main.ts
    ├── styles.scss                          (importa Tailwind + reset)
    ├── environments/
    │   ├── environment.ts                   (apiBase: '/api/v1')
    │   └── environment.prod.ts
    └── app/
        ├── app.component.ts                 (host do <router-outlet> + tryRefreshOnBoot)
        ├── app.config.ts                    (providers: HttpClient, interceptors, router)
        ├── app.routes.ts
        ├── core/
        │   ├── services/
        │   │   └── admin-auth.service.ts
        │   ├── guards/
        │   │   ├── auth.guard.ts            (precisa estar logado)
        │   │   └── role.guard.ts            (factory: roleGuard(rolesPermitidas))
        │   ├── interceptors/
        │   │   └── auth.interceptor.ts      (Bearer + 401-refresh-retry com gate)
        │   ├── layout/
        │   │   └── admin-shell/
        │   │       ├── admin-shell.page.ts
        │   │       ├── admin-shell.page.html
        │   │       └── admin-shell.page.scss
        │   └── models/
        │       ├── auth.ts                  (AdminUser, AuthResponse, LoginRequest, Role)
        │       └── errors.ts                (ForbiddenLoginError)
        ├── shared/
        │   ├── components/
        │   │   ├── skeleton/skeleton.component.ts                (cópia)
        │   │   ├── toast-stack/toast-stack.component.ts          (cópia)
        │   │   └── confirm-dialog/confirm-dialog.component.ts    (cópia)
        │   └── services/
        │       ├── toast.service.ts                              (cópia)
        │       └── confirm-dialog.service.ts                     (cópia)
        └── modules/
            ├── auth/
            │   └── pages/
            │       └── login/
            │           ├── login.page.ts
            │           └── login.page.html
            └── dashboard/
                └── pages/
                    └── home/
                        ├── home.page.ts
                        └── home.page.html
```

### 3.2 Routing (`app.routes.ts`)

```ts
import { Routes } from '@angular/router';
import { authGuard } from '@core/guards/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import('@modules/auth/pages/login/login.page').then(m => m.LoginPage),
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () =>
      import('@core/layout/admin-shell/admin-shell.page').then(m => m.AdminShellPage),
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('@modules/dashboard/pages/home/home.page').then(m => m.DashboardHomePage),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
```

- Sem rota `/admin/*`: app inteiro é o admin. Em prod, deploy em subdomínio ou `/admin` via nginx reverse-proxy.
- `authGuard` no shell garante que `/dashboard` e filhas exigem sessão; sem login redireciona pra `/login`.

### 3.3 Auth flow

**Login bem-sucedido com role admin:**

```
POST /auth/login {email, senha}
  → backend valida + emite access token (15min) + cookie refresh HttpOnly (7d)
  → frontend recebe AuthResponse {accessToken, usuario: {id, nome, email, role}}
  → role ∈ {ROLE_ADMIN_LOJA, ROLE_GERENTE, ROLE_OPERADOR}?
       ✓ sim → storeSession(); navigate('/dashboard')
       ✗ não → invalidaSession() via POST /auth/logout; throwError(ForbiddenLoginError)
                                                          → login.page captura
                                                          → toast.error("Sem permissão...")
```

**F5 (refresh do browser):**

```
app.component.ngOnInit
  → AdminAuthService.tryRefreshOnBoot()
     → POST /auth/refresh (cookie anexado via withCredentials)
        → 200: storeSession()
        → 401: clearSession() (sem session — authGuard redireciona)
```

**401 em qualquer endpoint logado:**

```
authInterceptor pega 401
  → gate BehaviorSubject pra evitar N refreshes simultâneos
  → POST /auth/refresh
     → 200: retry da request original com novo Bearer
     → falha: clearSession(); router.navigateByUrl('/login')
```

### 3.4 Services

#### `AdminAuthService`

```ts
@Injectable({ providedIn: 'root' })
export class AdminAuthService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiBase;
  private readonly router = inject(Router);

  private readonly _accessToken = signal<string | null>(null);
  private readonly _user = signal<AdminUser | null>(null);
  private refreshGate$: Observable<string> | null = null;

  readonly accessToken = this._accessToken.asReadonly();
  readonly currentUser = this._user.asReadonly();
  readonly currentUserRole = computed(() => this._user()?.role ?? null);
  readonly isAuthenticated = computed(() => this._accessToken() !== null);

  readonly isOperador = computed(() => this.currentUserRole() === 'ROLE_OPERADOR');
  readonly isGerente  = computed(() => this.currentUserRole() === 'ROLE_GERENTE');
  readonly isAdmin    = computed(() => this.currentUserRole() === 'ROLE_ADMIN_LOJA');

  private static readonly ROLES_ADMIN: Role[] = [
    'ROLE_ADMIN_LOJA', 'ROLE_GERENTE', 'ROLE_OPERADOR',
  ];

  login(req: LoginRequest): Observable<AdminUser> {
    return this.http.post<AuthResponse>(`${this.api}/auth/login`, req, { withCredentials: true })
      .pipe(
        switchMap(res => {
          if (!AdminAuthService.ROLES_ADMIN.includes(res.usuario.role)) {
            return this.http.post(`${this.api}/auth/logout`, {}, { withCredentials: true }).pipe(
              catchError(() => of(null)),  // logout best-effort; segue erro de qualquer forma
              switchMap(() => throwError(() => new ForbiddenLoginError())),
            );
          }
          this.storeSession(res);
          return of(res.usuario);
        }),
      );
  }

  refresh(): Observable<string> {
    if (this.refreshGate$) return this.refreshGate$;
    this.refreshGate$ = this.http
      .post<AuthResponse>(`${this.api}/auth/refresh`, {}, { withCredentials: true })
      .pipe(
        tap(res => this.storeSession(res)),
        map(res => res.accessToken),
        shareReplay(1),
        finalize(() => { this.refreshGate$ = null; }),
      );
    return this.refreshGate$;
  }

  tryRefreshOnBoot(): Observable<boolean> {
    return this.refresh().pipe(
      map(() => true),
      catchError(() => { this.clearSession(); return of(false); }),
    );
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${this.api}/auth/logout`, {}, { withCredentials: true })
      .pipe(
        tap({
          next: () => { this.clearSession(); this.router.navigateByUrl('/login'); },
          error: () => { this.clearSession(); this.router.navigateByUrl('/login'); },
        }),
      );
  }

  private storeSession(res: AuthResponse): void {
    this._accessToken.set(res.accessToken);
    this._user.set(res.usuario);
  }

  private clearSession(): void {
    this._accessToken.set(null);
    this._user.set(null);
  }
}
```

#### `authGuard`

```ts
export const authGuard: CanActivateFn = () => {
  const auth = inject(AdminAuthService);
  const router = inject(Router);
  return auth.isAuthenticated() ? true : router.parseUrl('/login');
};
```

#### `roleGuard` (factory)

```ts
export function roleGuard(rolesPermitidas: Role[]): CanActivateFn {
  return () => {
    const auth = inject(AdminAuthService);
    const router = inject(Router);
    const role = auth.currentUserRole();
    if (!role) return router.parseUrl('/login');
    if (!rolesPermitidas.includes(role)) return router.parseUrl('/dashboard');
    return true;
  };
}
```

**Não aplicado em nenhuma rota neste slice.** Disponível para slices futuros.

#### `authInterceptor`

```ts
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AdminAuthService);
  const router = inject(Router);
  const token = auth.accessToken();
  const reqAutenticada = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(reqAutenticada).pipe(
    catchError((erro: HttpErrorResponse) => {
      if (erro.status === 401 && !req.url.includes('/auth/')) {
        return auth.refresh().pipe(
          switchMap(novoToken => next(req.clone({ setHeaders: { Authorization: `Bearer ${novoToken}` } }))),
          catchError(() => {
            auth.logout().subscribe();
            return throwError(() => erro);
          }),
        );
      }
      return throwError(() => erro);
    }),
  );
};
```

### 3.5 Layout — `AdminShellPage`

**Estrutura HTML:**

```html
<div class="min-h-screen flex flex-col bg-graphite-50">
  <!-- Topbar -->
  <header class="h-14 border-b border-graphite-200 bg-graphite-0 flex items-center px-4 gap-4">
    <button class="md:hidden" (click)="abrirDrawer()" aria-label="Abrir menu">☰</button>
    <h1 class="font-display font-semibold text-graphite-900">Pet Hub Admin</h1>
    <div class="ml-auto flex items-center gap-3">
      <span class="text-sm text-graphite-700">{{ user()?.nome }}</span>
      <span [class]="classesBadgeRole(user()?.role)" class="rounded-full px-2 py-0.5 text-xs">
        {{ rotuloRole(user()?.role) }}
      </span>
      <button class="btn-ghost text-sm py-1.5 px-3" (click)="sair()">Sair</button>
    </div>
  </header>

  <!-- Body: sidebar + main -->
  <div class="flex flex-1">
    <aside [class.w-16]="!sidebarPinada()" [class.w-50]="sidebarPinada()"
           class="hidden md:flex flex-col border-r border-graphite-200 bg-graphite-0 transition-all">
      <button (click)="alternarPin()" aria-label="Fixar sidebar"
              class="self-end p-2 text-graphite-500">📌</button>
      <ng-container *ngTemplateOutlet="navItens; context: { compacta: !sidebarPinada() }" />
    </aside>

    <!-- Drawer mobile -->
    <div *ngIf="drawerAberto()" class="md:hidden fixed inset-0 z-40 flex"
         (keydown.escape)="fecharDrawer()">
      <div class="absolute inset-0 bg-graphite-900/40" (click)="fecharDrawer()"></div>
      <aside class="relative flex flex-col w-64 bg-graphite-0 px-2 py-4 shadow-xl">
        <button (click)="fecharDrawer()" class="self-end p-2" aria-label="Fechar">✕</button>
        <ng-container *ngTemplateOutlet="navItens; context: { compacta: false }" />
      </aside>
    </div>

    <main class="flex-1 p-6 md:p-8 max-w-page mx-auto w-full">
      <router-outlet />
    </main>
  </div>

  <app-toast-stack />
  <app-confirm-dialog />
</div>

<ng-template #navItens let-compacta="compacta">
  <nav class="flex-1 flex flex-col gap-1 px-2" aria-label="Navegação principal">
    <a *ngFor="let item of itensMenu"
       [routerLink]="item.rota"
       [routerLinkActiveOptions]="{ exact: item.exact }"
       routerLinkActive="bg-coral-50 text-coral-700"
       [attr.aria-disabled]="!item.habilitada"
       [class.pointer-events-none]="!item.habilitada"
       [class.opacity-50]="!item.habilitada"
       class="flex items-center gap-3 rounded-md px-3 py-2 text-sm text-graphite-700 hover:bg-graphite-100"
       [title]="compacta ? item.rotulo : ''"
       [attr.aria-label]="item.rotulo">
      <span aria-hidden="true">{{ item.icone }}</span>
      <span *ngIf="!compacta">{{ item.rotulo }}</span>
      <span *ngIf="!compacta && !item.habilitada" class="ml-auto text-xs text-graphite-500">em breve</span>
    </a>
  </nav>
</ng-template>
```

**Item menu type:**

```ts
interface ItemMenu {
  rota: string;
  rotulo: string;
  icone: string;
  habilitada: boolean;
  exact?: boolean;
}
```

**Items do slice 6.1:**

```ts
readonly itensMenu: ItemMenu[] = [
  { rota: '/dashboard',     rotulo: 'Dashboard',     icone: '📊', habilitada: true,  exact: true },
  { rota: '/produtos',      rotulo: 'Catálogo',      icone: '📦', habilitada: false },
  { rota: '/pedidos',       rotulo: 'Pedidos',       icone: '📋', habilitada: false },
  { rota: '/comercial',     rotulo: 'Comercial',     icone: '🏷️', habilitada: false },
  { rota: '/clientes',      rotulo: 'Clientes',      icone: '👥', habilitada: false },
  { rota: '/relatorios',    rotulo: 'Relatórios',    icone: '📈', habilitada: false },
  { rota: '/configuracoes', rotulo: 'Configurações', icone: '⚙️', habilitada: false },
];
```

**Estado da sidebar pinada:** `signal<boolean>` inicializado de `localStorage.getItem('pethub:admin:sidebar:pinned')`, persistido on-toggle.

**Badge de role:**

| Role | Cor classe Tailwind | Rótulo |
|---|---|---|
| `ROLE_OPERADOR` | `bg-blue-100 text-blue-800` | Operador |
| `ROLE_GERENTE` | `bg-amber-100 text-amber-800` | Gerente |
| `ROLE_ADMIN_LOJA` | `bg-red-100 text-red-800` | Admin Loja |

### 3.6 Pages

#### `LoginPage`

Centralizada, sem distrações. Form com email + senha. Submit:

```ts
this.adminAuth.login({ email, senha }).subscribe({
  next: () => this.router.navigateByUrl('/dashboard'),
  error: (err) => {
    if (err instanceof ForbiddenLoginError) {
      this.toast.error('Sem permissão para acessar o admin.');
    } else if (err.status === 401) {
      this.toast.error('E-mail ou senha inválidos.');
    } else {
      this.toast.error('Erro ao entrar. Tente novamente.');
    }
    this.submitting.set(false);
  },
});
```

#### `DashboardHomePage`

3 cards estáticos no grid. Nenhuma chamada HTTP nesta fase.

```html
<section>
  <h1 class="text-2xl font-display text-graphite-900">Dashboard</h1>
  <p class="mt-1 text-sm text-graphite-600">
    Olá, {{ user()?.nome }} · {{ rotuloRole(user()?.role) }}.
  </p>

  <div class="mt-6 grid gap-4 md:grid-cols-3">
    <article class="rounded-lg border border-graphite-200 bg-graphite-0 p-5">
      <h2 class="text-sm font-medium text-graphite-700">Pedidos hoje</h2>
      <p class="mt-2 text-3xl font-display text-graphite-400">--</p>
      <p class="text-xs text-graphite-500 mt-2">em breve (Slice 6.5)</p>
    </article>
    <article class="rounded-lg border border-graphite-200 bg-graphite-0 p-5">
      <h2 class="text-sm font-medium text-graphite-700">Estoque crítico</h2>
      <p class="mt-2 text-3xl font-display text-graphite-400">--</p>
      <p class="text-xs text-graphite-500 mt-2">em breve (Slice 6.5)</p>
    </article>
    <article class="rounded-lg border border-graphite-200 bg-graphite-0 p-5">
      <h2 class="text-sm font-medium text-graphite-700">Receita 7d</h2>
      <p class="mt-2 text-3xl font-display text-graphite-400">R$ --</p>
      <p class="text-xs text-graphite-500 mt-2">em breve (Slice 6.5)</p>
    </article>
  </div>
</section>
```

### 3.7 Models

```ts
// core/models/auth.ts
export type Role = 'ROLE_CLIENTE' | 'ROLE_ADMIN_LOJA' | 'ROLE_GERENTE' | 'ROLE_OPERADOR';

export interface AdminUser {
  id: number;
  nome: string;
  email: string;
  role: Role;
}

export interface LoginRequest {
  email: string;
  senha: string;
}

export interface AuthResponse {
  accessToken: string;
  usuario: AdminUser;
}

// core/models/errors.ts
export class ForbiddenLoginError extends Error {
  constructor() {
    super('Forbidden login: user does not have admin role');
    this.name = 'ForbiddenLoginError';
  }
}
```

## 4. Configurações de build

### 4.1 `package.json`

Espelhar `frontend/storefront/package.json`:
- Mesmas versões Angular 17.3.x
- Mesmas devDependencies (`@tailwindcss/forms`, `@tailwindcss/typography`, etc.)
- Sem `canvas-confetti` (não é usado no admin)
- Sem `dompurify` no slice 6.1 (entra se admin renderizar HTML rich em algum momento; não previsto)

Script `start`:
```json
"start": "ng serve --host 127.0.0.1 --port 4244 --proxy-config proxy.conf.json"
```

### 4.2 `proxy.conf.json`

Idêntico ao storefront:
```json
{
  "/api/v1/*": {
    "target": "http://localhost:8080",
    "secure": false,
    "changeOrigin": true,
    "logLevel": "debug"
  }
}
```

### 4.3 `tailwind.config.js`

Cópia exata do storefront (mesmos tokens `coral`, `graphite`, `success`, `warning`, `danger`, `info`, mesmas fontes).

### 4.4 Backend — CORS

`.env.local` do backend tem `CORS_ALLOWED_ORIGINS` com `http://127.0.0.1:4242` etc. Precisa adicionar `http://127.0.0.1:4244` e `http://localhost:4244` na lista, senão browser bloqueia.

**Modificação necessária em `.env.local`:**

```
CORS_ALLOWED_ORIGINS=http://127.0.0.1:4242,http://localhost:4242,http://localhost:4200,http://localhost:4201,http://127.0.0.1:4244,http://localhost:4244
```

E em `.env.local.example` (commitado) também.

## 5. Smoke checklist (validação manual no browser ao fim)

- [ ] `npm run build` na pasta `frontend/admin/` → sem erros nem warnings.
- [ ] `npm start` sobe em `http://127.0.0.1:4244/` e redireciona pra `/login` quando não logado.
- [ ] Login com `admin@pethub.com / Admin@123` (ou `gerente@pethub.com / Gerente@123`, `operador@pethub.com / Operador@123`) → redireciona pra `/dashboard`.
- [ ] Login com cliente do seed (V3 — verificar emails em `db/migration/V3__seed_data.sql`) → toast "Sem permissão para acessar o admin"; não navega.
- [ ] Login com senha errada → toast "E-mail ou senha inválidos".
- [ ] Após F5 em `/dashboard`, sessão é restaurada via refresh; não redireciona pra login.
- [ ] Sidebar item ativo (Dashboard) destacado.
- [ ] Itens "em breve" (Catálogo, Pedidos, etc.) visíveis com opacity reduzida, não clicáveis.
- [ ] Toggle de pin: clicar fixa sidebar em 200px; clicar de novo volta pra 64px; estado persiste após F5.
- [ ] Mobile (DevTools): hambúrguer abre drawer; ESC ou click fora fecha.
- [ ] Botão "Sair" na topbar → POST /auth/logout, limpa session, redireciona pra `/login`.
- [ ] Badge de role mostra cor + label corretos (Operador azul, Gerente âmbar, Admin Loja vermelho).
- [ ] `prefers-reduced-motion`: animação da sidebar respeita.

## 6. Sequência de implementação sugerida

1. **Scaffold** — `ng new admin` em `frontend/admin/` com `--routing --style=scss --skip-tests --standalone`.
2. **Tailwind + tokens** — copiar `tailwind.config.js`, `postcss.config.js`, ajustar `styles.scss` com Tailwind directives.
3. **Path aliases + proxy + scripts** — `tsconfig.json` aliases, `proxy.conf.json`, ajustar `package.json scripts.start` pra porta 4244.
4. **CORS backend** — atualizar `.env.local` e `.env.local.example` adicionando 4244.
5. **Models de auth** — `core/models/auth.ts` + `core/models/errors.ts`.
6. **AdminAuthService + guards + interceptor** — `core/services/admin-auth.service.ts`, `core/guards/auth.guard.ts`, `core/guards/role.guard.ts`, `core/interceptors/auth.interceptor.ts`.
7. **App.config + app.component + app.routes** — providers, `tryRefreshOnBoot` no `ngOnInit`, routing inicial.
8. **Shared duplicados** — copiar Toast, ConfirmDialog, Skeleton do storefront.
9. **AdminShellPage** — layout topbar + sidebar com toggle + drawer mobile + wiring de toast/dialog hosts.
10. **LoginPage** — form, submit, error handling.
11. **DashboardHomePage** — placeholder 3 cards.
12. **Smoke E2E manual** — checklist §5.
13. **Atualizar docs** — `ai-memory/roadmap/fase-6-pendencias.md` (criar novo) + ROADMAP.md (marcar Fase 6 como 🚧 com slice 6.1 ✅).

## 7. Critérios de "pronto"

- [ ] Build verde em `frontend/admin/`.
- [ ] Smoke checklist §5 verde.
- [ ] CORS atualizado em `.env.local.example`.
- [ ] Commits Conventional Commits com escopo `feat(admin):` (~13 commits previstos).
- [ ] `ai-memory/roadmap/fase-6-pendencias.md` criado com histórico do slice 1.
- [ ] `ROADMAP.md` atualizado: Fase 6 ganha status 🚧 (slice 1 ✅, slices 2-5 pendentes).

## 8. Dívida técnica registrada (não-bloqueante neste slice)

- Testes formais (Karma/Jest) — mesmo bloqueio das fases 1-5.
- AppSec (LOG-2 mascaramento, JWT-1, CORS-1, VAL-1) — endereçar no caminho para staging.
- "Esqueci minha senha" — fluxo dedicado fora de escopo.
- Multi-loja / multi-tenant — fora da Fase 6.

## 9. Premissas técnicas

- Backend rodando em `http://localhost:8080` com Postgres em `:5433` e Redis em `:6380` (configuração atual).
- Usuários admin do seed:
  - `admin@pethub.com` / `Admin@123` (role ROLE_ADMIN_LOJA — V3)
  - `gerente@pethub.com` / `Gerente@123` (role ROLE_GERENTE — V16)
  - `operador@pethub.com` / `Operador@123` (role ROLE_OPERADOR — V16)
- Browser-alvo: Chrome/Firefox/Safari atuais.

## 🛡️ OWASP & Security Checkpoint

| Vulnerabilidade OWASP | Mitigação aplicada neste slice |
|---|---|
| **A01:2021 — Broken Access Control** | `authGuard` em todas as rotas internas. `roleGuard` exportado para slices futuros aplicarem granularmente. Backend já enforce via `@PreAuthorize` (linha de defesa primária). Frontend bloqueia login de `ROLE_CLIENTE` e invalida session via logout. |
| **A02:2021 — Cryptographic Failures** | Refresh token em cookie HttpOnly emitido pelo backend, nunca acessível pelo JS. Access token em memória (signal), perdido em F5 (recuperado via refresh). Sem localStorage/sessionStorage para tokens. |
| **A05:2021 — Security Misconfiguration** | CORS_ALLOWED_ORIGINS atualizado pra incluir `:4244` (sem wildcard). Headers de segurança (HSTS, CSP) ficam pra nginx em prod. |
| **A07:2021 — Authentication Failures** | TTL padrão 7 dias (sem opção "manter conectado" no admin — janela menor de exposição em caso de comprometimento). Rate limit no `/auth/login` já existe (Bucket4j, Fase 1). |
| **A09:2021 — Logging Failures** | Frontend não loga PII em `console.log` em produção (gate `environment.production`). Logout server-side ao detectar role inválida — evita sessão fantasma. |
| **API1:2023 — BOLA** | Endpoints admin já enforce ownership/perfil via `@PreAuthorize` no backend. Frontend não envia IDs forjados. |
| **API5:2023 — BFLA (Broken Function Level Authorization)** | Validação dupla: backend bloqueia endpoints admin para `ROLE_CLIENTE` (camada primária); frontend bloqueia entrada na app (UX). |

**Premissas de infra:**
- HTTPS obrigatório em staging/prod (atualmente HTTP em dev local — aceitável).
- `Content-Security-Policy` servido pelo nginx em prod (não vem do Angular).
- Cookies HttpOnly do refresh token com flag `Secure` em prod e `SameSite=Lax` (já configurado no backend desde Fase 1).
- Backend continua sendo a fonte de verdade pra permissões — frontend é UX, não barreira de segurança.
