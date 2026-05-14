import { Routes } from '@angular/router';

import { MainLayoutComponent } from './shared/layout/main-layout.component';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    component: MainLayoutComponent,
    children: [
      {
        path: '',
        loadComponent: () => import('./features/home/home.page').then((m) => m.HomePage),
      },
      {
        path: 'login',
        loadComponent: () => import('./features/auth/login.page').then((m) => m.LoginPage),
      },
      {
        path: 'cadastro',
        loadComponent: () => import('./features/auth/register.page').then((m) => m.RegisterPage),
      },
      {
        path: 'produtos',
        loadComponent: () =>
          import('./features/catalog/product-list.page').then((m) => m.ProductListPage),
      },
      {
        path: 'produtos/:sku',
        loadComponent: () =>
          import('./features/catalog/product-detail.page').then((m) => m.ProductDetailPage),
      },
      {
        path: 'minha-conta',
        canActivate: [authGuard],
        loadComponent: () => import('./features/home/home.page').then((m) => m.HomePage),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
