import { HttpEvent, HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, BehaviorSubject, catchError, filter, switchMap, take, throwError } from 'rxjs';

import { AuthService } from '../services/auth.service';
import { environment } from '../../../environments/environment';

/**
 * Interceptor único de auth:
 *  - Anexa `Authorization: Bearer <accessToken>` quando disponível.
 *  - Para rotas em `credentialedPaths`, anexa `withCredentials: true` (cookie
 *    de refresh + autenticação). Resto dos requests NÃO leva credenciais —
 *    mitiga CSRF e evita leak desnecessário de cookies.
 *  - Em 401, tenta uma rotação de refresh. Pendurando outros requests num
 *    BehaviorSubject para não disparar N refreshes em paralelo. Após sucesso,
 *    re-emite o request original com novo Bearer; após falha, redireciona /login.
 *
 * Vulnerabilidades mitigadas (OWASP):
 *  - API2:2023 Broken Authentication — refresh transparente sem armazenar token.
 *  - A07:2021 — access em memória; cookie HttpOnly carrega refresh.
 *  - A05:2021 — `withCredentials` só onde precisa; sem cookies em chamadas
 *    de catálogo público.
 */

let isRefreshing = false;
const refreshGate$ = new BehaviorSubject<string | null>(null);

function needsCredentials(url: string): boolean {
  return environment.credentialedPaths.some((p) => url.includes(p));
}

function attachAuth<T>(req: HttpRequest<T>, token: string | null): HttpRequest<T> {
  const withCreds = needsCredentials(req.url) || req.withCredentials;
  let updated = req.clone({ withCredentials: withCreds });
  if (token) {
    updated = updated.clone({
      setHeaders: { Authorization: `Bearer ${token}` },
    });
  }
  return updated;
}

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  const isAuthEndpoint = req.url.includes('/auth/login')
    || req.url.includes('/auth/refresh')
    || req.url.includes('/auth/register');

  // Não interceptar o próprio /auth/refresh para refetch — caso contrário recursão
  const initial = attachAuth(req, isAuthEndpoint ? null : auth.accessToken());

  return next(initial).pipe(
    catchError((err: HttpErrorResponse) => handleError(err, req, next, auth, router)),
  );
};

function handleError(
  err: HttpErrorResponse,
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
  auth: AuthService,
  router: Router,
): Observable<HttpEvent<unknown>> {
  if (err.status !== 401) {
    return throwError(() => err);
  }
  if (req.url.includes('/auth/login') || req.url.includes('/auth/refresh')) {
    return throwError(() => err);
  }
  return tryRefreshAndRetry(req, next, auth, router);
}

function tryRefreshAndRetry(
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
  auth: AuthService,
  router: Router,
): Observable<HttpEvent<unknown>> {
  if (isRefreshing) {
    return refreshGate$.pipe(
      filter((t) => t !== null),
      take(1),
      switchMap((token) => next(attachAuth(req, token))),
    );
  }
  isRefreshing = true;
  refreshGate$.next(null);
  return auth.refresh().pipe(
    switchMap((newToken) => {
      isRefreshing = false;
      refreshGate$.next(newToken);
      return next(attachAuth(req, newToken));
    }),
    catchError((refreshErr) => {
      isRefreshing = false;
      refreshGate$.next(null);
      router.navigate(['/login']);
      return throwError(() => refreshErr);
    }),
  );
}
