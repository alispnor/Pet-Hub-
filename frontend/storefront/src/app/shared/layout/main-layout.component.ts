import { Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="min-h-screen flex flex-col">
      <header class="border-b border-graphite-200 bg-graphite-0">
        <div class="mx-auto flex max-w-page items-center justify-between px-6 py-4">
          <a routerLink="/" class="text-xl font-display font-semibold text-graphite-900">
            Pet Hub
          </a>
          <nav class="flex items-center gap-6 text-sm">
            <a routerLink="/produtos" routerLinkActive="text-coral-500"
               class="text-graphite-700 hover:text-graphite-900">Catálogo</a>
            <ng-container *ngIf="isLogged(); else loggedOut">
              <a routerLink="/minha-conta" routerLinkActive="text-coral-500"
                 class="text-graphite-700 hover:text-graphite-900">
                {{ userName() }}
              </a>
              <button class="btn-ghost text-sm py-1.5 px-3" (click)="onLogout()">Sair</button>
            </ng-container>
            <ng-template #loggedOut>
              <a routerLink="/login" class="text-graphite-700 hover:text-graphite-900">Entrar</a>
              <a routerLink="/cadastro" class="btn-primary text-sm py-2 px-4">Criar conta</a>
            </ng-template>
          </nav>
        </div>
      </header>
      <main class="flex-1">
        <router-outlet />
      </main>
      <footer class="border-t border-graphite-200 bg-graphite-0">
        <div class="mx-auto max-w-page px-6 py-8 text-xs text-graphite-500">
          © 2026 Pet Hub · Pet tech ecommerce
        </div>
      </footer>
    </div>
  `,
})
export class MainLayoutComponent {
  private readonly auth = inject(AuthService);
  readonly isLogged = this.auth.isAuthenticated;
  readonly userName = computed(() => this.auth.currentUser()?.nome ?? '');

  onLogout(): void {
    this.auth.logout().subscribe();
  }
}
