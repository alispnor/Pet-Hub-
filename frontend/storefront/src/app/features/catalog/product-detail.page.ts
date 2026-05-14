import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

import { CatalogService } from '../../core/services/catalog.service';
import { ProdutoDetail } from '../../core/models/catalog';
import { PriceDisplayComponent } from '../../shared/components/price-display/price-display.component';
import { ShippingCalculatorComponent } from '../../shared/components/shipping-calculator/shipping-calculator.component';

@Component({
  selector: 'app-product-detail-page',
  standalone: true,
  imports: [CommonModule, RouterLink, PriceDisplayComponent, ShippingCalculatorComponent],
  template: `
    <section *ngIf="produto() as p" class="mx-auto max-w-page px-6 py-12">
      <nav class="mb-6 text-sm text-graphite-500">
        <a routerLink="/produtos" class="hover:text-graphite-700">Catálogo</a>
        <span class="mx-1.5">/</span>
        <a [routerLink]="['/produtos']" [queryParams]="{ categoria: p.categoria.slug }"
           class="hover:text-graphite-700">{{ p.categoria.nome }}</a>
      </nav>

      <div class="grid grid-cols-1 gap-12 lg:grid-cols-2">
        <!-- Galeria -->
        <div>
          <div class="aspect-square overflow-hidden rounded-lg bg-graphite-100">
            <img *ngIf="imagemAtiva() as ia"
                 [src]="ia.url" [alt]="ia.alt"
                 class="h-full w-full object-cover" />
            <div *ngIf="!p.imagens.length"
                 class="flex h-full w-full items-center justify-center text-graphite-400 text-sm">
              sem imagem
            </div>
          </div>
          <div *ngIf="p.imagens.length > 1" class="mt-3 flex gap-2 overflow-x-auto">
            <button *ngFor="let img of p.imagens"
                    type="button"
                    (click)="selecionarImagem(img.id)"
                    class="h-20 w-20 flex-shrink-0 overflow-hidden rounded border transition-all"
                    [class.border-coral-500]="imagemAtivaId() === img.id"
                    [class.border-graphite-200]="imagemAtivaId() !== img.id">
              <img [src]="img.url" [alt]="img.alt" class="h-full w-full object-cover" />
            </button>
          </div>
        </div>

        <!-- Info -->
        <div class="space-y-6">
          <div>
            <p class="text-xs uppercase tracking-wider text-graphite-500">
              {{ p.categoria.nome }}<span *ngIf="p.marca"> · {{ p.marca }}</span>
            </p>
            <h1 class="mt-2 text-3xl text-graphite-900">{{ p.nome }}</h1>
            <p *ngIf="p.descricaoCurta" class="mt-3 text-graphite-600">
              {{ p.descricaoCurta }}
            </p>
          </div>

          <app-price-display [valor]="p.preco" />

          <div class="space-y-3">
            <label class="label" for="qty">Quantidade</label>
            <div class="flex items-center gap-2">
              <button type="button" class="btn-secondary px-3 py-1.5"
                      (click)="decrementar()"
                      aria-label="Diminuir quantidade">−</button>
              <input id="qty" type="number" class="input w-20 text-center"
                     [value]="qty()" (input)="setQty($any($event.target).value)"
                     min="1" max="10" />
              <button type="button" class="btn-secondary px-3 py-1.5"
                      (click)="incrementar()"
                      aria-label="Aumentar quantidade">+</button>
            </div>
          </div>

          <button type="button" class="btn-primary w-full"
                  (click)="adicionarAoCarrinho()"
                  [disabled]="addingToCart()">
            <span *ngIf="!addingToCart()">Adicionar ao carrinho</span>
            <span *ngIf="addingToCart()">Adicionando…</span>
          </button>
          <p *ngIf="addToCartHint()" class="text-xs text-graphite-500 text-center">
            {{ addToCartHint() }}
          </p>

          <div class="surface">
            <app-shipping-calculator [sku]="p.sku" [qty]="qty()" />
          </div>
        </div>
      </div>

      <section *ngIf="p.descricaoCompleta" class="mt-16 max-w-reading">
        <h2 class="text-2xl text-graphite-900">Sobre este produto</h2>
        <div class="prose prose-graphite mt-4 text-graphite-700"
             [innerHTML]="descricaoSanitizada(p.descricaoCompleta)"></div>
      </section>

      <section *ngIf="specsList(p).length" class="mt-12">
        <h2 class="text-2xl text-graphite-900">Especificações</h2>
        <dl class="mt-4 divide-y divide-graphite-200 border-y border-graphite-200">
          <div *ngFor="let s of specsList(p)" class="grid grid-cols-2 py-3 text-sm">
            <dt class="text-graphite-500">{{ s.label }}</dt>
            <dd class="text-graphite-900">{{ s.value }}</dd>
          </div>
        </dl>
      </section>
    </section>

    <section *ngIf="loading()" class="mx-auto max-w-page px-6 py-16">
      <div class="h-6 w-1/3 animate-pulse rounded bg-graphite-200"></div>
      <div class="mt-8 grid grid-cols-1 gap-12 lg:grid-cols-2">
        <div class="aspect-square animate-pulse rounded-lg bg-graphite-100"></div>
        <div class="space-y-4">
          <div class="h-8 w-3/4 animate-pulse rounded bg-graphite-200"></div>
          <div class="h-4 w-full animate-pulse rounded bg-graphite-200"></div>
          <div class="h-4 w-5/6 animate-pulse rounded bg-graphite-200"></div>
        </div>
      </div>
    </section>

    <section *ngIf="error()" class="mx-auto max-w-page px-6 py-24 text-center">
      <p class="text-graphite-600">{{ error() }}</p>
      <a routerLink="/produtos" class="btn-secondary mt-4 inline-flex">Voltar ao catálogo</a>
    </section>
  `,
})
export class ProductDetailPage implements OnInit {
  private readonly catalog = inject(CatalogService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly produto = signal<ProdutoDetail | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  readonly qty = signal(1);
  readonly imagemAtivaId = signal<number | null>(null);
  readonly addingToCart = signal(false);
  readonly addToCartHint = signal<string | null>(null);

  readonly imagemAtiva = () => {
    const p = this.produto();
    if (!p || !p.imagens.length) return null;
    const id = this.imagemAtivaId();
    return p.imagens.find((i) => i.id === id) ?? p.imagens.find((i) => i.principal) ?? p.imagens[0];
  };

  ngOnInit(): void {
    this.route.paramMap.subscribe((p) => {
      const sku = p.get('sku');
      if (!sku) {
        this.router.navigate(['/produtos']);
        return;
      }
      this.fetch(sku);
    });
  }

  selecionarImagem(id: number): void {
    this.imagemAtivaId.set(id);
  }

  incrementar(): void {
    this.qty.update((q) => Math.min(10, q + 1));
  }

  decrementar(): void {
    this.qty.update((q) => Math.max(1, q - 1));
  }

  setQty(raw: string): void {
    const n = Number(raw);
    if (Number.isFinite(n) && n >= 1 && n <= 10) {
      this.qty.set(Math.floor(n));
    }
  }

  adicionarAoCarrinho(): void {
    // Cart service entra no Slice 3. Por enquanto: feedback visual.
    this.addingToCart.set(true);
    this.addToCartHint.set(null);
    setTimeout(() => {
      this.addingToCart.set(false);
      this.addToCartHint.set('Carrinho disponível no próximo slice — em breve.');
    }, 400);
  }

  specsList(p: ProdutoDetail): Array<{ label: string; value: string }> {
    const specs = p.specs ?? {};
    return Object.entries(specs).map(([k, v]) => ({
      label: this.humanize(k),
      value: this.formatValue(v),
    }));
  }

  /**
   * O backend devolve descricaoCompleta como texto plano (no seed). Para mitigar
   * XSS caso algum admin colar HTML rich no futuro, escapamos < e > antes de
   * preencher innerHTML. Quando entrar um sanitizador real (Slice 4+), trocar
   * por DOMPurify via pipe dedicado.
   */
  descricaoSanitizada(raw: string): string {
    return raw
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/\n/g, '<br>');
  }

  private fetch(sku: string): void {
    this.loading.set(true);
    this.error.set(null);
    this.produto.set(null);
    this.catalog.getProductBySku(sku).subscribe({
      next: (p) => {
        this.produto.set(p);
        this.imagemAtivaId.set(p.imagens.find((i) => i.principal)?.id ?? p.imagens[0]?.id ?? null);
        this.loading.set(false);
        if (typeof document !== 'undefined') {
          document.title = `${p.nome} · Pet Hub`;
        }
      },
      error: (e: HttpErrorResponse) => {
        this.loading.set(false);
        this.error.set(e.status === 404 ? 'Produto não encontrado.' : 'Erro ao carregar o produto.');
      },
    });
  }

  private humanize(key: string): string {
    return key
      .replace(/_/g, ' ')
      .replace(/([A-Z])/g, ' $1')
      .replace(/^./, (s) => s.toUpperCase())
      .trim();
  }

  private formatValue(v: unknown): string {
    if (v === null || v === undefined) return '—';
    if (typeof v === 'boolean') return v ? 'Sim' : 'Não';
    if (Array.isArray(v)) return v.join(', ');
    if (typeof v === 'object') return JSON.stringify(v);
    return String(v);
  }
}
