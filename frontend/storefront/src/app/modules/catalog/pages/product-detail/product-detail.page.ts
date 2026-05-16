import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';

import { AuthService } from '@core/services/auth.service';
import { CartService } from '@modules/cart/services/cart.service';
import { CatalogService } from '@modules/catalog/services/catalog.service';
import { ProdutoDetail, ProdutoImagem } from '@modules/catalog/models/catalog';
import { PriceDisplayComponent } from '@shared/components/price-display/price-display.component';
import { ShippingCalculatorComponent } from '@shared/components/shipping-calculator/shipping-calculator.component';

interface EspecificacaoExibida {
  label: string;
  value: string;
}

@Component({
  selector: 'app-product-detail-page',
  standalone: true,
  imports: [CommonModule, RouterLink, PriceDisplayComponent, ShippingCalculatorComponent],
  templateUrl: './product-detail.page.html',
})
export class ProductDetailPage implements OnInit {
  private readonly catalogService = inject(CatalogService);
  private readonly cartService = inject(CartService);
  private readonly authService = inject(AuthService);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly produto = signal<ProdutoDetail | null>(null);
  readonly loading = signal(true);
  readonly erroCarregamento = signal<string | null>(null);

  readonly qty = signal(1);
  readonly imagemAtivaId = signal<number | null>(null);
  readonly adicionandoAoCarrinho = signal(false);
  readonly hintAdicionarCarrinho = signal<string | null>(null);

  readonly imagemAtiva = (): ProdutoImagem | null => {
    const produtoExibido = this.produto();
    if (!produtoExibido || !produtoExibido.imagens.length) return null;
    const idSelecionado = this.imagemAtivaId();
    return produtoExibido.imagens.find((imagem) => imagem.id === idSelecionado)
      ?? produtoExibido.imagens.find((imagem) => imagem.principal)
      ?? produtoExibido.imagens[0];
  };

  ngOnInit(): void {
    this.activatedRoute.paramMap.subscribe((paramMap) => {
      const skuRota = paramMap.get('sku');
      if (!skuRota) {
        this.router.navigate(['/produtos']);
        return;
      }
      this.carregarProduto(skuRota);
    });
  }

  selecionarImagem(imagemId: number): void {
    this.imagemAtivaId.set(imagemId);
  }

  incrementarQty(): void {
    this.qty.update((qtyAtual) => Math.min(10, qtyAtual + 1));
  }

  decrementarQty(): void {
    this.qty.update((qtyAtual) => Math.max(1, qtyAtual - 1));
  }

  atualizarQty(valorBruto: string): void {
    const valorNumerico = Number(valorBruto);
    if (Number.isFinite(valorNumerico) && valorNumerico >= 1 && valorNumerico <= 10) {
      this.qty.set(Math.floor(valorNumerico));
    }
  }

  adicionarAoCarrinho(): void {
    const produtoExibido = this.produto();
    if (!produtoExibido || this.adicionandoAoCarrinho()) return;
    if (!this.authService.isAuthenticated()) {
      this.router.navigate(['/login']);
      return;
    }
    this.adicionandoAoCarrinho.set(true);
    this.hintAdicionarCarrinho.set(null);
    this.cartService.add(produtoExibido.sku, this.qty()).subscribe({
      next: () => {
        this.adicionandoAoCarrinho.set(false);
        this.hintAdicionarCarrinho.set('Adicionado ao carrinho.');
      },
      error: (httpError: HttpErrorResponse) => {
        this.adicionandoAoCarrinho.set(false);
        this.hintAdicionarCarrinho.set(httpError.error?.detail ?? 'Não foi possível adicionar.');
      },
    });
  }

  listaEspecificacoes(produtoExibido: ProdutoDetail): EspecificacaoExibida[] {
    const especificacoes = produtoExibido.specs ?? {};
    return Object.entries(especificacoes).map(([chave, valor]) => ({
      label: this.humanizarChave(chave),
      value: this.formatarValor(valor),
    }));
  }

  /**
   * O backend devolve descricaoCompleta como texto plano (no seed). Para mitigar
   * XSS caso algum admin colar HTML rich no futuro, escapamos < e > antes de
   * preencher innerHTML. Quando entrar um sanitizador real (Slice 4+), trocar
   * por DOMPurify via pipe dedicado.
   */
  descricaoSanitizada(textoCru: string): string {
    return textoCru
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/\n/g, '<br>');
  }

  private carregarProduto(sku: string): void {
    this.loading.set(true);
    this.erroCarregamento.set(null);
    this.produto.set(null);
    this.catalogService.getProductBySku(sku).subscribe({
      next: (produtoCarregado) => {
        this.produto.set(produtoCarregado);
        const imagemPrincipal = produtoCarregado.imagens.find((imagem) => imagem.principal);
        this.imagemAtivaId.set(imagemPrincipal?.id ?? produtoCarregado.imagens[0]?.id ?? null);
        this.loading.set(false);
        if (typeof document !== 'undefined') {
          document.title = `${produtoCarregado.nome} · Pet Hub`;
        }
      },
      error: (httpError: HttpErrorResponse) => {
        this.loading.set(false);
        this.erroCarregamento.set(
          httpError.status === 404
            ? 'Produto não encontrado.'
            : 'Erro ao carregar o produto.',
        );
      },
    });
  }

  private humanizarChave(chave: string): string {
    return chave
      .replace(/_/g, ' ')
      .replace(/([A-Z])/g, ' $1')
      .replace(/^./, (primeiraLetra) => primeiraLetra.toUpperCase())
      .trim();
  }

  private formatarValor(valor: unknown): string {
    if (valor === null || valor === undefined) return '—';
    if (typeof valor === 'boolean') return valor ? 'Sim' : 'Não';
    if (Array.isArray(valor)) return valor.join(', ');
    if (typeof valor === 'object') return JSON.stringify(valor);
    return String(valor);
  }
}
