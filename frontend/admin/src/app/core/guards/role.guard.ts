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
