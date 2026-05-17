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
