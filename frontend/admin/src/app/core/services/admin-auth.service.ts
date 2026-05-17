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
  rolePrincipal,
  temAlgumaRoleAdmin,
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
  readonly currentUserRoles = computed<Role[]>(() => this._user()?.roles ?? []);
  readonly rolePrincipal = computed<Role | null>(() => rolePrincipal(this._user()?.roles));
  readonly isAuthenticated = computed(() => this._accessToken() !== null);
  readonly isOperador = computed(() => this.currentUserRoles().includes('ROLE_OPERADOR'));
  readonly isGerente = computed(() => this.currentUserRoles().includes('ROLE_GERENTE'));
  readonly isAdmin = computed(() => this.currentUserRoles().includes('ROLE_ADMIN_LOJA'));

  login(req: LoginRequest): Observable<AdminUser> {
    return this.http
      .post<AuthResponse>(`${this.api}/auth/login`, req, { withCredentials: true })
      .pipe(
        switchMap(res => {
          if (!temAlgumaRoleAdmin(res.usuario.roles)) {
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
