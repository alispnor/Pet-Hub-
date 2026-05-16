import { Component, OnInit, OnDestroy, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Subject, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs';

import { CatalogService } from '@modules/catalog/services/catalog.service';
import { Categoria, ProdutoSummary } from '@modules/catalog/models/catalog';
import { PageableResponse } from '@shared/models/page';
import { ProductCardComponent } from '@shared/components/product-card/product-card.component';

@Component({
  selector: 'app-product-list-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, ProductCardComponent],
  templateUrl: './product-list.page.html',
})
export class ProductListPage implements OnInit, OnDestroy {
  private readonly catalogService = inject(CatalogService);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destruir$ = new Subject<void>();

  readonly campoBusca = new FormControl('', { nonNullable: true });

  readonly paginaAtual = signal<PageableResponse<ProdutoSummary> | null>(null);
  readonly produtos = computed(() => this.paginaAtual()?.content ?? []);
  readonly loading = signal(true);
  readonly categorias = signal<Categoria[]>([]);
  readonly categoriaSlug = signal<string | null>(null);
  readonly termoBusca = signal<string>('');
  readonly numeroPagina = signal<number>(0);
  protected readonly skeletons = Array.from({ length: 9 });

  readonly nomeCategoriaSelecionada = computed(() => {
    const slugSelecionado = this.categoriaSlug();
    if (!slugSelecionado) return null;
    return this.categorias().find((categoria) => categoria.slug === slugSelecionado)?.nome ?? null;
  });

  ngOnInit(): void {
    this.catalogService.listCategories().subscribe({
      next: (listaCategorias) => this.categorias.set(
        listaCategorias
          .filter((categoria) => categoria.ativo)
          .sort((primeira, segunda) => primeira.ordem - segunda.ordem),
      ),
    });

    this.activatedRoute.queryParamMap
      .pipe(takeUntil(this.destruir$))
      .subscribe((queryParams) => {
        this.categoriaSlug.set(queryParams.get('categoria'));
        this.termoBusca.set(queryParams.get('q') ?? '');
        this.numeroPagina.set(Number(queryParams.get('page') ?? 0));
        this.campoBusca.setValue(this.termoBusca(), { emitEvent: false });
        this.buscarProdutos();
      });

    this.campoBusca.valueChanges
      .pipe(debounceTime(350), distinctUntilChanged(), takeUntil(this.destruir$))
      .subscribe((novoTermo) => this.atualizarQueryParams({ q: novoTermo, page: 0 }));
  }

  ngOnDestroy(): void {
    this.destruir$.next();
    this.destruir$.complete();
  }

  selecionarCategoria(slug: string | null): void {
    this.atualizarQueryParams({ categoria: slug, page: 0 });
  }

  irParaPagina(numeroPaginaDestino: number): void {
    this.atualizarQueryParams({ page: numeroPaginaDestino });
  }

  private atualizarQueryParams(
    patch: { q?: string; categoria?: string | null; page?: number },
  ): void {
    const queryParamsMerged: Record<string, string | null> = {
      q: patch.q !== undefined ? (patch.q || null) : this.termoBusca() || null,
      categoria: patch.categoria !== undefined ? patch.categoria : this.categoriaSlug(),
      page: String(patch.page ?? this.numeroPagina()),
    };
    if (queryParamsMerged['page'] === '0') queryParamsMerged['page'] = null;
    this.router.navigate([], {
      relativeTo: this.activatedRoute,
      queryParams: queryParamsMerged,
      queryParamsHandling: 'merge',
    });
  }

  private buscarProdutos(): void {
    this.loading.set(true);
    const observableProdutos = this.termoBusca()
      ? this.catalogService.searchProducts(this.termoBusca(), {
          page: this.numeroPagina(),
          size: 12,
        })
      : this.catalogService.listProducts({
          categoria: this.categoriaSlug() ?? undefined,
          page: this.numeroPagina(),
          size: 12,
        });
    observableProdutos.subscribe({
      next: (paginaResposta) => {
        this.paginaAtual.set(paginaResposta);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.paginaAtual.set(null);
      },
    });
  }
}
