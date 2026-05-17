import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AdminAuthService } from '@core/services/admin-auth.service';
import { Role } from '@core/models/auth';

export function roleGuard(rolesPermitidas: Role[]): CanActivateFn {
  return () => {
    const auth = inject(AdminAuthService);
    const router = inject(Router);
    const rolesDoUsuario = auth.currentUserRoles();
    if (rolesDoUsuario.length === 0) {
      return router.parseUrl('/login');
    }
    const temPermissao = rolesDoUsuario.some(role => rolesPermitidas.includes(role));
    if (!temPermissao) {
      return router.parseUrl('/dashboard');
    }
    return true;
  };
}
