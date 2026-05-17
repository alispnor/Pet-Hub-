# Admin Slice 6.1 — Foundation — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Entregar a fundação do app `frontend/admin/`: scaffold Angular 17 standalone, design tokens espelhando o storefront, AdminAuthService que rejeita login de ROLE_CLIENTE, AdminShellLayout (topbar + sidebar com toggle de pin), `/login` e `/dashboard` placeholder.

**Architecture:** Segundo app Angular 17 (standalone components + Tailwind 3) em `frontend/admin/`, separado do storefront. Mesmo backend, mesmo `POST /auth/login` (frontend valida que role do user é admin antes de persistir sessão). Refresh token em cookie HttpOnly (idêntico ao storefront). Porta dev 4244.

**Tech Stack:** Angular 17.3 standalone, TypeScript strict, Tailwind 3, Reactive Forms, RxJS (catchError/switchMap/shareReplay), signals para estado local.

**Spec:** `docs/superpowers/specs/2026-05-17-admin-slice1-foundation-design.md` (referência durante implementação).

**Convenção de commits:** `feat(admin):` ou `chore(admin):`, sufixo `slice1 NN-descricao` (NN = 01..14).

**Política de testes:** Tests formais Karma/Jest continuam dívida acumulada (mesmo bloqueio das fases 1-5). Verificação por task = `npm run build` no `frontend/admin/` + smoke programático/manual. Smoke checklist completa rodada na Task 14.

---

## File Structure

**Arquivos novos:**

```
frontend/admin/                                ⊕ TODA a pasta nova
├── angular.json
├── package.json + package-lock.json
├── proxy.conf.json
├── tailwind.config.js
├── postcss.config.js
├── tsconfig.json + tsconfig.app.json + tsconfig.spec.json
├── README.md
└── src/
    ├── index.html
    ├── main.ts
    ├── styles.scss
    ├── environments/
    │   ├── environment.ts
    │   └── environment.prod.ts
    └── app/
        ├── app.component.ts
        ├── app.config.ts
        ├── app.routes.ts
        ├── core/
        │   ├── models/
        │   │   ├── auth.ts
        │   │   └── errors.ts
        │   ├── services/
        │   │   └── admin-auth.service.ts
        │   ├── guards/
        │   │   ├── auth.guard.ts
        │   │   └── role.guard.ts
        │   ├── interceptors/
        │   │   └── auth.interceptor.ts
        │   └── layout/
        │       └── admin-shell/
        │           ├── admin-shell.page.ts
        │           ├── admin-shell.page.html
        │           └── admin-shell.page.scss
        ├── shared/
        │   ├── components/
        │   │   ├── skeleton/skeleton.component.ts
        │   │   ├── toast-stack/toast-stack.component.ts
        │   │   └── confirm-dialog/confirm-dialog.component.ts
        │   └── services/
        │       ├── toast.service.ts
        │       └── confirm-dialog.service.ts
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

**Arquivos modificados:**

- `.env.local` (gitignored) — adicionar `:4244` em `CORS_ALLOWED_ORIGINS`
- `.env.local.example` — mesma adição, commitada
- `ai-memory/roadmap/fase-6-pendencias.md` — criar novo (passa pra "Status: Slice 6.1 ✅ entregue...")
- `ROADMAP.md` — Fase 6 vira 🚧 (slice 1 ✅, slices 2-5 pendentes)

---

## Task 1: Scaffold do app Angular

**Files:**
- Create: pasta `frontend/admin/` inteira via Angular CLI

- [ ] **Step 1: Verificar que pasta admin está vazia**

```bash
ls /home/ali/projects/pet-hub/frontend/admin/ 2>&1
```

Expected: `ls: não foi possível acessar` (não existe) ou pasta vazia.

- [ ] **Step 2: Criar projeto Angular 17 standalone**

```bash
cd /home/ali/projects/pet-hub/frontend
npx -y @angular/cli@17 new admin --routing --style=scss --skip-tests=false --skip-git --strict --standalone --inline-style=false --inline-template=false
```

Quando perguntar sobre SSR/SSG, responder `N`.

Expected: Angular cria estrutura padrão em `frontend/admin/` com `npm install` automático.

- [ ] **Step 3: Verificar versão Angular**

```bash
cd /home/ali/projects/pet-hub/frontend/admin
grep '"@angular/core"' package.json
```

Expected: `"@angular/core": "^17.3.x"`.

- [ ] **Step 4: Smoke do build padrão**

```bash
npm run build
```

Expected: build sem erros (Angular generated default `app.component`).

- [ ] **Step 5: Commit scaffold**

```bash
cd /home/ali/projects/pet-hub
git add frontend/admin
git commit -m "feat(admin): slice1 01-angular 17 standalone scaffold"
```

---

## Task 2: Tailwind + design tokens + styles

**Files:**
- Create: `frontend/admin/tailwind.config.js`
- Create: `frontend/admin/postcss.config.js`
- Modify: `frontend/admin/src/styles.scss`
- Modify: `frontend/admin/package.json` (deps tailwind)

- [ ] **Step 1: Instalar Tailwind e plugins**

```bash
cd /home/ali/projects/pet-hub/frontend/admin
npm install -D tailwindcss@^3 postcss@^8 autoprefixer@^10 @tailwindcss/forms @tailwindcss/typography
```

- [ ] **Step 2: Criar `tailwind.config.js` (cópia exata do storefront)**

```bash
cp /home/ali/projects/pet-hub/frontend/storefront/tailwind.config.js /home/ali/projects/pet-hub/frontend/admin/tailwind.config.js
```

- [ ] **Step 3: Criar `postcss.config.js`**

Caminho: `frontend/admin/postcss.config.js`

```js
module.exports = {
  plugins: {
    tailwindcss: {},
    autoprefixer: {},
  },
};
```

- [ ] **Step 4: Substituir `styles.scss`**

Caminho: `frontend/admin/src/styles.scss`

```scss
@tailwind base;
@tailwind components;
@tailwind utilities;

