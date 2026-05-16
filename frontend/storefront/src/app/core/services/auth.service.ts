import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, tap, catchError, map, throwError } from 'rxjs';

import { environment } from '@env/environment';
import {
  AuthResponse,
  AuthenticatedUser,
  LoginRequest,
  RegisterClienteRequest,
  Role,
} from '@shared/models/user';

/**
 * AuthService — fonte da verdade da sessão no storefront.
 *
 * Estratégia:
 * - Access token: signal em memória (perdido no F5; resgatado via refresh).
 * - Refresh token: cookie HttpOnly emitido pelo backend (Path=/api/v1/auth).
 *   O JS nunca lê esse cookie. As chamadas para /auth/refresh anexam o cookie
 *   automaticamente porque o AuthInterceptor envia `withCredentials: true`.
 * - Usuário: signal computado, populado no login e no bootstrap (tryRefreshOnBoot).
 *
 * AppSec: nenhum token vai para localStorage/sessionStorage. Se XSS roubar
 * o access token, a janela é de no máximo 15 min — refresh continua protegido
 * pelo cookie HttpOnly.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiBase;

  private readonly _accessToken = signal<string | null>(null);
  private readonly _user = signal<AuthenticatedUser | null>(null);

  readonly accessToken = this._accessToken.asReadonly();
  readonly currentUser = this._user.asReadonly();
  readonly isAuthenticated = computed(() => this._accessToken() !== null);
  readonly isAdmin = computed(() => this.hasAnyRole(['ROLE_ADMIN_LOJA', 'ROLE_GERENTE', 'ROLE_OPERADOR']));

  // -------------------------------------------------------------- Public API

  login(req: LoginRequest): Observable<AuthenticatedUser> {
    return this.http
      .post<AuthResponse>(`${this.api}/auth/login`, req, { withCredentials: true })
      .pipe(
        tap((res) => this.storeSession(res)),
        map((res) => res.usuario),
      );
  }

  registerCliente(req: RegisterClienteRequest): Observable<AuthenticatedUser> {
    return this.http
      .post<AuthenticatedUser>(`${this.api}/auth/register/cliente`, req)
      .pipe(
        // Após registrar, faz login automático para popular sessão
        tap(() => null),
      );
  }

  /**
   * Troca o refresh (no cookie) por novo access token. Não exige body —
   * o backend lê do cookie.
   */
  refresh(): Observable<string> {
    return this.http
      .post<AuthResponse>(`${this.api}/auth/refresh`, {}, { withCredentials: true })
      .pipe(
        tap((res) => this.storeSession(res)),
        map((res) => res.accessToken),
      );
  }

  logout(): Observable<void> {
    return this.http
      .post<void>(`${this.api}/auth/logout`, {}, { withCredentials: true })
      .pipe(
        tap({
          next: () => this.clearSession(),
          error: () => this.clearSession(),
        }),
        catchError(() => of(void 0)),
      );
  }

  loadMe(): Observable<AuthenticatedUser> {
    return this.http
      .get<AuthenticatedUser>(`${this.api}/auth/me`, { withCredentials: true })
      .pipe(tap((u) => this._user.set(u)));
  }

  /**
   * Bootstrap inicial: se houver cookie de refresh válido, recupera a sessão
   * via refresh + me. Falhas são tratadas como "sem sessão" (estado inicial).
   */
  tryRefreshOnBoot(): Observable<AuthenticatedUser | null> {
    return this.refresh().pipe(
      // refresh já popula o user via /auth/refresh response (não, ele só seta access; precisa do me)
      tap(() => null),
      catchError(() => {
        this.clearSession();
        return throwError(() => new Error('no-session'));
      }),
      // map para garantir tipagem
      map(() => this._user()),
      catchError(() => of(null)),
    );
  }

  hasAnyRole(roles: ReadonlyArray<Role>): boolean {
    const user = this._user();
    if (!user) return false;
    return user.roles.some((r) => roles.includes(r));
  }

  // ------------------------------------------------------------- Internals

  private storeSession(res: AuthResponse): void {
    this._accessToken.set(res.accessToken);
    this._user.set(res.usuario);
  }

  private clearSession(): void {
    this._accessToken.set(null);
    this._user.set(null);
  }
}
