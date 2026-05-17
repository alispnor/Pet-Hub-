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