@layer base {
  html, body { @apply h-full; }
  body { @apply bg-graphite-50 text-graphite-900 font-sans antialiased; }
}

@layer components {
  .btn-primary {
    @apply inline-flex items-center justify-center rounded-md bg-coral-600 text-white font-medium
           hover:bg-coral-700 disabled:bg-coral-300 disabled:cursor-not-allowed;
  }
  .btn-ghost {
    @apply inline-flex items-center justify-center rounded-md text-graphite-700
           hover:bg-graphite-100 disabled:opacity-50 disabled:cursor-not-allowed;
  }
  .max-w-page { @apply max-w-[1280px]; }
}
```

- [ ] **Step 5: Build**

```bash
cd /home/ali/projects/pet-hub/frontend/admin && npm run build
```

Expected: build sem erros.

- [ ] **Step 6: Commit**

```bash
cd /home/ali/projects/pet-hub
git add frontend/admin/package.json frontend/admin/package-lock.json \
        frontend/admin/tailwind.config.js frontend/admin/postcss.config.js \
        frontend/admin/src/styles.scss
git commit -m "feat(admin): slice1 02-tailwind config and design tokens"
```

---

## Task 3: Path aliases + proxy + porta dev

**Files:**
- Modify: `frontend/admin/tsconfig.json`
- Create: `frontend/admin/proxy.conf.json`
- Modify: `frontend/admin/package.json` (scripts.start)
- Create: `frontend/admin/src/environments/environment.ts`
- Create: `frontend/admin/src/environments/environment.prod.ts`

- [ ] **Step 1: Adicionar path aliases ao `tsconfig.json`**

Em `frontend/admin/tsconfig.json`, dentro de `compilerOptions`, adicionar:

```json
"baseUrl": "./",
"paths": {
  "@core/*": ["src/app/core/*"],
  "@shared/*": ["src/app/shared/*"],
  "@modules/*": ["src/app/modules/*"],
  "@env/*": ["src/environments/*"]
}
```

(Manter as outras opções `compilerOptions` que o CLI já gerou.)

- [ ] **Step 2: Criar `proxy.conf.json`**

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

- [ ] **Step 3: Atualizar `package.json` script `start`**

Em `frontend/admin/package.json`, substituir o script `"start"` existente por:

```json
"start": "ng serve --host 127.0.0.1 --port 4244 --proxy-config proxy.conf.json"
```

- [ ] **Step 4: Criar `environment.ts`**

Caminho: `frontend/admin/src/environments/environment.ts`

```ts
export const environment = {
  production: false,
  apiBase: '/api/v1',
};
```

- [ ] **Step 5: Criar `environment.prod.ts`**

Caminho: `frontend/admin/src/environments/environment.prod.ts`

```ts
export const environment = {
  production: true,
  apiBase: '/api/v1',
};
```

- [ ] **Step 6: Configurar fileReplacements em `angular.json`**

Em `frontend/admin/angular.json`, no bloco `configurations.production`, adicionar (ou complementar) `fileReplacements`:

```json
"fileReplacements": [
  {
    "replace": "src/environments/environment.ts",
    "with": "src/environments/environment.prod.ts"
  }
]
```

- [ ] **Step 7: Build**

```bash
cd /home/ali/projects/pet-hub/frontend/admin && npm run build
```

Expected: build sem erros.

- [ ] **Step 8: Commit**

```bash
cd /home/ali/projects/pet-hub
git add frontend/admin/tsconfig.json frontend/admin/proxy.conf.json \
        frontend/admin/package.json frontend/admin/angular.json \
        frontend/admin/src/environments
git commit -m "feat(admin): slice1 03-path aliases proxy and dev port 4244"
```

---

## Task 4: Backend CORS para porta 4244

**Files:**
- Modify: `.env.local`
- Modify: `.env.local.example`

- [ ] **Step 1: Atualizar `.env.local.example`**

Substituir a linha existente de `CORS_ALLOWED_ORIGINS` por:

```
CORS_ALLOWED_ORIGINS=http://127.0.0.1:4242,http://localhost:4242,http://localhost:4200,http://localhost:4201,http://127.0.0.1:4244,http://localhost:4244
```

- [ ] **Step 2: Atualizar `.env.local`**

Mesma alteração no `.env.local` (gitignored — usado pelo container rodando).

- [ ] **Step 3: Reiniciar backend pra pegar nova CORS**

```bash
docker rm -f pethub-backend
cd /home/ali/projects/pet-hub
docker run -d --name pethub-backend --network host --env-file .env.local \
  -v "$PWD/backend/application/target/pet-hub-backend.jar:/app/app.jar:ro" \
  -v "$PWD/backend/application/var:/app/var" \
  -w /app eclipse-temurin:21-jre java -jar /app/app.jar

sleep 15 && curl -s -o /dev/null -w "backend health: %{http_code}\n" http://localhost:8080/actuator/health
```

Expected: backend health 200.

- [ ] **Step 4: Commit (.env.local.example)**

```bash
git add .env.local.example
git commit -m "chore(dev): slice1 04-add admin port 4244 to CORS"
```

(`.env.local` é gitignored, não entra no commit.)

---

## Task 5: Models de auth + errors

**Files:**
- Create: `frontend/admin/src/app/core/models/auth.ts`
- Create: `frontend/admin/src/app/core/models/errors.ts`

- [ ] **Step 1: Criar `auth.ts`**

```ts
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

export const ROLES_ADMIN: readonly Role[] = [
  'ROLE_ADMIN_LOJA',
  'ROLE_GERENTE',
  'ROLE_OPERADOR',
];

