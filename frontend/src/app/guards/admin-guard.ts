import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const AdminGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const isAdmin = authService.isAdmin();  // or authService.hasRole('ROLE_ADMIN')

  if (isAdmin) {
    return true;
  }

  router.navigate(['/dashboard']);
  return false;
};