import { Component, OnInit, OnDestroy, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Subject, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs';

import { CatalogService } from '../../core/services/catalog.service';
import { Categoria, ProdutoSummary } from '../../core/models/catalog';
import { PageableResponse } from '../../core/models/page';
import { ProductCardComponent } from '../../shared/components/product-card/product-card.component';

@Component({
  selector: 'app-product-list-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, ProductCardComponent],
  template: `
    <section class="mx-auto max-w-page px-6 py-12">
      <header class="mb-8">
        <h1 class="text-3xl text-graphite-900">
          {{ categoriaAtivaNome() || 'Todos os produtos' }}
        </h1>
        <p class="mt-2 text-graphite-600">
          {{ pageRes()?.totalElements ?? 0 }} produto(s) disponíveis
        </p>
      </header>

      <div class="grid grid-cols-1 gap-8 lg:grid-cols-[260px_1fr]">
        <!-- Filtros -->
        <aside class="space-y-6">
          <div class="surface">
            <h2 class="text-base font-semibold text-graphite-900">Buscar</h2>
            <input type="text" class="input mt-3"
                   placeholder="nome, marca…"
                   [formControl]="searchCtrl"
                   aria-label="Buscar produtos" />
          </div>
          <div class="surface">
            <h2 class="text-base font-semibold text-graphite-900">Categorias</h2>
            <ul class="mt-3 space-y-1">
              <li>
                <button type="button"
                        class="block w-full text-left text-sm py-1.5 rounded px-2 transition-colors"
                        [class.bg-coral-50]="!categoriaSlug()"
                        [class.text-coral-700]="!categoriaSlug()"
                        [class.text-graphite-700]="!!categoriaSlug()"
                        (click)="onCategoria(null)">
                  Todas
                </button>
              </li>
              <li *ngFor="let c of categorias()">
                <button type="button"
                        class="block w-full text-left text-sm py-1.5 rounded px-2 transition-colors"
                        [class.bg-coral-50]="categoriaSlug() === c.slug"
                        [class.text-coral-700]="categoriaSlug() === c.slug"
                        [class.text-graphite-700]="categoriaSlug() !== c.slug"
                        (click)="onCategoria(c.slug)">
                  {{ c.nome }}
                </button>
              </li>
            </ul>
          </div>
        </aside>

        <!-- Resultado -->
        <div>
          <div *ngIf="loading()" class="grid grid-cols-2 gap-4 md:grid-cols-3">
            <div *ngFor="let _ of skeletons"
                 class="aspect-square animate-pulse rounded-lg bg-graphite-100"></div>
          </div>

          <div *ngIf="!loading() && produtos().length"
               class="grid grid-cols-2 gap-4 md:grid-cols-3">
            <app-product-card *ngFor="let p of produtos()" [produto]="p" />
          </div>

          <p *ngIf="!loading() && !produtos().length"
             class="mt-8 text-graphite-500">
            Nenhum produto encontrado{{ search() ? ' para "' + search() + '"' : '' }}.
          </p>

          <!-- Paginação -->
          <nav *ngIf="pageRes() && pageRes()!.totalPages > 1"
               class="mt-10 flex items-center justify-center gap-2"
               aria-label="Paginação">
            <button class="btn-secondary text-sm py-2 px-3"
                    [disabled]="pageRes()!.first"
                    (click)="onPage(pageRes()!.page - 1)">Anterior</button>
            <span class="text-sm text-graphite-600 px-3">
              Página {{ pageRes()!.page + 1 }} de {{ pageRes()!.totalPages }}
            </span>
            <button class="btn-secondary text-sm py-2 px-3"
                    [disabled]="pageRes()!.last"
                    (click)="onPage(pageRes()!.page + 1)">Próxima</button>
          </nav>
        </div>
      </div>
    </section>
  `,
})
export class ProductListPage implements OnInit, OnDestroy {
  private readonly catalog = inject(CatalogService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroy$ = new Subject<void>();

  readonly searchCtrl = new FormControl('', { nonNullable: true });

  readonly pageRes = signal<PageableResponse<ProdutoSummary> | null>(null);
  readonly produtos = computed(() => this.pageRes()?.content ?? []);
  readonly loading = signal(true);
  readonly categorias = signal<Categoria[]>([]);
  readonly categoriaSlug = signal<string | null>(null);
  readonly search = signal<string>('');
  readonly page = signal<number>(0);
  protected readonly skeletons = Array.from({ length: 9 });

  readonly categoriaAtivaNome = computed(() => {
    const slug = this.categoriaSlug();
    if (!slug) return null;
    return this.categorias().find((c) => c.slug === slug)?.nome ?? null;
  });

  ngOnInit(): void {
    this.catalog.listCategories().subscribe({
      next: (cs) => this.categorias.set(cs.filter((c) => c.ativo).sort((a, b) => a.ordem - b.ordem)),
    });

    this.route.queryParamMap.pipe(takeUntil(this.destroy$)).subscribe((q) => {
      this.categoriaSlug.set(q.get('categoria'));
      this.search.set(q.get('q') ?? '');
      this.page.set(Number(q.get('page') ?? 0));
      this.searchCtrl.setValue(this.search(), { emitEvent: false });
      this.fetch();
    });

    this.searchCtrl.valueChanges
      .pipe(debounceTime(350), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe((v) => this.applyQuery({ q: v, page: 0 }));
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onCategoria(slug: string | null): void {
    this.applyQuery({ categoria: slug, page: 0 });
  }

  onPage(p: number): void {
    this.applyQuery({ page: p });
  }

  private applyQuery(patch: { q?: string; categoria?: string | null; page?: number }): void {
    const merged: Record<string, string | null> = {
      q: patch.q !== undefined ? (patch.q || null) : this.search() || null,
      categoria: patch.categoria !== undefined ? patch.categoria : this.categoriaSlug(),
      page: String(patch.page ?? this.page()),
    };
    if (merged['page'] === '0') merged['page'] = null;
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: merged,
      queryParamsHandling: 'merge',
    });
  }

  private fetch(): void {
    this.loading.set(true);
    const obs = this.search()
      ? this.catalog.searchProducts(this.search(), { page: this.page(), size: 12 })
      : this.catalog.listProducts({
          categoria: this.categoriaSlug() ?? undefined,
          page: this.page(),
          size: 12,
        });
    obs.subscribe({
      next: (p) => {
        this.pageRes.set(p);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.pageRes.set(null);
      },
    });
  }
}
