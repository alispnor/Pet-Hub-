import { Component, computed, input } from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * Exibe o preço em BRL com opção de parcelamento sugerido (1 a 12x sem juros
 * em compras acima de R$ 200, alinhado ao MockPaymentGateway).
 */
@Component({
  selector: 'app-price-display',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="space-y-0.5">
      <p class="text-2xl text-graphite-900 font-semibold tracking-tight">
        {{ valor() | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}
      </p>
      <p *ngIf="parcelas() && parcelas()!.qtd > 1"
         class="text-sm text-graphite-500">
        em {{ parcelas()!.qtd }}x de
        {{ parcelas()!.valor | currency:'BRL':'symbol':'1.2-2':'pt-BR' }} sem juros
      </p>
    </div>
  `,
})
export class PriceDisplayComponent {
  valor = input.required<number>();
  /** Máximo de parcelas a sugerir; default = 12. */
  maxParcelas = input(12);
  /** Piso para começar a parcelar (sem juros). Default R$ 200. */
  minPraParcelar = input(200);

  protected parcelas = computed(() => {
    const v = this.valor();
    if (v < this.minPraParcelar()) return null;
    // Heurística: 1x até R$200 / 3x até R$600 / 6x até R$1500 / 12x acima
    const qtd = v < 600 ? 3 : v < 1500 ? 6 : Math.min(12, this.maxParcelas());
    return { qtd, valor: Math.round((v / qtd) * 100) / 100 };
  });
}
