import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-home-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <section class="mx-auto max-w-page px-6 py-16">
      <p class="text-sm font-medium uppercase tracking-wider text-coral-500">
        Pet tech ecommerce
      </p>
      <h1 class="mt-4 text-4xl text-graphite-900 max-w-2xl">
        Inteligência conectada para o cuidado diário dos pets.
      </h1>
      <p class="mt-6 max-w-2xl text-lg text-graphite-600">
        Smart collars, GPS trackers, comedouros IoT e câmeras pet — selecionados
        e recomendados com IA generativa para a rotina do seu animal.
      </p>
      <div class="mt-8 flex flex-wrap gap-3">
        <a routerLink="/produtos" class="btn-primary">Explorar catálogo</a>
        <a *ngIf="!loggedIn()" routerLink="/cadastro" class="btn-secondary">Criar conta</a>
      </div>
      <p *ngIf="loggedIn()" class="mt-10 text-sm text-graphite-500">
        Olá, {{ userName() || 'visitante' }}. Bom te ver de volta.
      </p>
    </section>
  `,
})
export class HomePage {
  private readonly auth = inject(AuthService);
  readonly loggedIn = this.auth.isAuthenticated;
  readonly userName = () => this.auth.currentUser()?.nome;
}
