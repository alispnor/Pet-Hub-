import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

import { AuthService } from '@core/services/auth.service';
import { CatalogService } from '@modules/catalog/services/catalog.service';
import { ProdutoSummary } from '@modules/catalog/models/catalog';
import { ProductCardComponent } from '@shared/components/product-card/product-card.component';

@Component({
  selector: 'app-home-page',
  standalone: true,
  imports: [CommonModule, RouterLink, ProductCardComponent],
  templateUrl: './home.page.html',
})
export class HomePage implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly catalogService = inject(CatalogService);

  readonly usuarioLogado = this.authService.isAuthenticated;
  readonly produtosDestaque = signal<ProdutoSummary[]>([]);
  readonly loading = signal(true);
  readonly erroCarregamento = signal<string | null>(null);
  protected readonly skeletons = Array.from({ length: 8 });

  ngOnInit(): void {
    this.catalogService.listProducts({ size: 8 }).subscribe({
      next: (pagina) => {
        const ordenadoPorDestaque = [...pagina.content].sort(
          (primeiro, segundo) => Number(segundo.destacado) - Number(primeiro.destacado),
        );
        this.produtosDestaque.set(ordenadoPorDestaque);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.erroCarregamento.set('Não foi possível carregar os destaques agora.');
      },
    });
  }
}
