import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { MockAuthRole } from './models/auth.models';
import { AuthService } from './auth.service';

export function roleGuard(allowedRoles: MockAuthRole[]): CanActivateFn {
  return () => {
    const authService = inject(AuthService);
    const router = inject(Router);

    if (!authService.isAuthenticated()) {
      return router.createUrlTree(['/login']);
    }

    const role = authService.getCurrentRole();
    return role && allowedRoles.includes(role) ? true : router.createUrlTree(['/dashboard']);
  };
}