export const ROLE_LABEL: Record<Role, string> = {
  ROLE_CLIENTE: 'Cliente',
  ROLE_ADMIN_LOJA: 'Admin Loja',
  ROLE_GERENTE: 'Gerente',
  ROLE_OPERADOR: 'Operador',
};

export const ROLE_BADGE_CLASSES: Record<Role, string> = {
  ROLE_CLIENTE: 'bg-graphite-100 text-graphite-700',
  ROLE_OPERADOR: 'bg-blue-100 text-blue-800',
  ROLE_GERENTE: 'bg-amber-100 text-amber-800',
  ROLE_ADMIN_LOJA: 'bg-red-100 text-red-800',
};
```

- [ ] **Step 2: Criar `errors.ts`**

```ts
export class ForbiddenLoginError extends Error {
  constructor() {
    super('Forbidden login: user does not have admin role');
    this.name = 'ForbiddenLoginError';
  }
}
```

- [ ] **Step 3: Build**

```bash
cd /home/ali/projects/pet-hub/frontend/admin && npm run build
```

Expected: build sem erros (modelos puros, sem dependência de outras camadas).

- [ ] **Step 4: Commit**

```bash
cd /home/ali/projects/pet-hub
git add frontend/admin/src/app/core/models
git commit -m "feat(admin): slice1 05-auth models and errors"
```

---

## Task 6: AdminAuthService

**Files:**
- Create: `frontend/admin/src/app/core/services/admin-auth.service.ts`

- [ ] **Step 1: Criar `admin-auth.service.ts`**

```ts
import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';
import { catchError, finalize, map, shareReplay, switchMap, tap } from 'rxjs/operators';

import { environment } from '@env/environment';
import {
  AdminUser,
  AuthResponse,
  LoginRequest,
  Role,
  ROLES_ADMIN,
} from '@core/models/auth';
import { ForbiddenLoginError } from '@core/models/errors';

