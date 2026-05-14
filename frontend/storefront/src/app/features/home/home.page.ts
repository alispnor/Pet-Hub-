import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../core/services/auth.service';
import { CatalogService } from '../../core/services/catalog.service';
import { ProdutoSummary } from '../../core/models/catalog';
import { ProductCardComponent } from '../../shared/components/product-card/product-card.component';

@Component({
  selector: 'app-home-page',
  standalone: true,
  imports: [CommonModule, RouterLink, ProductCardComponent],
  template: `
    <section class="bg-gradient-to-b from-graphite-50 to-graphite-0">
      <div class="mx-auto max-w-page px-6 py-16 lg:py-24">
        <p class="text-sm font-medium uppercase tracking-wider text-coral-500">
          Pet tech ecommerce
        </p>
        <h1 class="mt-4 max-w-2xl text-4xl text-graphite-900 lg:text-5xl">
          Inteligência conectada para o cuidado diário dos pets.
        </h1>
        <p class="mt-6 max-w-2xl text-lg text-graphite-600">
          Smart collars, GPS trackers, comedouros IoT e câmeras pet — selecionados
          com IA generativa para a rotina do seu animal.
        </p>
        <div class="mt-8 flex flex-wrap gap-3">
          <a routerLink="/produtos" class="btn-primary">Explorar catálogo</a>
          <a *ngIf="!loggedIn()" routerLink="/cadastro" class="btn-secondary">Criar conta</a>
        </div>
      </div>
    </section>

    <section class="mx-auto max-w-page px-6 py-12">
      <div class="flex items-end justify-between">
        <h2 class="text-3xl text-graphite-900">Destaques da semana</h2>
        <a routerLink="/produtos"
           class="text-sm font-medium text-coral-500 hover:text-coral-600">
          Ver tudo →
        </a>
      </div>

      <div *ngIf="loading()" class="mt-8 grid grid-cols-2 gap-4 md:grid-cols-3 lg:grid-cols-4">
        <div *ngFor="let _ of skeletons" class="aspect-square animate-pulse rounded-lg bg-graphite-100"></div>
      </div>

      <div *ngIf="!loading() && featured().length"
           class="mt-8 grid grid-cols-2 gap-4 md:grid-cols-3 lg:grid-cols-4">
        <app-product-card *ngFor="let p of featured()" [produto]="p" />
      </div>

      <p *ngIf="!loading() && !featured().length && error()"
         class="mt-8 text-sm text-graphite-500">{{ error() }}</p>
    </section>
  `,
})
export class HomePage implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly catalog = inject(CatalogService);

  readonly loggedIn = this.auth.isAuthenticated;
  readonly featured = signal<ProdutoSummary[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  protected readonly skeletons = Array.from({ length: 8 });

  ngOnInit(): void {
    this.catalog.listProducts({ size: 8 }).subscribe({
      next: (page) => {
        const ordered = [...page.content].sort((a, b) => Number(b.destacado) - Number(a.destacado));
        this.featured.set(ordered);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set('Não foi possível carregar os destaques agora.');
      },
    });
  }
}
