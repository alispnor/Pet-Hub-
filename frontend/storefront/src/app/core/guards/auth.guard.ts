import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { catchError, map, of } from 'rxjs';

import { AuthService } from '../services/auth.service';

/**
 * Bloqueia rotas autenticadas. Se o signal indica logado, libera direto;
 * caso contrário tenta um refresh transparente antes de redirecionar para
 * /login (cobre o F5: cookie ainda válido recupera sessão sem novo login).
 */
export const authGuard: CanActivateFn = (route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (auth.isAuthenticated()) {
    return true;
  }

  return auth.refresh().pipe(
    map(() => true),
    catchError(() => {
      router.navigate(['/login'], { queryParams: { redirectTo: state.url } });
      return of(false);
    }),
  );
};