@Injectable({ providedIn: 'root' })
export class AdminAuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly api = environment.apiBase;

  private readonly _accessToken = signal<string | null>(null);
  private readonly _user = signal<AdminUser | null>(null);
  private refreshGate$: Observable<string> | null = null;

  readonly accessToken = this._accessToken.asReadonly();
  readonly currentUser = this._user.asReadonly();
  readonly currentUserRole = computed<Role | null>(() => this._user()?.role ?? null);
  readonly isAuthenticated = computed(() => this._accessToken() !== null);
  readonly isOperador = computed(() => this.currentUserRole() === 'ROLE_OPERADOR');
  readonly isGerente = computed(() => this.currentUserRole() === 'ROLE_GERENTE');
  readonly isAdmin = computed(() => this.currentUserRole() === 'ROLE_ADMIN_LOJA');

  login(req: LoginRequest): Observable<AdminUser> {
    return this.http
      .post<AuthResponse>(`${this.api}/auth/login`, req, { withCredentials: true })
      .pipe(
        switchMap(res => {
          if (!ROLES_ADMIN.includes(res.usuario.role)) {
            return this.http
              .post(`${this.api}/auth/logout`, {}, { withCredentials: true })
              .pipe(
                catchError(() => of(null)),
                switchMap(() => throwError(() => new ForbiddenLoginError())),
              );
          }
          this.storeSession(res);
          return of(res.usuario);
        }),
      );
  }

  refresh(): Observable<string> {
    if (this.refreshGate$) {
      return this.refreshGate$;
    }
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
      catchError(() => {
        this.clearSession();
        return of(false);
      }),
    );
  }

  logout(): Observable<void> {
    return this.http
      .post<void>(`${this.api}/auth/logout`, {}, { withCredentials: true })
      .pipe(
        tap({
          next: () => this.finalizarLogout(),
          error: () => this.finalizarLogout(),
        }),
      );
  }

  private finalizarLogout(): void {
    this.clearSession();
    this.router.navigateByUrl('/login');
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

- [ ] **Step 2: Build**

```bash
cd /home/ali/projects/pet-hub/frontend/admin && npm run build
```

Expected: build sem erros.

- [ ] **Step 3: Commit**

```bash
cd /home/ali/projects/pet-hub
git add frontend/admin/src/app/core/services/admin-auth.service.ts
git commit -m "feat(admin): slice1 06-admin auth service with role validation"
```

---

## Task 7: Guards (auth + role factory)

**Files:**
- Create: `frontend/admin/src/app/core/guards/auth.guard.ts`
- Create: `frontend/admin/src/app/core/guards/role.guard.ts`

- [ ] **Step 1: Criar `auth.guard.ts`**

```ts
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AdminAuthService } from '@core/services/admin-auth.service';

export const authGuard: CanActivateFn = () => {
  const auth = inject(AdminAuthService);
  const router = inject(Router);
  return auth.isAuthenticated() ? true : router.parseUrl('/login');
};
```

- [ ] **Step 2: Criar `role.guard.ts`**

```ts
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AdminAuthService } from '@core/services/admin-auth.service';
import { Role } from '@core/models/auth';

export function roleGuard(rolesPermitidas: Role[]): CanActivateFn {
  return () => {
    const auth = inject(AdminAuthService);
    const router = inject(Router);
    const role = auth.currentUserRole();
    if (!role) {
      return router.parseUrl('/login');
    }
    if (!rolesPermitidas.includes(role)) {
      return router.parseUrl('/dashboard');
    }
    return true;
  };
}
```

- [ ] **Step 3: Build**

```bash
cd /home/ali/projects/pet-hub/frontend/admin && npm run build
```

- [ ] **Step 4: Commit**

```bash
cd /home/ali/projects/pet-hub
git add frontend/admin/src/app/core/guards
git commit -m "feat(admin): slice1 07-auth and role guards"
```

---

## Task 8: HTTP authInterceptor

**Files:**
- Create: `frontend/admin/src/app/core/interceptors/auth.interceptor.ts`

- [ ] **Step 1: Criar `auth.interceptor.ts`**

```ts
import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';

import { AdminAuthService } from '@core/services/admin-auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AdminAuthService);
  const token = auth.accessToken();
  const reqAutenticada = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(reqAutenticada).pipe(
    catchError((erro: HttpErrorResponse) => {
      if (erro.status !== 401 || req.url.includes('/auth/')) {
        return throwError(() => erro);
      }
      return auth.refresh().pipe(
        switchMap(novoToken =>
          next(req.clone({ setHeaders: { Authorization: `Bearer ${novoToken}` } })),
        ),
        catchError(erroRefresh => {
          auth.logout().subscribe();
          return throwError(() => erroRefresh);
        }),
      );
    }),
  );
};
```

- [ ] **Step 2: Build**

```bash
cd /home/ali/projects/pet-hub/frontend/admin && npm run build
```

- [ ] **Step 3: Commit**

```bash
cd /home/ali/projects/pet-hub
git add frontend/admin/src/app/core/interceptors
git commit -m "feat(admin): slice1 08-auth http interceptor with refresh retry"
```

---

## Task 9: App config + component + rotas iniciais (com stubs)

**Files:**
- Modify: `frontend/admin/src/app/app.config.ts`
- Modify: `frontend/admin/src/app/app.component.ts`
- Modify: `frontend/admin/src/app/app.routes.ts`
- Create: `frontend/admin/src/app/modules/auth/pages/login/login.page.ts` (stub)
- Create: `frontend/admin/src/app/modules/dashboard/pages/home/home.page.ts` (stub)
- Create: `frontend/admin/src/app/core/layout/admin-shell/admin-shell.page.ts` (stub)

- [ ] **Step 1: Substituir `app.config.ts`**

```ts
import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';

import { routes } from './app.routes';
import { authInterceptor } from '@core/interceptors/auth.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withFetch(), withInterceptors([authInterceptor])),
  ],
};
```

- [ ] **Step 2: Substituir `app.component.ts`**

```ts
import { Component, OnInit, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { AdminAuthService } from '@core/services/admin-auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  template: `<router-outlet />`,
})
export class AppComponent implements OnInit {
  private readonly auth = inject(AdminAuthService);

  ngOnInit(): void {
    this.auth.tryRefreshOnBoot().subscribe();
  }
}
```

- [ ] **Step 3: Criar stub `login.page.ts`**

```ts
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-login-page',
  standalone: true,
  imports: [CommonModule],
  template: `<section class="p-6"><h1 class="text-2xl">Login</h1><p>Stub (Task 12).</p></section>`,
})
export class LoginPage {}
```

- [ ] **Step 4: Criar stub `home.page.ts` (dashboard)**

```ts
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-dashboard-home-page',
  standalone: true,
  imports: [CommonModule],
  template: `<section><h1 class="text-2xl">Dashboard</h1><p>Stub (Task 13).</p></section>`,
})
export class DashboardHomePage {}
```

- [ ] **Step 5: Criar stub `admin-shell.page.ts`**

```ts
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-admin-shell-page',
  standalone: true,
  imports: [CommonModule, RouterOutlet],
  template: `<div class="p-4"><p class="text-graphite-500 text-xs mb-4">Shell stub (Task 11)</p><router-outlet /></div>`,
})
export class AdminShellPage {}
```

- [ ] **Step 6: Substituir `app.routes.ts`**

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

- [ ] **Step 7: Build**

```bash
cd /home/ali/projects/pet-hub/frontend/admin && npm run build
```

Expected: build sem erros. Rotas funcionando, todas direcionando pra stubs.

- [ ] **Step 8: Commit**

```bash
cd /home/ali/projects/pet-hub
git add frontend/admin/src/app/app.config.ts \
        frontend/admin/src/app/app.component.ts \
        frontend/admin/src/app/app.routes.ts \
        frontend/admin/src/app/modules \
        frontend/admin/src/app/core/layout
git commit -m "feat(admin): slice1 09-app config component routes and stubs"
```

---

## Task 10: Shared components (Skeleton, ToastService, ConfirmDialog)

**Files:**
- Create: `frontend/admin/src/app/shared/components/skeleton/skeleton.component.ts`
- Create: `frontend/admin/src/app/shared/components/toast-stack/toast-stack.component.ts`
- Create: `frontend/admin/src/app/shared/components/confirm-dialog/confirm-dialog.component.ts`
- Create: `frontend/admin/src/app/shared/services/toast.service.ts`
- Create: `frontend/admin/src/app/shared/services/confirm-dialog.service.ts`

- [ ] **Step 1: Criar `skeleton.component.ts`**

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

- [ ] **Step 2: Criar `toast.service.ts`**

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

- [ ] **Step 3: Criar `toast-stack.component.ts`**

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

- [ ] **Step 4: Criar `confirm-dialog.service.ts`**

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

- [ ] **Step 5: Criar `confirm-dialog.component.ts`**

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
            <button type="button" class="btn-ghost text-sm py-2 px-3" (click)="cancelar()">Cancelar</button>
            <button type="button"
                    [class]="pendente.acaoVariant === 'danger' ? 'btn-danger' : 'btn-primary'"
                    class="text-sm py-2 px-3" (click)="confirmar()">
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

- [ ] **Step 6: Build**

```bash
cd /home/ali/projects/pet-hub/frontend/admin && npm run build
```

Expected: build sem erros.

- [ ] **Step 7: Commit**

```bash
cd /home/ali/projects/pet-hub
git add frontend/admin/src/app/shared
git commit -m "feat(admin): slice1 10-shared skeleton toast and dialog components"
```

---

## Task 11: AdminShellPage (topbar + sidebar com toggle de pin)

**Files:**
- Modify: `frontend/admin/src/app/core/layout/admin-shell/admin-shell.page.ts`
- Create: `frontend/admin/src/app/core/layout/admin-shell/admin-shell.page.html`
- Create: `frontend/admin/src/app/core/layout/admin-shell/admin-shell.page.scss`

- [ ] **Step 1: Substituir `admin-shell.page.ts`**

```ts
import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AdminAuthService } from '@core/services/admin-auth.service';
import { Role, ROLE_BADGE_CLASSES, ROLE_LABEL } from '@core/models/auth';
import { ToastStackComponent } from '@shared/components/toast-stack/toast-stack.component';
import { ConfirmDialogComponent } from '@shared/components/confirm-dialog/confirm-dialog.component';

interface ItemMenu {
  rota: string;
  rotulo: string;
  icone: string;
  habilitada: boolean;
  exact?: boolean;
}

const CHAVE_SIDEBAR_PINADA = 'pethub:admin:sidebar:pinned';

@Component({
  selector: 'app-admin-shell-page',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    ToastStackComponent,
    ConfirmDialogComponent,
  ],
  templateUrl: './admin-shell.page.html',
  styleUrls: ['./admin-shell.page.scss'],
})
export class AdminShellPage {
  private readonly auth = inject(AdminAuthService);

  readonly user = this.auth.currentUser;
  readonly sidebarPinada = signal<boolean>(this.lerEstadoInicial());
  readonly drawerAberto = signal(false);

  readonly badgeClasses = computed(() => {
    const role = this.user()?.role;
    return role ? ROLE_BADGE_CLASSES[role] : '';
  });

  readonly rotuloRole = computed(() => {
    const role = this.user()?.role;
    return role ? ROLE_LABEL[role] : '';
  });

  readonly itensMenu: ItemMenu[] = [
    { rota: '/dashboard',     rotulo: 'Dashboard',     icone: '📊', habilitada: true, exact: true },
    { rota: '/produtos',      rotulo: 'Catálogo',      icone: '📦', habilitada: false },
    { rota: '/pedidos',       rotulo: 'Pedidos',       icone: '📋', habilitada: false },
    { rota: '/comercial',     rotulo: 'Comercial',     icone: '🏷️', habilitada: false },
    { rota: '/clientes',      rotulo: 'Clientes',      icone: '👥', habilitada: false },
    { rota: '/relatorios',    rotulo: 'Relatórios',    icone: '📈', habilitada: false },
    { rota: '/configuracoes', rotulo: 'Configurações', icone: '⚙️', habilitada: false },
  ];

  alternarPin(): void {
    const novoEstado = !this.sidebarPinada();
    this.sidebarPinada.set(novoEstado);
    localStorage.setItem(CHAVE_SIDEBAR_PINADA, novoEstado ? '1' : '0');
  }

  abrirDrawer(): void { this.drawerAberto.set(true); }
  fecharDrawer(): void { this.drawerAberto.set(false); }

  sair(): void {
    this.auth.logout().subscribe();
  }

  rotularRole(role: Role | undefined): string {
    return role ? ROLE_LABEL[role] : '';
  }

  classesBadgeRole(role: Role | undefined): string {
    return role ? ROLE_BADGE_CLASSES[role] : '';
  }

  private lerEstadoInicial(): boolean {
    return localStorage.getItem(CHAVE_SIDEBAR_PINADA) === '1';
  }
}
```

- [ ] **Step 2: Criar `admin-shell.page.html`**

```html
<div class="min-h-screen flex flex-col bg-graphite-50">
  <header class="h-14 border-b border-graphite-200 bg-graphite-0 flex items-center px-4 gap-4">
    <button type="button" class="md:hidden p-2 -ml-2 text-graphite-700"
            (click)="abrirDrawer()" aria-label="Abrir menu">☰</button>
    <h1 class="font-display font-semibold text-graphite-900">Pet Hub Admin</h1>
    <div class="ml-auto flex items-center gap-3">
      <span class="text-sm text-graphite-700 hidden sm:inline">{{ user()?.nome }}</span>
      <span [class]="badgeClasses()" class="rounded-full px-2 py-0.5 text-xs">
        {{ rotuloRole() }}
      </span>
      <button type="button" class="btn-ghost text-sm py-1.5 px-3" (click)="sair()">Sair</button>
    </div>
  </header>

  <div class="flex flex-1 overflow-hidden">
    <aside [class.w-16]="!sidebarPinada()" [class.w-50]="sidebarPinada()"
           class="hidden md:flex flex-col border-r border-graphite-200 bg-graphite-0 transition-all overflow-y-auto">
      <button type="button" (click)="alternarPin()"
              [attr.aria-label]="sidebarPinada() ? 'Recolher sidebar' : 'Fixar sidebar'"
              [attr.aria-pressed]="sidebarPinada()"
              class="self-end p-2 text-graphite-500 hover:text-graphite-700">📌</button>
      <ng-container *ngTemplateOutlet="navItens; context: { compacta: !sidebarPinada() }"></ng-container>
    </aside>

    <div *ngIf="drawerAberto()" class="md:hidden fixed inset-0 z-40 flex" (keydown.escape)="fecharDrawer()">
      <div class="absolute inset-0 bg-graphite-900/40" (click)="fecharDrawer()" aria-hidden="true"></div>
      <aside class="relative flex flex-col w-64 bg-graphite-0 px-2 py-4 shadow-xl animate-slide-in">
        <button type="button" (click)="fecharDrawer()" class="self-end p-2 text-graphite-700"
                aria-label="Fechar menu">✕</button>
        <ng-container *ngTemplateOutlet="navItens; context: { compacta: false }"></ng-container>
      </aside>
    </div>

    <main class="flex-1 p-6 md:p-8 max-w-page mx-auto w-full overflow-y-auto">
      <router-outlet />
    </main>
  </div>

  <app-toast-stack />
  <app-confirm-dialog />
</div>

<ng-template #navItens let-compacta="compacta">
  <nav class="flex-1 flex flex-col gap-1 px-2 py-2" aria-label="Navegação principal">
    <a *ngFor="let item of itensMenu"
       [routerLink]="item.habilitada ? item.rota : null"
       [routerLinkActiveOptions]="{ exact: !!item.exact }"
       routerLinkActive="bg-coral-50 text-coral-700"
       [attr.aria-disabled]="!item.habilitada ? 'true' : null"
       [class.pointer-events-none]="!item.habilitada"
       [class.opacity-50]="!item.habilitada"
       class="flex items-center gap-3 rounded-md px-3 py-2 text-sm text-graphite-700 hover:bg-graphite-100"
       [title]="compacta ? item.rotulo : ''"
       [attr.aria-label]="item.rotulo"
       (click)="fecharDrawer()">
      <span aria-hidden="true">{{ item.icone }}</span>
      <span *ngIf="!compacta">{{ item.rotulo }}</span>
      <span *ngIf="!compacta && !item.habilitada" class="ml-auto text-xs text-graphite-500">em breve</span>
    </a>
  </nav>
</ng-template>
```

- [ ] **Step 3: Criar `admin-shell.page.scss`**

```scss
.w-50 {
  width: 12.5rem; /* 200px */
}

@keyframes slide-in {
  from { transform: translateX(-100%); }
  to { transform: translateX(0); }
}
.animate-slide-in { animation: slide-in 200ms ease-out; }

@media (prefers-reduced-motion: reduce) {
  .animate-slide-in {
    animation: none;
  }
}
```

- [ ] **Step 4: Build**

```bash
cd /home/ali/projects/pet-hub/frontend/admin && npm run build
```

Expected: build sem erros.

- [ ] **Step 5: Commit**

```bash
cd /home/ali/projects/pet-hub
git add frontend/admin/src/app/core/layout/admin-shell
git commit -m "feat(admin): slice1 11-admin shell with topbar and sidebar pin"
```

---

## Task 12: LoginPage

**Files:**
- Modify: `frontend/admin/src/app/modules/auth/pages/login/login.page.ts`
- Create: `frontend/admin/src/app/modules/auth/pages/login/login.page.html`

- [ ] **Step 1: Substituir `login.page.ts`**

```ts
import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';

import { AdminAuthService } from '@core/services/admin-auth.service';
import { ForbiddenLoginError } from '@core/models/errors';
import { ToastService } from '@shared/services/toast.service';
import { ToastStackComponent } from '@shared/components/toast-stack/toast-stack.component';

@Component({
  selector: 'app-login-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, ToastStackComponent],
  templateUrl: './login.page.html',
})
export class LoginPage {
  private readonly auth = inject(AdminAuthService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  readonly submetendo = signal(false);

  readonly formulario = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    senha: ['', [Validators.required, Validators.minLength(8)]],
  });

  submeter(): void {
    if (this.formulario.invalid || this.submetendo()) {
      return;
    }
    this.submetendo.set(true);
    const valor = this.formulario.getRawValue();
    this.auth.login({ email: valor.email, senha: valor.senha }).subscribe({
      next: () => {
        this.submetendo.set(false);
        this.router.navigateByUrl('/dashboard');
      },
      error: (erro) => {
        this.submetendo.set(false);
        if (erro instanceof ForbiddenLoginError) {
          this.toast.error('Sem permissão para acessar o admin.');
        } else if (erro instanceof HttpErrorResponse && erro.status === 401) {
          this.toast.error('E-mail ou senha inválidos.');
        } else {
          this.toast.error('Erro ao entrar. Tente novamente.');
        }
      },
    });
  }
}
```

- [ ] **Step 2: Criar `login.page.html`**

```html
<section class="min-h-screen flex items-center justify-center bg-graphite-50 px-4">
  <div class="w-full max-w-sm">
    <header class="text-center mb-8">
      <h1 class="text-2xl font-display font-semibold text-graphite-900">Pet Hub Admin</h1>
      <p class="mt-1 text-sm text-graphite-600">Entre com sua conta administrativa</p>
    </header>

    <form [formGroup]="formulario" (ngSubmit)="submeter()"
          class="rounded-lg border border-graphite-200 bg-graphite-0 p-6 space-y-4 shadow-sm">

      <div>
        <label class="block text-sm font-medium text-graphite-700" for="email">E-mail</label>
        <input id="email" type="email" formControlName="email" autocomplete="email"
               class="mt-1 w-full rounded-md border-graphite-300" />
        <p *ngIf="formulario.controls.email.touched && formulario.controls.email.errors?.['email']"
           class="text-xs text-red-600 mt-1">Informe um e-mail válido.</p>
      </div>

      <div>
        <label class="block text-sm font-medium text-graphite-700" for="senha">Senha</label>
        <input id="senha" type="password" formControlName="senha" autocomplete="current-password"
               class="mt-1 w-full rounded-md border-graphite-300" />
        <p *ngIf="formulario.controls.senha.touched && formulario.controls.senha.errors?.['minlength']"
           class="text-xs text-red-600 mt-1">Senha deve ter pelo menos 8 caracteres.</p>
      </div>

      <button type="submit" class="btn-primary w-full py-2"
              [disabled]="formulario.invalid || submetendo()">
        {{ submetendo() ? 'Entrando...' : 'Entrar' }}
      </button>

      <p class="text-xs text-graphite-500 text-center">
        <span class="opacity-50">Esqueci minha senha</span> · em breve
      </p>
    </form>
  </div>
  <app-toast-stack />
</section>
```

- [ ] **Step 3: Build**

```bash
cd /home/ali/projects/pet-hub/frontend/admin && npm run build
```

Expected: build sem erros.

- [ ] **Step 4: Commit**

```bash
cd /home/ali/projects/pet-hub
git add frontend/admin/src/app/modules/auth/pages/login
git commit -m "feat(admin): slice1 12-login page with role validation feedback"
```

---

## Task 13: DashboardHomePage (placeholder real)

**Files:**
- Modify: `frontend/admin/src/app/modules/dashboard/pages/home/home.page.ts`
- Create: `frontend/admin/src/app/modules/dashboard/pages/home/home.page.html`

- [ ] **Step 1: Substituir `home.page.ts`**

```ts
import { Component, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';

import { AdminAuthService } from '@core/services/admin-auth.service';
import { ROLE_LABEL } from '@core/models/auth';

@Component({
  selector: 'app-dashboard-home-page',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './home.page.html',
})
export class DashboardHomePage {
  private readonly auth = inject(AdminAuthService);

  readonly user = this.auth.currentUser;
  readonly rotuloRole = computed(() => {
    const role = this.user()?.role;
    return role ? ROLE_LABEL[role] : '';
  });
}
```

- [ ] **Step 2: Criar `home.page.html`**

```html
<section>
  <h1 class="text-2xl font-display font-semibold text-graphite-900">Dashboard</h1>
  <p class="mt-1 text-sm text-graphite-600">
    Olá, {{ user()?.nome }} · {{ rotuloRole() }}.
  </p>

  <div class="mt-6 grid gap-4 grid-cols-1 md:grid-cols-3">
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

  <div class="mt-8 rounded-lg border border-dashed border-graphite-300 bg-graphite-0 p-6 text-sm text-graphite-600">
    <p class="font-medium text-graphite-700 mb-2">Slice 6.1 entregue</p>
    <p>Esta fundação habilita os próximos slices da Fase 6:</p>
    <ul class="mt-2 list-disc list-inside space-y-0.5 text-graphite-600">
      <li>Slice 6.2 — Catálogo (produtos, categorias)</li>
      <li>Slice 6.3 — Operações (estoque, pedidos, etiqueta PDF)</li>
      <li>Slice 6.4 — Comercial (cupons, promoções, impostos)</li>
      <li>Slice 6.5 — Insights (dashboards reais, clientes, audit log, SSE)</li>
    </ul>
  </div>
</section>
```

- [ ] **Step 3: Build**

```bash
cd /home/ali/projects/pet-hub/frontend/admin && npm run build
```

Expected: build sem erros.

- [ ] **Step 4: Commit**

```bash
cd /home/ali/projects/pet-hub
git add frontend/admin/src/app/modules/dashboard/pages/home
git commit -m "feat(admin): slice1 13-dashboard placeholder with role greeting"
```

---

## Task 14: Smoke E2E + docs (fase-6-pendencias, ROADMAP)

**Files:**
- Create: `ai-memory/roadmap/fase-6-pendencias.md`
- Modify: `ROADMAP.md`

- [ ] **Step 1: Build final**

```bash
cd /home/ali/projects/pet-hub/frontend/admin && npm run build
```

Expected: build sem erros nem warnings novos.

- [ ] **Step 2: Subir admin em dev e verificar**

```bash
cd /home/ali/projects/pet-hub/frontend/admin && npm start &
sleep 30 && curl -s -o /dev/null -w "admin /login: %{http_code}\n" http://127.0.0.1:4244/login
```

Expected: HTTP 200 servindo a SPA (Angular faz fallback pra index.html em qualquer rota).

- [ ] **Step 3: Smoke checklist (manual no browser, §5 do spec)**

Storefront em :4242 (Slice 4), backend em :8080. Abrir `http://127.0.0.1:4244/`. Verificar:

- [ ] Sem sessão, redireciona para `/login`.
- [ ] Login com `admin@pethub.com / Admin@123` → redireciona para `/dashboard`.
- [ ] Login com `gerente@pethub.com / Gerente@123` → dashboard, badge âmbar "Gerente".
- [ ] Login com `operador@pethub.com / Operador@123` → dashboard, badge azul "Operador".
- [ ] Login com cliente do seed V3 → toast "Sem permissão para acessar o admin", não navega.
- [ ] Login com senha errada → toast "E-mail ou senha inválidos".
- [ ] Após F5 em `/dashboard`, sessão é restaurada via refresh.
- [ ] Sidebar item Dashboard destacado; demais itens com opacidade 50% e "em breve".
- [ ] Toggle de pin expande sidebar pra 200px; estado persiste após F5.
- [ ] Mobile (DevTools): hambúrguer abre drawer; ESC fecha; click fora fecha.
- [ ] Sair na topbar → POST /auth/logout, limpa session, redireciona pra `/login`.

Documentar qualquer falha encontrada e corrigir antes de seguir.

- [ ] **Step 4: Parar admin dev**

```bash
pkill -f "ng serve.*4244" 2>/dev/null || true
```

- [ ] **Step 5: Criar `ai-memory/roadmap/fase-6-pendencias.md`**

```markdown
# Fase 6 — Pendências e checklist

> **Status:** Slice 6.1 ✅ entregue em 2026-05-17. Slices 6.2-6.5 ⏳ pendentes.
> Testes Karma/Jest formais continuam dívida acumulada (mesmo bloqueio das Fases 1-5).

## ✅ Slices entregues

### Slice 6.1 — Foundation (`<sha-task-1>` → `<sha-task-13>`)

14 commits sequenciais entregues em 2026-05-17:
- `01-angular 17 standalone scaffold`
- `02-tailwind config and design tokens`
- `03-path aliases proxy and dev port 4244`
- `04-add admin port 4244 to CORS`
- `05-auth models and errors`
- `06-admin auth service with role validation`
- `07-auth and role guards`
- `08-auth http interceptor with refresh retry`
- `09-app config component routes and stubs`
- `10-shared skeleton toast and dialog components`
- `11-admin shell with topbar and sidebar pin`
- `12-login page with role validation feedback`
- `13-dashboard placeholder with role greeting`
- `14-roadmap fase 6 slice 1 entregue` (este commit)

**Decisões congeladas (Slice 6.1):**
- Dois apps Angular separados (storefront + admin), não SPA único com módulos.
- Code sharing entre apps = duplicação (não pacote shared) — velocidade > DRY no momento.
- Visual: mesma paleta `coral`/`graphite` do storefront, layout admin-style (sidebar densa 64px com toggle de pin pra 200px, topbar fina 56px).
- Login = mesmo endpoint `POST /auth/login`; frontend rejeita `ROLE_CLIENTE` invocando `POST /auth/logout` e disparando `ForbiddenLoginError`.
- Sem opção "manter conectado" no admin (TTL padrão 7 dias).
- Refresh em cookie HttpOnly (mesma estratégia do storefront).
- Estado da sidebar (pinada) persistido em `localStorage` chave `pethub:admin:sidebar:pinned`.
- Porta dev 4244; CORS atualizado em `.env.local.example` e `.env.local`.
- `roleGuard` implementado e exportado, mas não aplicado em nenhuma rota neste slice — disponível pros slices 6.2+.

## ⏳ Slices 6.2-6.5 pendentes

Plano em [`ai-memory/roadmap/fase-6-decomposicao.md`](./fase-6-decomposicao.md).

- **6.2 Catálogo** — Produtos (lista+detalhe+criar/editar, DnD imagens), Categorias (CRUD). ~5-6h.
- **6.3 Operações** — Estoque, Pedidos admin (transições, etiqueta PDF), audit log básico. ~6-7h.
- **6.4 Comercial** — Cupons, Promoções, Regras de Imposto (CRUDs + endpoints backend novos). ~5-6h.
- **6.5 Insights** — Dashboard com ApexCharts, Clientes admin, Audit log UI, SSE para novos pedidos. ~5-6h.

## ⚠️ Dívida técnica

### Testes formais
Mesmo bloqueio das fases 1-5 — sem CI rodando ainda. Pendências catalogadas pra um pacote dedicado quando entrar TDD na Fase 11+.

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

# Admin (slice 6.1)
cd frontend/admin && npm start  # http://127.0.0.1:4244
```

Storefront em :4242, admin em :4244 — rodam lado a lado.

**Próximo passo: Slice 6.2 (Catálogo).**
```

- [ ] **Step 6: Atualizar `ROADMAP.md`**

Encontrar a linha (aproximadamente):

```
## Fase 6 — Frontend Admin (Back-Office) · ⬜
```

Substituir por:

```
## Fase 6 — Frontend Admin (Back-Office) · 🚧 (slice 6.1 ✅, slices 6.2-6.5 pendentes)

> **Slice 6.1 entregue em 2026-05-17.** Scaffold `frontend/admin` (Angular 17, porta 4244), AdminAuthService com validação de role no login, AdminShell (topbar + sidebar com toggle de pin), `/login` e `/dashboard` placeholder. Plano dos 5 slices em [`ai-memory/roadmap/fase-6-decomposicao.md`](./ai-memory/roadmap/fase-6-decomposicao.md); histórico em [`ai-memory/roadmap/fase-6-pendencias.md`](./ai-memory/roadmap/fase-6-pendencias.md).
```

- [ ] **Step 7: Commit final**

```bash
cd /home/ali/projects/pet-hub
git add ai-memory/roadmap/fase-6-pendencias.md ROADMAP.md
git commit -m "$(cat <<'EOF'
docs(roadmap): slice 6.1 entregue — admin foundation

Scaffold frontend/admin Angular 17 com auth (rejeita ROLE_CLIENTE),
shell (topbar + sidebar com pin), login e dashboard placeholder.
14 commits sequenciais. Build verde por task; smoke programático
(HTTP 200 em /login) ok.

Próximo slice: 6.2 (Catálogo).
EOF
)"
```

- [ ] **Step 8: Verificar histórico dos commits do slice**

```bash
git log --oneline | grep "slice1" | head -16
```

Expected: 14 commits sequenciais com prefixo `slice1` + 1 commit final `docs(roadmap)`.

---

## Verificação final

**Critérios de "pronto" (do spec §7):**

- [ ] Build verde em `frontend/admin/`.
- [ ] Smoke checklist §5 do spec verde (manual).
- [ ] CORS atualizado em `.env.local.example`.
- [ ] Commits Conventional Commits com escopo `feat(admin):` ou `chore(admin):` ou `chore(dev):` (~14 commits).
- [ ] `ai-memory/roadmap/fase-6-pendencias.md` criado com histórico do slice 1.
- [ ] `ROADMAP.md` atualizado: Fase 6 ganha status 🚧 (slice 1 ✅, slices 2-5 pendentes).

**Dívida técnica registrada:**

- Testes formais (Karma/Jest) — mesmo bloqueio das fases 1-5.
- AppSec (LOG-2, JWT-1, CORS-1, VAL-1) — staging path.
- "Esqueci minha senha" — backlog.
- Multi-loja / multi-tenant — fora da Fase 6.

**Próximo passo:** Slice 6.2 (Catálogo) — produtos com DnD de imagens + categorias.
